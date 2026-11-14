# 任务清单 - Android 与 iOS 网络出口提供者实现（人话版）

状态：IN_PROGRESS

## 这份文档是干什么的

这份任务清单把移动端网络出口 Provider 拆成可以逐步验证的工作。目标是先完成共享 Kotlin Provider Engine，再接入 Android 前台服务和 iOS 前台宿主，最后通过服务端回放和真实设备验证公共 TCP 出口。

本 Spec 不把 iOS 长期后台 Provider、Network Extension、系统级 VPN 或任意 UDP 加入实现任务。

## 状态说明

- `TODO`：还没开始
- `IN_PROGRESS`：正在做
- `BLOCKED`：被外部问题卡住
- `IN_REVIEW`：已经有结果，等复核
- `DONE`：已经完成，并且已经回写状态
- `CANCELLED`：取消，不做了，但要写原因

规则：

- 只有 `状态：DONE` 的任务才能勾选成 `[x]`
- `BLOCKED` 必须写清楚卡在哪里
- `CANCELLED` 必须写清楚为什么不做
- 每做完一个任务，必须立刻更新这里

---

## 阶段 1：Spec 基线和协议边界

- [x] 1.1 建立 006.1 Spec 文档结构
  - 状态：DONE
  - 这一步到底做什么：从 `specs/000-Spec规范/Spec模板` 建立 006.1 的 README、requirements、design、tasks 和 docs 目录。
  - 做完你能看到什么：`specs/006.1-Android与iOS网络出口提供者实现/` 下有完整的四份主文档和实现边界文档。
  - 先依赖什么：无
  - 开始前先看：`specs/000-Spec规范/Spec模板/README.md`
  - 主要改哪里：
    - `specs/006.1-Android与iOS网络出口提供者实现/README.md`
    - `specs/006.1-Android与iOS网络出口提供者实现/requirements.md`
    - `specs/006.1-Android与iOS网络出口提供者实现/design.md`
    - `specs/006.1-Android与iOS网络出口提供者实现/tasks.md`
    - `specs/006.1-Android与iOS网络出口提供者实现/docs/`
  - 这一步先不做什么：不修改业务代码，不安装依赖，不启动服务。
  - 怎么算完成：
    1. 文档目录和模板文件齐全。
    2. 主文档之间和上游 Spec 006 之间有明确引用。
  - 怎么验证：
    - `find specs/006.1-Android与iOS网络出口提供者实现 -maxdepth 2 -type f | sort`
    - 人工检查 README、requirements、design、tasks 的链接和状态。
  - 对应需求：`requirements.md` 全部需求
  - 对应设计：`design.md` §1、§2

- [x] 1.2 完成现状和协议契约审计
  - 状态：DONE
  - 这一步到底做什么：核对现有 TypeScript browser-network 协议、服务端 enrollment/relay、Android KMP 模块和 iOS Swift 工程边界。
  - 做完你能看到什么：实现任务明确哪些逻辑必须进入 `mobile/shared`，哪些能力必须留在 Android/iOS adapter。
  - 先依赖什么：1.1
  - 开始前先看：
    - `specs/006-Windows与移动端网络出口及电脑操作界面选择/docs/20261113-移动端网络出口可行性评估.md`
    - `packages/plugin-sdk/src/browser-network/protocol.ts`
    - `packages/plugin-sdk/src/browser-network/policy.ts`
    - `apps/server/src/services/browser-network-service.ts`
    - `apps/server/src/services/machine-service.ts`
    - `mobile/shared/build.gradle.kts`
    - `apps/ios/Package.swift`
  - 主要改哪里：
    - `specs/006.1-Android与iOS网络出口提供者实现/docs/20261114-移动端网络出口实现边界.md`
    - 当前 Spec 文档
  - 这一步先不做什么：不复制 Node `BrowserNetworkClient`，不把 iOS 后台能力标记为已支持。
  - 怎么算完成：
    1. enrollment、connection ID、deployment ID、lease、stream 和 public-only 边界已记录。
    2. Android 前台和 iOS 前台实现边界已写清楚。
  - 怎么验证：
    - `rg -n "enroll|channel/connect|browser-network-identity|browser-network.v1|LEASE_MS|MAX_STREAMS|publicOnly" apps/server packages/plugin-sdk apps/desktop`
    - 人工对照 Spec 006 的移动端评估。
  - 对应需求：`requirements.md` 需求 1、需求 2、需求 3、需求 4、需求 5
  - 对应设计：`design.md` §1.3、§2.2、§3.3、§4.1

### 阶段检查

- [x] 1.3 Spec 基线检查
  - 状态：DONE
  - 这一步到底做什么：确认 006.1 已经从可行性评估收敛为可执行的 Android/iOS 前台 Provider 实现范围。
  - 做完你能看到什么：可以开始写共享 Provider Engine，不会把生命周期问题隐藏在协议代码里。
  - 先依赖什么：1.1、1.2
  - 开始前先看：
    - `requirements.md`
    - `design.md`
    - `tasks.md`
    - `docs/20261114-移动端网络出口实现边界.md`
  - 主要改哪里：当前 Spec 全部文档
  - 这一步先不做什么：不添加新需求，不进入平台实现。
  - 怎么算完成：
    1. 每条需求都有设计章节和任务入口。
    2. Android/iOS 后台承诺、公共 TCP 边界和 KMP 分层没有歧义。
  - 怎么验证：
    - 人工逐项检查 requirements → design → tasks 追踪关系。
  - 对应需求：`requirements.md` 全部需求
  - 对应设计：`design.md` §1、§2、§6、§7

---

## 阶段 2：Kotlin Multiplatform Provider Engine

- [x] 2.1 实现共享 frame codec 和协议模型
  - 状态：DONE
  - 这一步到底做什么：把现有 TypeScript frame header、frame type、payload 限制、control message、route 和 OpenTarget 模型移植到 `mobile/shared/src/commonMain`。
  - 做完你能看到什么：Android 和 iOS 可以使用同一套 Kotlin frame 编码、解码和结构校验。
  - 先依赖什么：1.3
  - 开始前先看：
    - `requirements.md` 需求 1
    - `design.md` §3.1、§3.2、§3.3.4
    - `packages/plugin-sdk/src/browser-network/protocol.ts`
    - `mobile/shared/src/commonMain/kotlin/com/outcomego/mobile/core/LiveVoice.kt`
  - 主要改哪里：
    - `mobile/shared/src/commonMain/kotlin/com/outcomego/mobile/network/NetworkProtocol.kt`
    - `mobile/shared/src/commonTest/kotlin/com/outcomego/mobile/network/NetworkProtocolTest.kt`
  - 这一步先不做什么：不实现 WebSocket、不实现 TCP dialer、不改变服务端 TypeScript 协议。
  - 怎么算完成：
    1. 合法 frame 与 TypeScript fixture 双向兼容。
    2. 非法版本、type、id、payload、Window 和 Fin 输入被拒绝。
    3. JSON control message 的 sequence、nonce、route 和 error code 有边界校验。
  - 怎么验证：
    - `./gradlew :mobile:shared:allTests`
    - 使用固定 binary fixture 与 `packages/plugin-sdk/test/browser-network/protocol.test.ts` 对照。
  - 对应需求：`requirements.md` 需求 1，验收 1.1、1.3、1.4
  - 对应设计：`design.md` §3.1、§3.2、§6.1

- [x] 2.2 实现共享 mux 和 stream flow control
  - 状态：DONE
  - 这一步到底做什么：实现 64 条 stream 上限、stream ID、Ready/Error/Data/Window/Fin/Reset、credit、half-close、取消和 16 MiB 队列预算。
  - 做完你能看到什么：不依赖 Android/iOS socket API 的情况下，可以在 fake transport 上完成完整 multiplexed stream 回放。
  - 先依赖什么：2.1
  - 开始前先看：
    - `requirements.md` 需求 1、需求 3
    - `design.md` §2.3.3、§3.1、§4.2、§6.2
    - `packages/plugin-sdk/src/browser-network/mux.ts`
    - `packages/plugin-sdk/test/browser-network/mux.test.ts`
  - 主要改哪里：
    - `mobile/shared/src/commonMain/kotlin/com/outcomego/mobile/network/NetworkMux.kt`
    - `mobile/shared/src/commonMain/kotlin/com/outcomego/mobile/network/ProviderSocket.kt`
    - `mobile/shared/src/commonTest/kotlin/com/outcomego/mobile/network/NetworkMuxTest.kt`
  - 这一步先不做什么：不接真实 WebSocket，不实现平台通知或 service。
  - 怎么算完成：
    1. 大块双向数据传输严格遵守窗口 credit。
    2. stream cancellation、half-close、RESET 和迟到 socket 不泄漏状态。
    3. 超过 64 streams、窗口或队列预算时返回确定错误。
  - 怎么验证：
    - `./gradlew :mobile:shared:allTests`
    - 复用 TypeScript mux test 的输入场景建立 Kotlin common fixtures。
  - 对应需求：`requirements.md` 需求 1、需求 3，验收 1.2、1.3、3.4
  - 对应设计：`design.md` §2.3.3、§3.1、§4.2、§6.2、§7.1

- [x] 2.3 实现共享 lease、route epoch 和 reconnect 状态机
  - 状态：DONE
  - 这一步到底做什么：实现 ProviderSession 的 enrollment 后连接、lease challenge、2 秒 renewal、6 秒 expiry、invalidate、applied、epoch fencing 和 bounded reconnect。
  - 做完你能看到什么：Provider Engine 可以在 fake WebSocket 上从 stopped 进入 ready，并在 route/lease/network 变化时安全恢复或失效。
  - 先依赖什么：2.1、2.2
  - 开始前先看：
    - `requirements.md` 需求 1、需求 2、需求 4、需求 5
    - `design.md` §2.3.2、§2.3.4、§2.3.5、§4.2、§6.2、§6.5
    - `packages/plugin-sdk/src/browser-network/client.ts`
    - `apps/server/src/services/browser-network-service.ts`
  - 主要改哪里：
    - `mobile/shared/src/commonMain/kotlin/com/outcomego/mobile/network/ProviderSession.kt`
    - `mobile/shared/src/commonMain/kotlin/com/outcomego/mobile/network/ProviderState.kt`
    - `mobile/shared/src/commonTest/kotlin/com/outcomego/mobile/network/ProviderSessionTest.kt`
  - 这一步先不做什么：不决定 Android foreground service type，不实现 iOS scene binding。
  - 怎么算完成：
    1. lease renewal 使用可注入 Clock，测试可确定复现过期。
    2. epoch 变化会销毁旧 stream、取消 pending dial 并拒绝迟到结果。
    3. reconnect backoff 有上限，停止后不再启动新任务。
    4. credential revoke 和 consent revoke 能进入 revoked/stopped。
  - 怎么验证：
    - `./gradlew :mobile:shared:allTests`
    - 增加 wall-clock jump、late callback、duplicate control 和 stop/restart 测试。
  - 对应需求：`requirements.md` 需求 1、需求 2、需求 4、需求 5
  - 对应设计：`design.md` §2.3.2、§2.3.4、§2.3.5、§4.2、§6.2、§6.5

- [x] 2.4 实现共享 public target policy 和 enrollment client
  - 状态：DONE
  - 这一步到底做什么：实现 hostname canonicalization、DNS 全结果公共地址校验、TCP 目标检查，以及 enrollment/connect/deployment identity 的请求模型和响应校验。
  - 做完你能看到什么：Android/iOS adapter 只需提供 HTTP/DNS/TCP 能力，就能共享同一套安全规则。
  - 先依赖什么：2.1、2.3
  - 开始前先看：
    - `requirements.md` 需求 2、需求 3
    - `design.md` §2.3.1、§2.3.3、§3.3.1 至 §3.3.5、§5.1
    - `packages/plugin-sdk/src/browser-network/policy.ts`
    - `apps/server/src/services/machine-service.ts`
  - 主要改哪里：
    - `mobile/shared/src/commonMain/kotlin/com/outcomego/mobile/network/PublicTargetPolicy.kt`
    - `mobile/shared/src/commonMain/kotlin/com/outcomego/mobile/network/MobileEnrollment.kt`
    - `mobile/shared/src/commonTest/kotlin/com/outcomego/mobile/network/PublicTargetPolicyTest.kt`
    - `mobile/shared/src/commonTest/kotlin/com/outcomego/mobile/network/MobileEnrollmentTest.kt`
  - 这一步先不做什么：不实现平台 Keychain/Keystore，不读取真实网络接口。
  - 怎么算完成：
    1. 混合公共/私有 DNS 结果不会建立连接。
    2. IPv4、IPv6、特殊用途和非法 hostname 行为与现有 policy test 一致。
    3. enrollment response、machine ID、credential、connection ID 和 deployment ID 有严格校验。
  - 怎么验证：
    - `./gradlew :mobile:shared:allTests`
    - 与 `packages/plugin-sdk/test/browser-network/protocol.test.ts` 的 policy cases 对照。
  - 对应需求：`requirements.md` 需求 2、需求 3，验收 2.1 至 2.5、3.1 至 3.3
  - 对应设计：`design.md` §2.3.1、§2.3.3、§3.2.3、§3.3.1 至 §3.3.5、§5.1

### 阶段检查

- [ ] 2.5 KMP Provider Engine 阶段检查
  - 状态：BLOCKED
  - 阻塞原因：common source 和 Android target 已通过编译及测试，但 Kotlin/Native framework/test link 需要 Xcode；当前环境的 `xcodebuild -version` 不可用。
  - 这一步到底做什么：确认 shared module 已经可以在 fake platform transport 上完成一条完整 Provider 主链路。
  - 做完你能看到什么：进入平台开发时，Android/iOS 只需要实现 transport、storage 和 lifecycle，不再复制协议状态机。
  - 先依赖什么：2.1、2.2、2.3、2.4
  - 开始前先看：
    - `requirements.md`
    - `design.md` §2、§3、§4、§6、§7
    - `tasks.md` 阶段 2 全部任务
  - 主要改哪里：
    - `mobile/shared/src/commonMain/`
    - `mobile/shared/src/commonTest/`
    - 必要时更新 `mobile/README.md`
  - 这一步先不做什么：不开始 Android/iOS UI 和后台生命周期实现。
  - 怎么算完成：
    1. common tests 覆盖 frame、lease、mux、policy、enrollment、cancel 和 epoch fencing。
    2. shared module 能完成 Android target 和 iOS framework 构建。
  - 怎么验证：
    - `./gradlew :mobile:shared:allTests :mobile:shared:assemble`
    - `./gradlew :mobile:shared:linkDebugFrameworkIosSimulatorArm64`
  - 对应需求：`requirements.md` 需求 1、需求 2、需求 3
  - 对应设计：`design.md` §2.1、§2.2、§3、§6、§7.1

---

## 阶段 3：Android 前台 Provider

- [x] 3.1 接入 Android KMP framework 和平台 adapter
  - 状态：DONE
  - 这一步到底做什么：在 Android app 中接入 shared Provider Engine，并实现 OkHttp WebSocket、TCP socket、HTTP enrollment 和加密存储 adapter。
  - 做完你能看到什么：Android 可以在测试入口中启动 Provider Engine，使用真实 server/session 完成 enrollment 和 device channel。
  - 先依赖什么：2.5
  - 开始前先看：
    - `requirements.md` 需求 2、需求 3
    - `design.md` §2.2、§2.3.1、§3.3.1 至 §3.3.5
    - `apps/android/app/build.gradle.kts`
    - `apps/android/app/src/main/kotlin/com/outcomego/platform/AndroidLiveVoiceSession.kt`
    - `apps/android/app/src/main/java/com/outcomego/android/data/AndroidCoreAdapters.kt`
  - 主要改哪里：
    - `apps/android/app/build.gradle.kts`
    - `apps/android/app/src/main/kotlin/com/outcomego/platform/AndroidProviderTransport.kt`
    - `apps/android/app/src/main/kotlin/com/outcomego/platform/AndroidNetworkProvider.kt`
    - `apps/android/app/src/main/java/com/outcomego/android/data/AndroidProviderStore.kt`
    - `apps/android/app/src/test/`
  - 这一步先不做什么：不启动 Foreground Service，不修改 Android manifest 的最终 service type。
  - 怎么算完成：
    1. WebSocket binary message 不被误当成 text message。
    2. TCP socket 可取消、可关闭，并能映射到 shared ProviderSocket。
    3. machine ID、credential 和 consent 使用安全存储，日志无秘密。
  - 怎么验证：
    - `./gradlew :mobile:shared:allTests :mobile:androidApp:testDebugUnitTest`
    - adapter fake transport tests。
  - 对应需求：`requirements.md` 需求 2、需求 3
  - 对应设计：`design.md` §2.2、§2.3.1、§2.3.3、§3.3.5、§4.1

- [ ] 3.2 实现 Android Provider Foreground Service 和通知
  - 状态：IN_REVIEW
  - 当前结果：代码、Manifest、APK 和通知 channel 已完成；已在 `10.255.0.204:5555` 安装并启动应用。设备没有登录 session，无法从 shell 触发真实 consent/enrollment/service channel 回放。
  - 这一步到底做什么：增加用户主动启动的 Provider Service、状态通知、停止操作和幂等 cleanup。
  - 做完你能看到什么：Android 用户开启共享后，通知栏显示 provider 状态；点击 Stop Sharing 可以立即关闭 channel 和 streams。
  - 先依赖什么：3.1
  - 开始前先看：
    - `requirements.md` 需求 4
    - `design.md` §2.3.4、§3.1、§4.2、§5.3
    - `apps/android/app/src/main/AndroidManifest.xml`
    - `apps/android/app/src/main/kotlin/com/outcomego/platform/AndroidLiveVoiceForegroundService.kt`
    - `apps/android/app/src/main/kotlin/com/outcomego/platform/AndroidNotifications.kt`
  - 主要改哪里：
    - `apps/android/app/src/main/AndroidManifest.xml`
    - `apps/android/app/src/main/kotlin/com/outcomego/platform/AndroidNetworkProvider.kt`
    - `apps/android/app/src/main/kotlin/com/outcomego/platform/AndroidNotifications.kt`
    - `apps/android/app/src/test/`
  - 这一步先不做什么：不承诺 Android 15 无限时长，不自动开机启动，不使用普通后台 task 维持 lease。
  - 怎么算完成：
    1. Service 在正确的 foreground service type 和权限下启动。
    2. notification 有状态、打开设置/应用入口和 Stop Sharing action。
    3. service stop、logout、revoke、onDestroy 和重复 stop 都不会泄漏 transport、socket 或 coroutine。
  - 怎么验证：
    - `bash scripts/android/package-debug.sh`
    - `adb -s 10.255.0.204:5555 shell dumpsys activity services com.outcomego.android.debug`
    - `adb -s 10.255.0.204:5555 shell dumpsys notification --noredact`
  - 对应需求：`requirements.md` 需求 4，验收 4.1、4.2、4.3
  - 对应设计：`design.md` §2.3.4、§3.1、§4.2、§5.3、§7.2

- [ ] 3.3 接入 Android 网络变化和平台限制状态
  - 状态：IN_REVIEW
  - 当前结果：`ConnectivityManager.NetworkCallback`、网络切换 fence、恢复重连和前台状态通知已实现；真实 Wi-Fi/移动网络/Doze 回放仍需已登录设备和服务端联调。
  - 这一步到底做什么：使用 `ConnectivityManager.NetworkCallback` 处理网络切换、断网、VPN 变化和恢复，并将 Doze/省电等不可用状态映射到 ProviderSnapshot。
  - 做完你能看到什么：Wi-Fi 断开或切换网络时，旧 channel/streams 被 fence；新网络恢复后 provider 按 backoff reconnect。
  - 先依赖什么：3.2
  - 开始前先看：
    - `requirements.md` 需求 4，非功能需求 2
    - `design.md` §2.3.4、§4.2、§5.3、§6.2、§6.5
    - Android 当前设备基线和 `docs/20261114-移动端网络出口实现边界.md`
  - 主要改哪里：
    - `apps/android/app/src/main/kotlin/com/outcomego/platform/AndroidNetworkMonitor.kt`
    - `apps/android/app/src/main/kotlin/com/outcomego/platform/AndroidNetworkProvider.kt`
    - `apps/android/app/src/test/`
    - `apps/android/app/src/androidTest/`
  - 这一步先不做什么：不绕过 Doze、不申请系统 VPN、不把不可用状态伪装成 ready。
  - 怎么算完成：
    1. callback 变化会取消旧 dial 和 old WebSocket。
    2. provider 无有效 lease 时服务端能 failover。
    3. 回到可用网络后不会重复建立多个 channel。
  - 怎么验证：
    - Android unit/instrumentation tests。
    - 使用 ADB 执行 Wi-Fi/airplane/Doze 受控回放，并检查服务端 lease 和 provider snapshot。
  - 对应需求：`requirements.md` 需求 4，验收 4.4、4.5
  - 对应设计：`design.md` §2.3.4、§4.2、§5.3、§6.2、§6.5、§7.3

### 阶段检查

- [ ] 3.4 Android Provider 阶段检查
  - 状态：TODO
  - 这一步到底做什么：在真实 Android 设备上完成 enrollment、consent、lease、公共 TCP、断线恢复和清理回放。
  - 做完你能看到什么：Android Provider 能被服务端识别为 device，Computer 能通过它建立公共 TCP，断线时能安全 failover。
  - 先依赖什么：3.1、3.2、3.3
  - 开始前先看：
    - `requirements.md` 需求 2、需求 3、需求 4
    - `design.md` §7.2、§7.3、§8
    - `docs/20261114-移动端网络出口实现边界.md`
  - 主要改哪里：Android 阶段全部相关代码和测试证据文档。
  - 这一步先不做什么：不开始 iOS 实现，不宣布 Android 后台长期稳定。
  - 怎么算完成：
    1. 真实设备成功完成公共 TCP round-trip 和 egress source/epoch 校验。
    2. Wi-Fi/移动网络/Doze/停止共享/注销登录路径均有证据。
    3. API 35+ foreground service 限制已记录并决定是否阻塞发布。
  - 怎么验证：
    - `bash scripts/android/package-debug.sh`
    - ADB 真机回放。
    - Server browser-network integration tests。
  - 对应需求：`requirements.md` 需求 2、需求 3、需求 4
  - 对应设计：`design.md` §2.3.1、§2.3.3、§2.3.4、§7.2、§7.3、§8

---

## 阶段 4：iOS 前台 Provider

- [ ] 4.1 将 KMP shared framework 接入 iOS 工程
  - 状态：BLOCKED
  - 阻塞原因：Kotlin/Native framework link 需要 Xcode；当前环境不能执行 `xcodebuild -version`。当前 iOS Provider 使用 Swift 平台实现，并未把 shared framework 接入 Xcode 工程。
  - 这一步到底做什么：让现有 Swift iOS app 构建并调用 `OutcomeGoShared` framework，保留现有 Swift UI 和 Core models。
  - 做完你能看到什么：iOS 可以从 Swift 调用 shared Provider Engine 的配置、状态和启动/停止接口。
  - 先依赖什么：2.5
  - 开始前先看：
    - `requirements.md` 需求 1、需求 5
    - `design.md` §1.3、§2.2、§2.3.5
    - `mobile/shared/build.gradle.kts`
    - `mobile/README.md`
    - `apps/ios/Package.swift`
    - `apps/ios/scripts/generate-project.py`
  - 主要改哪里：
    - `apps/ios/Package.swift` 或 Xcode project generation inputs
    - `apps/ios/scripts/`
    - `apps/ios/Sources/App/` provider bridge
    - `mobile/shared/` framework export surface
  - 这一步先不做什么：不重写现有 Swift UI，不实现 Network Extension。
  - 怎么算完成：
    1. iOS simulator framework 构建成功。
    2. Swift 可以创建 shared Provider Engine 并订阅 ProviderSnapshot。
    3. framework 集成不破坏现有 iOS Core tests 和 app build。
  - 怎么验证：
    - `./gradlew :mobile:shared:linkDebugFrameworkIosSimulatorArm64`
    - `swift test --package-path apps/ios`
    - 按项目现有脚本生成并构建 iOS project。
  - 对应需求：`requirements.md` 需求 1、需求 5
  - 对应设计：`design.md` §1.3、§2.2、§2.3.5、§7.2

- [ ] 4.2 实现 iOS WebSocket、TCP、Keychain 和 enrollment adapter
  - 状态：IN_REVIEW
  - 当前结果：Swift 前台 Provider 已实现对应 adapter、全量 DNS 公共地址检查和流控；shared framework 接入仍受 4.1 阻塞。
  - 这一步到底做什么：提供 iOS 平台的 binary WebSocket、public TCP、HTTP enrollment、Keychain credential 和 consent 实现。
  - 做完你能看到什么：iOS active scene 可以建立和关闭 device channel，且 shared engine 不依赖 Swift transport 细节。
  - 先依赖什么：4.1
  - 开始前先看：
    - `requirements.md` 需求 2、需求 3、需求 5
    - `design.md` §2.2、§2.3.1、§2.3.3、§3.3、§4.1
    - `apps/ios/Sources/Core/LiveVoiceSession.swift`
    - `apps/ios/Sources/App/SecureSession.swift`
    - `apps/ios/Sources/Core/BrowserNetwork.swift`
  - 主要改哪里：
    - `apps/ios/Sources/Core/MobileNetworkProvider.swift`
    - `apps/ios/Tests/CoreTests/`
  - 这一步先不做什么：不在 background scene 保持 WebSocket，不申请 Network Extension entitlement。
  - 怎么算完成：
    1. WebSocket binary callback 和 socket callback 能安全取消。
    2. Keychain 中 credential/consent 与 session/account/server scope 清楚。
    3. public-only policy 使用 shared code，不在 Swift 中复制第二套策略。
  - 怎么验证：
    - `swift test --package-path apps/ios`
    - iOS adapter fake transport tests。
    - simulator active scene channel test。
  - 对应需求：`requirements.md` 需求 2、需求 3、需求 5
  - 对应设计：`design.md` §2.2、§2.3.1、§2.3.3、§3.3、§4.1

- [ ] 4.3 接入 iOS scene 和 network path 生命周期
  - 状态：IN_REVIEW
  - 当前结果：active/inactive 生命周期、NWPathMonitor、旧 transport fence 和恢复重连已接入；真机/模拟器生命周期回放受 Xcode 环境限制。
  - 这一步到底做什么：将 provider 与 active/inactive/background scene 和网络路径变化绑定，保证后台时不伪造 ready。
  - 做完你能看到什么：iOS 前台共享可以运行；离开前台后 provider 明确 unavailable；回到前台后可恢复。
  - 先依赖什么：4.2
  - 开始前先看：
    - `requirements.md` 需求 5，非功能需求 2
    - `design.md` §2.3.5、§4.2、§5.3、§6.5
    - `apps/ios/Sources/App/OutcomeGoApp.swift`
    - `apps/ios/Sources/App/BrowserNetworkSettingsView.swift`
    - `apps/ios/Sources/Core/BrowserNetwork.swift`
  - 主要改哪里：
    - `apps/ios/Sources/App/MobileNetworkProviderHost.swift`
    - `apps/ios/Sources/App/OutcomeGoApp.swift`
    - `apps/ios/Sources/Core/NetworkPathMonitor.swift`
    - `apps/ios/Tests/CoreTests/`
    - `apps/ios/Tests/UITests/`
  - 这一步先不做什么：不把 active scene 测试结果扩展为长期后台支持。
  - 怎么算完成：
    1. background/inactive 触发 stop renewal、close/fence 和 unavailable。
    2. active 恢复后只建立一个 channel，并重新应用有效 route。
    3. path change 不会留下旧 WebSocket、TCP socket 或 pending dial。
  - 怎么验证：
    - iOS lifecycle unit tests。
    - iOS simulator active/background/active UI tests。
    - 真机网络切换回放。
  - 对应需求：`requirements.md` 需求 5，验收 5.1 至 5.5
  - 对应设计：`design.md` §2.3.5、§4.2、§5.3、§6.2、§6.5、§7.3

### 阶段检查

- [ ] 4.4 iOS Provider 阶段检查
  - 状态：TODO
  - 这一步到底做什么：在 iOS active scene 内完成 enrollment、consent、lease、公共 TCP、path change 和 background stop 回放。
  - 做完你能看到什么：iOS 前台 Provider 的支持边界有真实证据，后台行为没有被误报。
  - 先依赖什么：4.1、4.2、4.3
  - 开始前先看：
    - `requirements.md` 需求 2、需求 3、需求 5
    - `design.md` §7.2、§7.3、§8
    - `docs/20261114-移动端网络出口实现边界.md`
  - 主要改哪里：iOS 阶段全部相关代码、测试和证据文档。
  - 这一步先不做什么：不实现 Network Extension，不承诺长期后台 provider。
  - 怎么算完成：
    1. active scene 能完成公共 TCP round-trip 和 egress source/epoch 校验。
    2. background 后 lease 和 stream 清理可验证。
    3. 回到 active 后 reconnect 和 consent 行为可验证。
  - 怎么验证：
    - `swift test --package-path apps/ios`
    - iOS simulator/真机回放。
    - Server browser-network integration tests。
  - 对应需求：`requirements.md` 需求 2、需求 3、需求 5
  - 对应设计：`design.md` §2.3.1、§2.3.3、§2.3.5、§7.2、§7.3、§8

---

## 阶段 5：服务端联调、设备验收和文档回写

- [ ] 5.1 增加 Server 与移动端 Provider 的联调 fixture
  - 状态：TODO
  - 这一步到底做什么：使用 fake Android/iOS device peer 接入现有 BrowserNetworkService，验证 enrollment credential、connection ID、lease、Open、Data、Window、Fin、Reset、failover 和 epoch fence。
  - 做完你能看到什么：服务端不需要为 Android/iOS 复制第二套路由协议，shared engine 可以直接接入现有 relay。
  - 先依赖什么：2.5、3.4 或 4.4 任一平台完成基础 channel
  - 开始前先看：
    - `requirements.md` 需求 1、需求 2、需求 3
    - `design.md` §2.1、§2.3、§6.1、§6.2、§7.2
    - `apps/server/test/browser-network.test.ts`
    - `packages/plugin-sdk/test/browser-network/peer.fixture.ts`
  - 主要改哪里：
    - `apps/server/test/browser-network.mobile.test.ts`
    - `mobile/shared/src/commonTest/`
    - 必要时更新 server test fixture
  - 这一步先不做什么：不修改 BrowserNetworkService 的选路算法，不新增移动端专用 route 模型。
  - 怎么算完成：
    1. Android/iOS fake peer 都能完成一条完整 stream round-trip。
    2. provider disconnect、lease expiry、epoch switch 和 late dial 都不会泄漏旧流。
    3. public-only reject 和 capacity reject 与 Desktop provider 一致。
  - 怎么验证：
    - `bun test apps/server/test/browser-network.mobile.test.ts apps/server/test/browser-network.test.ts`
    - `./gradlew :mobile:shared:allTests`
  - 对应需求：`requirements.md` 需求 1、需求 2、需求 3
  - 对应设计：`design.md` §2.1、§2.3、§6.1、§6.2、§7.2

- [ ] 5.2 完成 Android 真机 egress 验收
  - 状态：TODO
  - 这一步到底做什么：在授权 Android 设备上验证从用户 consent 到 Computer 通过移动端公共出口的完整链路。
  - 做完你能看到什么：有可复核的设备日志、服务端状态、egress source/epoch 和故障恢复证据。
  - 先依赖什么：3.4、5.1
  - 开始前先看：
    - `requirements.md` 需求 2、需求 3、需求 4
    - `design.md` §7.3、§8
    - `docs/20261114-移动端网络出口实现边界.md`
  - 主要改哪里：
    - `specs/006.1-Android与iOS网络出口提供者实现/docs/20261114-Android真机验收记录.md`
    - 必要时更新 Android instrumentation tests
  - 这一步先不做什么：不覆盖 Android 15 之外未准备的系统版本，不把一次成功标记为长期稳定。
  - 怎么算完成：
    1. enrollment、consent、notification、lease、public TCP 和 egress check 全部有证据。
    2. Wi-Fi/移动网络/断网/Doze/stop sharing 至少有一条可复现回放。
    3. blocked destination、logout/revoke 和 stale epoch 均验证。
  - 怎么验证：
    - `bash scripts/android/package-debug.sh`
    - ADB 设备安装、启动、状态检查和日志回放。
    - Server egress/route API 检查。
  - 对应需求：`requirements.md` 需求 2、需求 3、需求 4
  - 对应设计：`design.md` §2.3.1、§2.3.3、§2.3.4、§7.3、§8

- [ ] 5.3 完成 iOS 前台 egress 验收
  - 状态：TODO
  - 这一步到底做什么：在 iOS simulator/真机上验证 active scene provider 和 background stop/reconnect 链路。
  - 做完你能看到什么：有可复核的前台 egress、path change、scene background 和恢复证据。
  - 先依赖什么：4.4、5.1
  - 开始前先看：
    - `requirements.md` 需求 2、需求 3、需求 5
    - `design.md` §7.3、§8
    - `docs/20261114-移动端网络出口实现边界.md`
  - 主要改哪里：
    - `specs/006.1-Android与iOS网络出口提供者实现/docs/20261114-iOS前台验收记录.md`
    - `apps/ios/Tests/CoreTests/`
    - `apps/ios/Tests/UITests/`
  - 这一步先不做什么：不验证或宣称长期后台 provider，不申请 Network Extension entitlement。
  - 怎么算完成：
    1. active scene 完成 enrollment、consent、lease、public TCP 和 egress check。
    2. background 触发不可用和清理；active 恢复后重新连接。
    3. route epoch 切换和 blocked destination 有证据。
  - 怎么验证：
    - `swift test --package-path apps/ios`
    - iOS simulator/真机回放。
    - Server egress/route API 检查。
  - 对应需求：`requirements.md` 需求 2、需求 3、需求 5
  - 对应设计：`design.md` §2.3.5、§6.2、§6.5、§7.3、§8

- [ ] 5.4 回写 Spec 006 和移动端交付文档
  - 状态：TODO
  - 这一步到底做什么：将 Spec 006 的移动端“仅评估”描述更新为已拆分到 006.1，并记录 Android 前台和 iOS 前台的实际支持边界。
  - 做完你能看到什么：维护者不会把 006.1 的前台验收误解为 Android/iOS 长期后台支持。
  - 先依赖什么：5.2、5.3
  - 开始前先看：
    - `specs/006-Windows与移动端网络出口及电脑操作界面选择/requirements.md`
    - `specs/006-Windows与移动端网络出口及电脑操作界面选择/design.md`
    - `specs/006-Windows与移动端网络出口及电脑操作界面选择/tasks.md`
    - `specs/006.1-Android与iOS网络出口提供者实现/docs/`
  - 主要改哪里：
    - Spec 006 的移动端范围、设计和任务引用
    - `specs/006.1-Android与iOS网络出口提供者实现/docs/`
    - 必要时 `docs/usage/computer.md`
  - 这一步先不做什么：不把 iOS 后台 Network Extension 写成完成，不修改无关产品文档。
  - 怎么算完成：
    1. Spec 006 和 006.1 的边界没有相互矛盾。
    2. 真机证据和遗留风险有日期文件记录。
  - 怎么验证：
    - `rg -n "006\.1|Android|iOS|后台|前台|Network Extension" specs/006-Windows与移动端网络出口及电脑操作界面选择 specs/006.1-Android与iOS网络出口提供者实现`
    - 人工走查文档追踪关系。
  - 对应需求：`requirements.md` 需求 4、需求 5，成功定义
  - 对应设计：`design.md` §7.4、§8

### 最终检查

- [ ] 5.5 006.1 最终验收
  - 状态：TODO
  - 这一步到底做什么：确认共享 engine、Android 前台 Provider、iOS 前台 Provider、服务端联调和验收证据全部闭环。
  - 做完你能看到什么：后续维护者可以依据 Spec 判断当前支持能力和下一步后台能力是否需要独立立项。
  - 先依赖什么：5.1、5.2、5.3、5.4
  - 开始前先看：
    - `requirements.md`
    - `design.md`
    - `tasks.md`
    - `docs/`
  - 主要改哪里：当前 Spec 全部文档和相关测试证据。
  - 这一步先不做什么：不追加新平台、不扩大为系统 VPN、不改变服务端路由协议。
  - 怎么算完成：
    1. 每条需求至少有一个自动化或设备验证证据。
    2. Android/iOS 前台边界、失败行为和后台限制已明确写出。
    3. 所有已完成任务都已回写为 `DONE` 并勾选，未完成项有状态和原因。
  - 怎么验证：
    - `./gradlew :mobile:shared:allTests :mobile:shared:assemble`
    - Android 单元/真机验证。
    - `swift test --package-path apps/ios`
    - Server browser-network integration tests。
    - Spec 需求→设计→任务→证据逐项核对。
  - 对应需求：`requirements.md` 全部需求
  - 对应设计：`design.md` 全文
