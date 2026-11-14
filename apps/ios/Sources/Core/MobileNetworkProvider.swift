import Foundation
import Network
import Security
import Combine

#if canImport(FoundationNetworking)
  import FoundationNetworking
#endif

private let mobileNetworkProtocol = "outcomego-browser-network.v1"
private let mobileNetworkLeaseMs: UInt64 = 6_000
private let mobileNetworkRenewMs: UInt64 = 2_000
private let mobileNetworkWindow = 128 * 1024
private let mobileNetworkMaxData = 64 * 1024

private enum MobileNetworkFrameType: UInt8 {
  case control = 0
  case open = 1
  case ready = 2
  case error = 3
  case data = 4
  case window = 5
  case fin = 6
  case reset = 7
}

private struct MobileNetworkFrame {
  let type: MobileNetworkFrameType
  let id: UInt32
  let payload: Data
}

private enum MobileNetworkProviderError: Error, Equatable {
  case invalidFrame
  case invalidControl
  case invalidTarget
  case unavailable
  case blocked
  case connectionFailed
}

@MainActor
public final class MobileNetworkProvider: NSObject, ObservableObject {
  public enum State: Equatable, Sendable {
    case stopped
    case enrolling
    case connecting
    case ready
    case reconnecting
    case unavailable
    case revoked
    case failed(String)
  }

  @Published public private(set) var state: State = .stopped
  @Published public private(set) var activeStreams = 0

  private var api: API?
  private var sessionToken: String?
  private var machineID: String?
  private var deploymentID: String?
  private var credential: String?
  private var connectionID: String?
  private var socket: URLSessionWebSocketTask?
  private var socketSession: URLSession?
  private var pathMonitor: NWPathMonitor?
  private var reconnectTask: Task<Void, Never>?
  private var leaseTask: Task<Void, Never>?
  private var routeEpoch: String?
  private var routeAvailable = false
  private var sentSequence: UInt64 = 0
  private var receivedSequence: UInt64 = 0
  private var lastLeaseAt: UInt64 = 0
  private var expiresAt: UInt64 = 0
  private var reconnectAttempt = 0
  private var foreground = true
  private var consent = false
  private var streams: [UInt32: MobileNetworkStream] = [:]
  private var highestIncomingStreamID: UInt32 = 0
  private let store = MobileNetworkProviderStore()

  public override init() {
    super.init()
  }

  public func start(api: API, allowSharing: Bool, label: String) {
    self.api = api
    sessionToken = api.token
    guard let token = api.token, !token.isEmpty else {
      state = .failed("Sign in before enabling network sharing.")
      return
    }
    Task { @MainActor [weak self] in
      guard let self else { return }
      await self.stopTransport()
      do {
        state = .enrolling
        let installedMachineID = try store.machineID()
        machineID = installedMachineID
        let enrollment: MobileNetworkEnrollment
        if let stored = try store.credential(server: api.baseURL.absoluteString, machineID: installedMachineID),
          !stored.isEmpty {
          do {
            enrollment = try await resume(api: api, machineID: installedMachineID, credential: stored)
          } catch {
            try? store.clearCredential(server: api.baseURL.absoluteString, machineID: installedMachineID)
            enrollment = try await enroll(api: api, token: token, label: label)
          }
        } else {
          enrollment = try await enroll(api: api, token: token, label: label)
        }
        deploymentID = enrollment.deploymentID
        credential = enrollment.credential
        connectionID = enrollment.connectionID
        try store.saveCredential(enrollment.credential, server: api.baseURL.absoluteString, machineID: enrollment.machineID)
        if allowSharing {
          consent = true
          try store.setConsent(true, server: api.baseURL.absoluteString, deploymentID: enrollment.deploymentID)
        } else {
          consent = store.consent(server: api.baseURL.absoluteString, deploymentID: enrollment.deploymentID)
        }
        guard consent else {
          state = .stopped
          return
        }
        guard foreground else {
          state = .unavailable
          return
        }
        startPathMonitor()
        try await connect()
      } catch let error as MobileNetworkProviderError {
        state = error == .unavailable ? .unavailable : .failed("Network sharing could not start.")
      } catch {
        state = .failed("Network sharing could not start.")
      }
    }
  }

  public func stop(revokeConsent: Bool = false) {
    Task { @MainActor [weak self] in
      guard let self else { return }
      if revokeConsent, let api, let deploymentID {
        try? store.setConsent(false, server: api.baseURL.absoluteString, deploymentID: deploymentID)
        consent = false
      }
      await stopTransport()
      state = .stopped
    }
  }

  public func setForeground(_ active: Bool) {
    foreground = active
    if !active {
      Task { @MainActor [weak self] in
        guard let self else { return }
        await stopTransport()
        if consent { state = .unavailable }
      }
    } else if consent, socket == nil, let api {
      start(api: api, allowSharing: false, label: "iPhone")
    }
  }

  private func enroll(api: API, token: String, label: String) async throws -> MobileNetworkEnrollment {
    let id = try store.machineID()
    let body = try JSONSerialization.data(withJSONObject: ["machineId": id, "label": label], options: [])
    let enrolled = try await api.raw(
      "/api/v0/machines/enroll",
      method: "POST",
      data: body,
      headers: ["Authorization": "Bearer \(token)", "Content-Type": "application/json"])
    guard let value = try JSONSerialization.jsonObject(with: enrolled.0) as? [String: Any],
      let machineID = value["machineId"] as? String, machineID == id,
      let credential = value["credential"] as? String, !credential.isEmpty
    else { throw MobileNetworkProviderError.invalidControl }

    let machineHeaders = [
      "Authorization": "Bearer \(credential)",
      "x-outcomego-machine-id": id,
    ]
    let connected = try await api.raw("/api/v0/machines/channel/connect", method: "POST", headers: machineHeaders)
    guard let connection = try JSONSerialization.jsonObject(with: connected.0) as? [String: Any],
      let connectionID = connection["connectionId"] as? String, !connectionID.isEmpty
    else { throw MobileNetworkProviderError.invalidControl }

    let identity = try await api.raw(
      "/api/v0/machines/channel/browser-network-identity",
      headers: machineHeaders.merging(["x-outcomego-connection-id": connectionID]) { _, value in value })
    guard let deployment = try JSONSerialization.jsonObject(with: identity.0) as? [String: Any],
      let deploymentID = deployment["deploymentId"] as? String, UUID(uuidString: deploymentID) != nil
    else { throw MobileNetworkProviderError.invalidControl }
    return MobileNetworkEnrollment(machineID: id, credential: credential, connectionID: connectionID, deploymentID: deploymentID)
  }

  private func resume(api: API, machineID: String, credential: String) async throws -> MobileNetworkEnrollment {
    let headers = [
      "Authorization": "Bearer \(credential)",
      "x-outcomego-machine-id": machineID,
    ]
    let connected = try await api.raw(
      "/api/v0/machines/channel/connect",
      method: "POST",
      headers: headers)
    guard let connection = try JSONSerialization.jsonObject(with: connected.0) as? [String: Any],
      let connectionID = connection["connectionId"] as? String, !connectionID.isEmpty
    else { throw MobileNetworkProviderError.invalidControl }
    let identity = try await api.raw(
      "/api/v0/machines/channel/browser-network-identity",
      headers: headers.merging(["x-outcomego-connection-id": connectionID]) { _, value in value })
    guard let deployment = try JSONSerialization.jsonObject(with: identity.0) as? [String: Any],
      let deploymentID = deployment["deploymentId"] as? String, UUID(uuidString: deploymentID) != nil
    else { throw MobileNetworkProviderError.invalidControl }
    return MobileNetworkEnrollment(
      machineID: machineID,
      credential: credential,
      connectionID: connectionID,
      deploymentID: deploymentID)
  }

  private func connect() async throws {
    guard foreground, consent, let api, let credential, let machineID, let connectionID, let deploymentID else {
      throw MobileNetworkProviderError.unavailable
    }
    state = .connecting
    var components = URLComponents(url: api.baseURL, resolvingAgainstBaseURL: false)!
    components.scheme = api.baseURL.scheme == "https" ? "wss" : "ws"
    components.percentEncodedPath = "/api/v0/machines/channel/browser-network"
    guard let url = components.url else { throw MobileNetworkProviderError.connectionFailed }
    var request = URLRequest(url: url)
    request.setValue(mobileNetworkProtocol, forHTTPHeaderField: "Sec-WebSocket-Protocol")
    request.setValue("Bearer \(credential)", forHTTPHeaderField: "Authorization")
    request.setValue(machineID, forHTTPHeaderField: "x-outcomego-machine-id")
    request.setValue(connectionID, forHTTPHeaderField: "x-outcomego-connection-id")
    let configuration = URLSessionConfiguration.ephemeral
    configuration.httpShouldSetCookies = false
    let session = URLSession(configuration: configuration)
    socketSession = session
    let task = session.webSocketTask(with: request)
    socket = task
    routeEpoch = nil
    routeAvailable = false
    highestIncomingStreamID = 0
    receivedSequence = 0
    task.resume()
    receiveNext()
    lastLeaseAt = monotonicMs()
    expiresAt = 0
    try await sendControl(["op": "lease", "nonce": nonce(), "deploymentId": deploymentID, "maxWindow": mobileNetworkWindow])
    leaseTask?.cancel()
    leaseTask = Task { @MainActor [weak self] in
      guard let self else { return }
      while !Task.isCancelled {
        try? await Task.sleep(for: .milliseconds(250))
        let now = self.monotonicMs()
        if self.expiresAt > 0, now >= self.expiresAt {
          await self.fence()
          self.state = .unavailable
        }
        if now - self.lastLeaseAt >= mobileNetworkRenewMs {
          self.lastLeaseAt = now
          try? await self.sendControl(["op": "lease", "nonce": self.nonce(), "deploymentId": deploymentID, "maxWindow": mobileNetworkWindow])
        }
      }
    }
  }

  private func receiveNext() {
    socket?.receive { [weak self] result in
      Task { @MainActor in
        guard let self else { return }
        do {
          switch result {
          case .success(.data(let data)):
            try await self.handleFrame(data)
            self.receiveNext()
          case .success(.string):
            throw MobileNetworkProviderError.invalidFrame
          case .failure:
            throw MobileNetworkProviderError.unavailable
          @unknown default:
            throw MobileNetworkProviderError.unavailable
          }
        } catch {
          await self.stopTransport()
          if self.consent && self.foreground { self.scheduleReconnect() }
        }
      }
    }
  }

  private func handleFrame(_ data: Data) async throws {
    let frame = try decodeFrame(data)
    if frame.type == .control {
      guard let message = try JSONSerialization.jsonObject(with: frame.payload) as? [String: Any],
        let sequence = message["sequence"] as? NSNumber, sequence.int64Value > Int64(receivedSequence)
      else { throw MobileNetworkProviderError.invalidControl }
      receivedSequence = UInt64(sequence.int64Value)
      guard let op = message["op"] as? String else { throw MobileNetworkProviderError.invalidControl }
      switch op {
      case "invalidate":
        await fence()
        try await sendControl(["op": "lease", "nonce": nonce(), "deploymentId": deploymentID ?? "", "maxWindow": mobileNetworkWindow])
      case "lease":
        guard let route = message["route"] as? [String: Any],
          let deployment = route["deploymentId"] as? String, deployment == deploymentID,
          let epoch = route["epoch"] as? String,
          let available = route["available"] as? Bool,
          let revision = route["revision"] as? NSNumber
        else { throw MobileNetworkProviderError.invalidControl }
        if routeEpoch != nil, routeEpoch != epoch { await fence() }
        routeEpoch = epoch
        routeAvailable = available
        expiresAt = monotonicMs() + mobileNetworkLeaseMs
        try await sendControl(["op": "applied", "epoch": epoch, "revision": revision.intValue])
        state = available ? .ready : .unavailable
      default:
        throw MobileNetworkProviderError.invalidControl
      }
      return
    }
    guard let stream = streams[frame.id] else {
      if frame.type == .open {
        try await open(frame)
      } else if frame.id > highestIncomingStreamID {
        throw MobileNetworkProviderError.invalidFrame
      }
      return
    }
    try await stream.receive(frame)
    if stream.closed { streams.removeValue(forKey: frame.id); activeStreams = streams.count }
  }

  private func open(_ frame: MobileNetworkFrame) async throws {
    guard frame.id > highestIncomingStreamID else {
      throw MobileNetworkProviderError.invalidFrame
    }
    highestIncomingStreamID = frame.id
    guard streams.count < 64 else {
      try await sendFrame(.error, id: frame.id, payload: try jsonData(["code": "capacity_exceeded"]))
      return
    }
    guard let object = try JSONSerialization.jsonObject(with: frame.payload) as? [String: Any],
      let host = object["host"] as? String, let port = (object["port"] as? NSNumber)?.intValue,
      let epoch = object["epoch"] as? String, epoch == routeEpoch, routeAvailable,
      (1...65_535).contains(port), publicTargetSyntax(host)
    else {
      try await sendFrame(.error, id: frame.id, payload: try jsonData(["code": "destination_blocked"]))
      return
    }
    do {
      try await requirePublicTarget(host)
    } catch let error as MobileNetworkProviderError {
      let code: String
      switch error {
      case .blocked: code = "destination_blocked"
      case .connectionFailed: code = "dns_failed"
      default: code = "dns_failed"
      }
      try await sendFrame(.error, id: frame.id, payload: try jsonData(["code": code]))
      return
    }
    guard routeAvailable, epoch == routeEpoch else {
      try await sendFrame(.error, id: frame.id, payload: try jsonData(["code": "device_unavailable"]))
      return
    }
    let stream = MobileNetworkStream(
      id: frame.id,
      epoch: epoch,
      connection: NWConnection(host: NWEndpoint.Host(host), port: NWEndpoint.Port(rawValue: UInt16(port))!, using: .tcp),
      send: { [weak self] type, id, payload in
        guard let self else { return }
        try await self.sendFrame(type, id: id, payload: payload)
      },
      onReady: { [weak self] stream in
        guard let self else { return }
        Task { @MainActor in
          guard self.streams[stream.id] === stream, self.routeEpoch == stream.epoch,
            self.routeAvailable else {
            stream.close()
            return
          }
          try? await self.sendFrame(.ready, id: stream.id, payload: Data())
        }
      },
      onSocketEnd: { [weak self] stream, _ in
        guard let self else { return }
        Task { @MainActor in
          guard self.streams[stream.id] === stream else { return }
          self.streams.removeValue(forKey: stream.id)
          self.activeStreams = self.streams.count
        }
      })
    streams[frame.id] = stream
    activeStreams = streams.count
    stream.start()
  }

  private func sendControl(_ object: [String: Any]) async throws {
    var value = object
    sentSequence += 1
    value["sequence"] = sentSequence
    try await sendFrame(.control, id: 0, payload: try jsonData(value))
  }

  private func sendFrame(_ type: MobileNetworkFrameType, id: UInt32, payload: Data) async throws {
    guard let socket else { throw MobileNetworkProviderError.unavailable }
    try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
      socket.send(.data(encodeFrame(type: type, id: id, payload: payload))) { error in
        if let error { continuation.resume(throwing: error) }
        else { continuation.resume() }
      }
    }
  }

  private func fence() async {
    for stream in streams.values { stream.close() }
    streams.removeAll()
    activeStreams = 0
    highestIncomingStreamID = 0
    routeEpoch = nil
    routeAvailable = false
  }

  private func stopTransport() async {
    reconnectTask?.cancel()
    reconnectTask = nil
    leaseTask?.cancel()
    leaseTask = nil
    await fence()
    socket?.cancel(with: .normalClosure, reason: "Network provider stopped".data(using: .utf8))
    socket = nil
    socketSession?.invalidateAndCancel()
    socketSession = nil
    pathMonitor?.cancel()
    pathMonitor = nil
  }

  private func scheduleReconnect() {
    guard reconnectTask == nil, let api else { return }
    reconnectAttempt += 1
    let delay = min(4.0, pow(2.0, Double(min(reconnectAttempt, 2))))
    reconnectTask = Task { @MainActor [weak self] in
      try? await Task.sleep(for: .seconds(delay))
      guard let self else { return }
      self.reconnectTask = nil
      self.start(api: api, allowSharing: false, label: "iPhone")
    }
  }

  private func startPathMonitor() {
    pathMonitor?.cancel()
    let monitor = NWPathMonitor()
    monitor.pathUpdateHandler = { [weak self] path in
      Task { @MainActor in
        guard let self, self.foreground, self.consent else { return }
        if path.status != .satisfied {
          await self.stopTransport()
          self.state = .unavailable
        }
      }
    }
    monitor.start(queue: DispatchQueue(label: "outcomego.mobile-network-path"))
    pathMonitor = monitor
  }

  private func publicTargetSyntax(_ host: String) -> Bool {
    let value = host.lowercased().trimmingCharacters(in: CharacterSet(charactersIn: "."))
    if value.isEmpty || value.count > 253 || value == "localhost" ||
      value.hasSuffix(".localhost") || value.hasSuffix(".local") ||
      value.contains("%") || value.contains("/") || value.contains("@") ||
      value.contains("?") || value.contains("#") || value.contains("\\") ||
      value.unicodeScalars.contains(where: { $0.value < 0x20 || $0.properties.isWhitespace }) {
      return false
    }
    if value.contains(":") {
      return IPv6Address(value) != nil || IPv4Address(value) != nil
    }
    if value.split(separator: ".").count == 4, value.allSatisfy({ $0.isNumber || $0 == "." }) {
      return IPv4Address(value) != nil
    }
    if value.allSatisfy({ $0.isNumber || $0 == "." }) { return false }
    return value.split(separator: ".").allSatisfy { label in
      !label.isEmpty && label.count <= 63 &&
        (label.first!.isLetter || label.first!.isNumber) &&
        (label.last!.isLetter || label.last!.isNumber) &&
        label.allSatisfy { $0.isLetter || $0.isNumber || $0 == "-" }
    }
  }

  private func requirePublicTarget(_ host: String) async throws {
    let value = host.trimmingCharacters(in: CharacterSet(charactersIn: ".")).lowercased()
    if let ipv4 = IPv4Address(value) {
      guard publicAddress(ipv4) else { throw MobileNetworkProviderError.blocked }
      return
    }
    if let ipv6 = IPv6Address(value) {
      guard publicAddress(ipv6) else { throw MobileNetworkProviderError.blocked }
      return
    }
    let addresses = try await resolve(host: value)
    guard !addresses.isEmpty, addresses.allSatisfy({ numeric in
      if let ipv4 = IPv4Address(numeric) { return publicAddress(ipv4) }
      if let ipv6 = IPv6Address(numeric) { return publicAddress(ipv6) }
      return false
    }) else {
      throw MobileNetworkProviderError.blocked
    }
  }

  private func resolve(host: String) async throws -> [String] {
    try await withCheckedThrowingContinuation { continuation in
      DispatchQueue.global(qos: .utility).async {
        var result: UnsafeMutablePointer<addrinfo>?
        let status = getaddrinfo(host, nil, nil, &result)
        guard status == 0, let first = result else {
          if result != nil { freeaddrinfo(result) }
          continuation.resume(throwing: MobileNetworkProviderError.connectionFailed)
          return
        }
        defer { freeaddrinfo(first) }
        var addresses: [String] = []
        var current: UnsafeMutablePointer<addrinfo>? = first
        while let info = current {
          var value = [CChar](repeating: 0, count: Int(NI_MAXHOST))
          let numericStatus = getnameinfo(
            info.pointee.ai_addr,
            info.pointee.ai_addrlen,
            &value,
            socklen_t(value.count),
            nil,
            0,
            NI_NUMERICHOST)
          if numericStatus == 0 {
            addresses.append(String(decoding: value.prefix { $0 != 0 }.map(UInt8.init), as: UTF8.self))
          }
          current = info.pointee.ai_next
        }
        continuation.resume(returning: addresses)
      }
    }
  }

  private func publicAddress(_ address: IPv4Address) -> Bool {
    let bytes = Array(address.rawValue)
    guard bytes.count == 4 else { return false }
    let a = Int(bytes[0]), b = Int(bytes[1]), c = Int(bytes[2])
    return a != 0 && a != 10 && a != 127 && a < 224 &&
      !(a == 100 && (64...127).contains(b)) &&
      !(a == 169 && b == 254) && !(a == 172 && (16...31).contains(b)) &&
      !(a == 192 && b == 168) && !(a == 192 && b == 0 && c == 0) &&
      !(a == 192 && b == 0 && c == 2) && !(a == 198 && b == 18) &&
      !(a == 198 && b == 19) && !(a == 198 && b == 51 && c == 100) &&
      !(a == 203 && b == 0 && c == 113)
  }

  private func publicAddress(_ address: IPv6Address) -> Bool {
    let bytes = Array(address.rawValue)
    guard bytes.count == 16 else { return false }
    if bytes.allSatisfy({ $0 == 0 }) || (bytes.dropLast().allSatisfy({ $0 == 0 }) && bytes[15] == 1) {
      return false
    }
    if bytes[0] & 0xfe == 0xfc || bytes[0] & 0xc0 == 0x80 || bytes[0] == 0xff {
      return false
    }
    if bytes[0] == 0x20 && bytes[1] == 0x01 &&
      ((bytes[2] == 0x0d && bytes[3] == 0xb8) ||
       (bytes[2] == 0x00 && bytes[3] <= 0xff)) {
      return false
    }
    if (bytes[0] == 0x20 && bytes[1] == 0x02) ||
      (bytes[0] == 0x3f && bytes[1] == 0xff) ||
      (bytes[0] == 0x00 && bytes[1] == 0x64 &&
       bytes[2] == 0xff && bytes[3] == 0x9b) {
      return false
    }
    if bytes[0...11].allSatisfy({ $0 == 0 }) && bytes[12] == 0 && bytes[13] == 0 &&
      bytes[14] == 0xff && bytes[15] == 0xff {
      return false
    }
    return true
  }

  private func jsonData(_ value: [String: Any]) throws -> Data {
    try JSONSerialization.data(withJSONObject: value, options: [])
  }

  private func nonce() -> String {
    Data((0..<32).map { _ in UInt8.random(in: UInt8.min...UInt8.max) })
      .base64EncodedString()
      .replacingOccurrences(of: "+", with: "-")
      .replacingOccurrences(of: "/", with: "_")
      .replacingOccurrences(of: "=", with: "")
  }
  private func monotonicMs() -> UInt64 { UInt64(ProcessInfo.processInfo.systemUptime * 1_000) }
}

private struct MobileNetworkEnrollment {
  let machineID: String
  let credential: String
  let connectionID: String
  let deploymentID: String
}

private final class MobileNetworkProviderStore {
  private let service = "dev.outcomego.mobile.swift.network-provider"

  func machineID() throws -> String {
    if let value = try read(account: "machine-id"), !value.isEmpty { return value }
    let value = UUID().uuidString.lowercased()
    try write(value, account: "machine-id")
    return value
  }

  func saveCredential(_ value: String, server: String, machineID: String) throws {
    try write(value, account: "credential:\(server)|\(machineID)")
  }

  func credential(server: String, machineID: String) throws -> String? {
    try read(account: "credential:\(server)|\(machineID)")
  }

  func clearCredential(server: String, machineID: String) throws {
    let query: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service,
      kSecAttrAccount as String: "credential:\(server)|\(machineID)",
    ]
    let status = SecItemDelete(query as CFDictionary)
    guard status == errSecSuccess || status == errSecItemNotFound else {
      throw APIError("Could not clear network provider storage.")
    }
  }

  func setConsent(_ value: Bool, server: String, deploymentID: String) throws {
    try write(value ? "1" : "0", account: "consent:\(server)|\(deploymentID)")
  }

  func consent(server: String, deploymentID: String) -> Bool {
    do {
      guard let value = try read(account: "consent:\(server)|\(deploymentID)") else { return false }
      return value == "1"
    } catch {
      return false
    }
  }

  private func read(account: String) throws -> String? {
    let query: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service,
      kSecAttrAccount as String: account, kSecReturnData as String: true,
    ]
    var result: CFTypeRef?
    let status = SecItemCopyMatching(query as CFDictionary, &result)
    if status == errSecItemNotFound { return nil }
    guard status == errSecSuccess, let data = result as? Data,
      let value = String(data: data, encoding: .utf8) else { throw APIError("Could not read network provider storage.") }
    return value
  }

  private func write(_ value: String, account: String) throws {
    let query: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service,
      kSecAttrAccount as String: account,
    ]
    let data = Data(value.utf8)
    let attributes: [String: Any] = [
      kSecValueData as String: data,
      kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
    ]
    let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
    if status == errSecItemNotFound {
      guard SecItemAdd(query.merging(attributes) { _, new in new } as CFDictionary, nil) == errSecSuccess
      else { throw APIError("Could not save network provider storage.") }
    } else if status != errSecSuccess {
      throw APIError("Could not update network provider storage.")
    }
  }
}

@MainActor
private final class MobileNetworkStream: @unchecked Sendable {
  let id: UInt32
  let epoch: String
  let connection: NWConnection
  private let send: (MobileNetworkFrameType, UInt32, Data) async throws -> Void
  private let onReady: (MobileNetworkStream) -> Void
  private let onSocketEnd: (MobileNetworkStream, Error?) -> Void
  private(set) var closed = false
  private var sendCredit = mobileNetworkWindow
  private var receiveCredit = mobileNetworkWindow
  private var remoteEnded = false
  private var pendingData: [Data] = []
  private var pendingBytes = 0
  private var sendTail: Task<Void, Never>?

  init(id: UInt32, epoch: String, connection: NWConnection,
       send: @escaping (MobileNetworkFrameType, UInt32, Data) async throws -> Void,
       onReady: @escaping (MobileNetworkStream) -> Void,
       onSocketEnd: @escaping (MobileNetworkStream, Error?) -> Void) {
    self.id = id
    self.epoch = epoch
    self.connection = connection
    self.send = send
    self.onReady = onReady
    self.onSocketEnd = onSocketEnd
  }

  func start() {
    connection.stateUpdateHandler = { [weak self] state in
      Task { @MainActor [weak self] in
        guard let self else { return }
        if case .ready = state {
          self.onReady(self)
          self.receive()
        } else if case .failed(let error) = state {
          self.socketEnded(error)
        } else if case .cancelled = state {
          self.socketEnded(nil)
        }
      }
    }
    connection.start(queue: DispatchQueue(label: "outcomego.mobile-network-stream-\(id)"))
  }

  func receive(_ frame: MobileNetworkFrame) async throws {
    guard !closed else { return }
    switch frame.type {
    case .data:
      guard !remoteEnded, frame.payload.count <= receiveCredit else {
        throw MobileNetworkProviderError.invalidFrame
      }
      receiveCredit -= frame.payload.count
      connection.send(content: frame.payload, completion: .contentProcessed { [weak self] _ in
        Task { @MainActor [weak self] in
          guard let self, !self.closed else { return }
          self.receiveCredit += frame.payload.count
          self.enqueue(.window, payload: Self.windowPayload(frame.payload.count))
        }
      })
    case .window:
      guard frame.payload.count == 4 else { throw MobileNetworkProviderError.invalidFrame }
      let count = Int(readUInt32(frame.payload))
      guard count > 0, sendCredit + count <= mobileNetworkWindow else {
        throw MobileNetworkProviderError.invalidFrame
      }
      sendCredit += count
      flush()
    case .fin:
      guard !remoteEnded else { throw MobileNetworkProviderError.invalidFrame }
      remoteEnded = true
      connection.send(
        content: nil,
        contentContext: .finalMessage,
        isComplete: true,
        completion: .contentProcessed { [weak self] _ in
          Task { @MainActor [weak self] in self?.close() }
        })
    case .reset, .error:
      close()
    case .ready, .open, .control:
      throw MobileNetworkProviderError.invalidFrame
    }
  }

  private func receive() {
    guard !closed else { return }
    connection.receive(minimumIncompleteLength: 1, maximumLength: mobileNetworkMaxData) { [weak self] data, _, isComplete, error in
      Task { @MainActor [weak self] in
        guard let self, !self.closed else { return }
        if let data, !data.isEmpty {
          guard self.pendingBytes + data.count <= 16 * 1024 * 1024 else {
            self.socketEnded(MobileNetworkProviderError.connectionFailed)
            return
          }
          self.pendingData.append(data)
          self.pendingBytes += data.count
          self.flush()
        }
        if isComplete || error != nil {
          self.socketEnded(error)
        } else {
          self.receive()
        }
      }
    }
  }

  private func flush() {
    while sendCredit > 0, let data = pendingData.first, !data.isEmpty {
      let count = min(mobileNetworkMaxData, min(sendCredit, data.count))
      let chunk = data.prefix(count)
      sendCredit -= count
      pendingBytes -= count
      if count == data.count {
        pendingData.removeFirst()
      } else {
        pendingData[0] = data.dropFirst(count)
      }
      enqueue(.data, payload: Data(chunk))
    }
  }

  private func enqueue(
    _ type: MobileNetworkFrameType,
    payload: Data,
    after: (() -> Void)? = nil
  ) {
    let previous = sendTail
    sendTail = Task { @MainActor [weak self] in
      _ = await previous?.result
      guard let self else { return }
      try? await self.send(type, self.id, payload)
      after?()
    }
  }

  private func socketEnded(_ error: Error?) {
    guard !closed else { return }
    let type: MobileNetworkFrameType = error == nil ? .fin : .reset
    let payload = error == nil ? Data() : Data(#"{"code":"connect_failed"}"#.utf8)
    enqueue(type, payload: payload) { [weak self] in
      guard let self else { return }
      self.onSocketEnd(self, error)
      self.close()
    }
  }

  func close() {
    guard !closed else { return }
    closed = true
    sendTail?.cancel()
    sendTail = nil
    pendingData.removeAll()
    pendingBytes = 0
    connection.cancel()
  }

  private static func windowPayload(_ count: Int) -> Data {
    Data(UInt32(count).bigEndianBytes)
  }
}

private func encodeFrame(type: MobileNetworkFrameType, id: UInt32, payload: Data) -> Data {
  var data = Data([1, type.rawValue, 0, 0])
  data.append(contentsOf: UInt32(id).bigEndianBytes)
  data.append(contentsOf: UInt32(payload.count).bigEndianBytes)
  data.append(payload)
  return data
}

private func decodeFrame(_ data: Data) throws -> MobileNetworkFrame {
  guard data.count >= 12, data[0] == 1, data[2] == 0, data[3] == 0,
    let type = MobileNetworkFrameType(rawValue: data[1]),
    let id = data.readUInt32(at: 4), let size = data.readUInt32(at: 8),
    Int(size) == data.count - 12,
    Int(size) <= (type == .data ? mobileNetworkMaxData : 4 * 1024)
  else { throw MobileNetworkProviderError.invalidFrame }
  if (type == .control) != (id == 0) { throw MobileNetworkProviderError.invalidFrame }
  if type == .window && size != 4 { throw MobileNetworkProviderError.invalidFrame }
  if type == .fin && size != 0 { throw MobileNetworkProviderError.invalidFrame }
  return MobileNetworkFrame(type: type, id: id, payload: data.subdata(in: 12..<data.count))
}

private func readUInt32(_ data: Data) -> UInt32 { data.readUInt32(at: 0) ?? 0 }

private extension UInt32 {
  var bigEndianBytes: [UInt8] {
    let value = bigEndian
    return [
      UInt8((value >> 24) & 0xff), UInt8((value >> 16) & 0xff),
      UInt8((value >> 8) & 0xff), UInt8(value & 0xff),
    ]
  }
}

private extension Data {
  func readUInt32(at offset: Int) -> UInt32? {
    guard count >= offset + 4 else { return nil }
    return UInt32(self[offset]) << 24 | UInt32(self[offset + 1]) << 16 |
      UInt32(self[offset + 2]) << 8 | UInt32(self[offset + 3])
  }
}
