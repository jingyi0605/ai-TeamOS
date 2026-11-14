# 需求文档 - Android 与 iOS 网络出口提供者实现

状态：IN_PROGRESS

## 简介

Spec 006 已确认 Android 和 iOS 可以在协议层复用 Desktop Provider，但当前移动端没有 machine enrollment、deployment-scoped consent、browser-network WebSocket、multiplexed TCP stream、网络变化处理和稳定的 provider 生命周期。

本 Spec 解决移动端作为网络出口提供者的第一阶段实现问题。移动端不运行 TUN，也不接管设备上的其他应用流量；它只作为服务端认证的 `device` peer，按服务端发送的 `Open` frame 建立公共 TCP 连接，并把连接字节通过现有 relay 返回 Computer。

主要用户是希望临时允许 OutcomeGo 使用 Android 或 iOS 当前网络出口的用户，以及需要在服务端验证移动端出口稳定性的维护者。

## 术语表

- **System**：OutcomeGo 的浏览器网络出口系统。
- **Mobile Provider**：运行在 Android 或 iOS 上、通过 machine credential 建立 device channel 的移动端出口提供者。
- **Provider Engine**：位于 `mobile/shared` 的 Kotlin Multiplatform 共享协议、租约、复用流和状态机实现。
- **Device Channel**：移动端到服务端 `/api/v0/machines/channel/browser-network` 的认证 WebSocket。
- **Route Epoch**：服务端每次出口切换时生成的路由世代标识；epoch 变化时旧流必须失效。
- **Public TCP**：经 DNS 结果和 IP 范围校验后允许访问的公共单播 TCP 目标。
- **前台共享**：用户明确打开共享后，Android 由 Foreground Service 持有 provider；iOS 要求应用保持前台。

## 范围说明

### In Scope

- Kotlin Multiplatform 共享 browser-network frame codec、control message、lease、route epoch、multiplexed stream 和 reconnect 状态机。
- Android machine enrollment、credential/consent 存储、Foreground Service、常驻通知、网络变化和公共 TCP dialer。
- iOS machine enrollment、Keychain/consent 存储、前台 provider 生命周期、WebSocket、网络路径变化和公共 TCP dialer。
- Android 与 iOS 共享同一 `outcomego-browser-network.v1` wire contract，并被服务端识别为 `device` peer。
- 复用现有服务端 `MachineService`、`BrowserNetworkService`、`sourceOrder`、lease 和 route fencing。
- 真实 Android 设备和 iOS 模拟器/真机上的协议、连接恢复和公共 TCP 出口验证。

### Out of Scope

- 系统级 VPN、Packet Tunnel、全设备流量代理或本地应用流量改道。
- 任意 UDP、ICMP、局域网、私有地址、metadata 地址和非单播目标。
- iOS 长期后台 provider、Network Extension entitlement 和 App Store 发布审批。
- Android 自动开机启动、无用户操作的长期共享和绕过厂商省电策略。
- 修改服务端路由选择算法、lease 时长、stream window 或公共地址策略。
- 将移动端作为默认出口或在未完成真实设备验收前自动提升其排名。

## 需求

### 需求 1：共享 Provider Engine

**用户故事：** 作为维护者，我希望 Android 与 iOS 共享同一套协议和状态机，以便两个平台不会产生不同的租约、流控和 route fencing 行为。

#### 验收标准

1. WHEN Provider Engine 编码或解码 browser-network frame THEN System SHALL 遵循现有版本号、frame type、stream id、payload 长度和 control id 约束。
2. WHEN Provider Engine 收到 `lease`、`invalidate`、`Open`、`Ready`、`Data`、`Window`、`Fin` 或 `Reset` THEN System SHALL 使用与现有 Desktop Provider 一致的状态转移和错误码。
3. WHEN route epoch、deployment ID、stream window 或 control sequence 不满足协议约束 THEN System SHALL 拒绝消息、销毁受影响的连接或关闭 device channel。
4. WHEN Provider Engine 在 Android 和 iOS 上运行相同的 common test fixture THEN System SHALL 产生相同的 frame、lease、stream window 和 teardown 结果。

### 需求 2：移动端 enrollment、consent 和 device channel

**用户故事：** 作为已登录用户，我希望明确允许某个服务器使用手机网络，以便移动端 provider 具备可撤销、可追踪的身份和授权边界。

#### 验收标准

1. WHEN 用户已登录并打开共享 THEN System SHALL 使用稳定的安装级 machine ID 调用现有 enrollment API，并保存服务端返回的 machine credential。
2. WHEN enrollment 成功 THEN System SHALL 建立 connection ID，并使用 machine ID、credential、connection ID 和 deployment ID 建立 device channel。
3. WHEN 用户未同意当前 server/deployment、退出登录、撤销设备或服务端拒绝 credential THEN System SHALL 不建立或立即关闭 device channel。
4. WHEN 用户关闭共享 THEN System SHALL 停止 lease renewal、关闭 WebSocket、销毁所有 active stream，并保留可再次打开共享所需的安装身份。
5. WHEN machine credential 或 session 过期 THEN System SHALL 清理失效 credential 并要求下一次用户操作重新 enrollment，不得使用过期凭据无限重试。

### 需求 3：公共 TCP 网络出口

**用户故事：** 作为 Computer 运行时，我希望把目标连接通过已授权移动端发出，以便在移动网络环境下使用移动端的公共出口。

#### 验收标准

1. WHEN device peer 收到有效且当前 epoch 匹配的 `Open` frame THEN System SHALL 对目标 hostname 执行规范化、DNS 查询和公共单播地址校验后建立 TCP 连接。
2. WHEN 目标是 loopback、私有地址、link-local、metadata、特殊用途、非单播、非法 hostname 或解析结果包含被禁止地址 THEN System SHALL 拒绝连接并返回安全的 network error code。
3. WHEN DNS 查询或 TCP 连接在超时、取消、断网或 route epoch 变化后完成 THEN System SHALL 关闭迟到的 socket，不得把它接入旧 stream。
4. WHEN TCP socket 收发数据 THEN System SHALL 通过 Provider Engine 的 `Data`、`Window`、`Fin` 和 `Reset` frame 传输，并遵守单流窗口和总队列上限。
5. WHEN 目标连接失败 THEN System SHALL 只拒绝当前 stream，不修改服务端 `sourceOrder`，不重放用户请求。

### 需求 4：Android 前台 Provider

**用户故事：** 作为 Android 用户，我希望在明确开启网络共享后，即使离开应用界面，provider 仍可在受系统允许的前台生命周期内工作。

#### 验收标准

1. WHEN 用户从前台界面开启共享 THEN System SHALL 启动声明了正确 foreground service type 的 Provider Service，并在规定时间内显示常驻通知。
2. WHEN Provider Service 运行 THEN System SHALL 维护 device channel、lease renewal、network callback 和 stream cleanup，且不依赖 Activity 实例存活。
3. WHEN 用户点击通知中的停止操作、应用退出登录或撤销 consent THEN System SHALL 停止服务并释放 WebSocket、TCP socket、协程和 wake lock 等资源。
4. WHEN Android 网络从 Wi-Fi 切换到移动网络、移动网络切换到 Wi-Fi、短时断网或 VPN 状态改变 THEN System SHALL fence 旧连接并按 bounded backoff 建立新 channel。
5. WHEN Android 设备进入 Doze、省电模式或厂商限制 provider 运行 THEN System SHALL 在 lease 失效后显示不可用状态，不得宣称仍可用或绕过系统限制。

### 需求 5：iOS 前台 Provider

**用户故事：** 作为 iOS 用户，我希望在 OutcomeGo 保持前台时共享当前网络，以便先验证协议和公共 TCP 出口能力。

#### 验收标准

1. WHEN 用户在 iOS 前台打开共享 THEN System SHALL 建立 device channel，并在应用生命周期内执行 lease renewal。
2. WHEN iOS scene 进入 inactive 或 background THEN System SHALL 立即停止或标记 provider 不可用，并关闭无法继续保证租约的旧连接。
3. WHEN iOS scene 回到 active 且用户仍保留 consent THEN System SHALL 重新 enrollment 或 reconnect，并在取得有效 lease 前保持不可用状态。
4. WHEN iOS 网络路径发生变化 THEN System SHALL 取消旧 DNS/TCP 连接和 WebSocket，使用新路径重新建立 provider channel。
5. WHEN iOS 前台 provider 验收完成 THEN System SHALL 明确记录长期后台 provider 仍需独立 Network Extension 评估，不得把前台结果标记为后台支持。

## 非功能需求

### 非功能需求 1：性能

1. WHEN device channel 已连接且 route 已应用 THEN Provider Engine SHALL 在收到 `Open` 后 15 秒内完成 DNS/TCP 尝试或返回明确错误。
2. WHEN lease renewal 运行 THEN System SHALL 以 2 秒目标周期发送 renewal，并在 6 秒 lease 到期前失去续租能力时进入不可用状态。
3. WHEN单个 stream 传输大块数据 THEN System SHALL 遵守现有 128 KiB/256 KiB 协商窗口、64 KiB 单 frame 和 16 MiB 总队列上限。

### 非功能需求 2：可靠性

1. WHEN channel、网络路径、credential、deployment ID 或 route epoch 变化 THEN System SHALL fence 旧 stream，禁止迟到 socket 或迟到 frame 恢复旧路由。
2. WHEN服务端暂时不可达 THEN System SHALL 使用有上限的 reconnect backoff，不创建无限协程或无限 pending dial。
3. WHEN用户关闭共享或系统销毁平台宿主 THEN System SHALL 具备幂等清理路径，重复停止不得崩溃或泄漏 socket。
4. WHEN provider 不可用 THEN 服务端已有 ranked source failover SHALL 继续生效，移动端不得修改或绕过服务端选择权威。

### 非功能需求 3：安全与隐私

1. WHEN 保存 machine credential、session token 或 consent THEN System SHALL 使用 Android Keystore/加密存储或 iOS Keychain，并且日志不得输出秘密。
2. WHEN 连接目标被拒绝 THEN System SHALL 返回有限错误码，不向服务端或 UI 暴露未验证 DNS 地址列表中的敏感细节。
3. WHEN device channel 接受 frame THEN System SHALL 验证 machine credential、connection ID、deployment ID、control sequence 和 route epoch。
4. WHEN移动端作为 provider THEN System SHALL 只提供公共 TCP，不暴露手机局域网、文件、浏览器 cookies 或本地服务。

### 非功能需求 4：可维护性

1. WHEN 调整协议、流控或 lease 状态机 THEN System SHALL 优先修改 `mobile/shared` common code 和 common tests，不复制 Android/iOS 两份逻辑。
2. WHEN 调整系统生命周期 THEN System SHALL 只修改对应平台 adapter，并通过接口测试验证共享 engine 行为不变。
3. WHEN 问题排查 THEN System SHALL 区分 enrollment、consent、channel、lease、route、DNS、TCP 和平台生命周期状态。

## 成功定义

- Android 和 iOS 都能用稳定 machine ID 完成 enrollment、consent、device channel 和 lease renewal。
- Android 前台 Provider 能通过服务端 relay 建立公共 TCP 连接，并在网络切换、断线和 route epoch 变化时安全恢复或失败关闭。
- iOS 前台 Provider 能在 active scene 内完成同一协议链路，并在进入后台时明确降级为不可用。
- Kotlin Multiplatform common tests 覆盖 frame、lease、mux、窗口、取消、epoch fencing 和错误路径。
- Android 真机和 iOS 真机/模拟器验证了公共 TCP、断网恢复、目标策略拒绝和资源清理。
- Spec 006 的移动端“仅评估”边界被更新为“前台 Provider 第一阶段实现”；长期后台能力仍有独立边界和验收前提。
