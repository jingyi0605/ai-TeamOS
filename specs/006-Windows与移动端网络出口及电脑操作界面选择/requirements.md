# 需求文档 - Windows 与移动端网络出口及电脑操作界面选择

状态：IN_PROGRESS

## 简介

当前浏览器网络出口由服务端统一选择，Desktop 通过 `BrowserNetworkClient` 提供公共 TCP 和 DNS 出口，但 Desktop 运行时只允许 macOS 和 Linux。电脑操作界面只能显示当前出口，用户必须离开电脑界面进入 Server 设置才能调整连接优先级。

本 Spec 为 Windows Desktop 补齐现有网络出口提供者能力，并在电脑操作界面中提供一个安全、可追踪的手动出口选择入口。Android 和 iOS 的工作限定为技术可行性评估，避免在没有移动端后台网络生命周期、凭据 enrollment 和连接保活方案的情况下直接扩大实现范围。

## 术语表

- **System**：OutcomeGo 浏览器网络出口系统。
- **Computer**：运行在 Docker Linux 容器中的 bot computer。
- **Desktop Provider**：已 enrollment、获得本地用户同意并通过网络 lease 的桌面客户端出口提供者。
- **Ranked Source Order**：服务端保存的 `sourceOrder`，按照优先级排列 Desktop Provider 和 `server`。
- **手动选择**：从电脑操作界面选择一个当前可用出口，并将其提升为 `sourceOrder` 第一项，使服务端立即按已有 ranked 机制切换。
- **Route Epoch**：服务端每次出口切换时生成的路由世代标识，用于 fence 旧连接。

## 范围说明

### In Scope

- Windows Desktop 作为 Desktop Provider 建立身份查询、consent、WebSocket lease、公共 TCP 出站和重连能力。
- Windows Desktop 的网络出口开关、状态显示、断网、睡眠、登出和 deployment identity 变化处理。
- 电脑操作界面显示出口列表、当前出口、可用状态和手动选择操作。
- 手动选择复用现有 `sourceOrder`、`revision`、`route epoch` 和 server-side fencing，不新建第二套路由状态。
- Android 与 iOS 作为网络出口提供者的技术可行性评估、限制和后续拆分建议。

### Out of Scope

- Android 或 iOS 网络出口提供者的代码实现。
- Windows 全局 VPN、任意 UDP、ICMP、局域网和私有地址出口。
- 修改现有服务器出口选择算法为负载均衡或按实时测速自动排序。
- 将手动选择扩展为跨账户或跨部署的全局策略。

## 需求

### 需求 1：Windows Desktop 网络出口提供者

**用户故事：** 作为 Windows 用户，我希望允许 bot computer 使用这台 Windows 电脑的公共网络连接，以便沿用现有 Desktop Provider 能力。

#### 验收标准

1. WHEN Desktop 运行在 Windows (`win32`) 且已完成 enrollment THEN System SHALL 将 Windows 标记为支持网络共享，并执行 deployment identity、consent 和 provider WebSocket 流程。
2. WHEN Windows 用户打开网络共享 THEN System SHALL 使用现有 `BrowserNetworkClient`、`outcomego-browser-network.v1`、machine credential 和 connection ID 建立 provider lease。
3. WHEN Computer 通过 Windows Provider 打开目标连接 THEN System SHALL 只允许现有公共 TCP 和 DNS 能力，并继续拒绝本地、私有、非单播和未支持的 UDP 目标。
4. WHEN Windows 网络地址、睡眠、恢复、登出或 deployment identity 发生变化 THEN System SHALL 停止旧 provider、清理旧 lease，并按现有重连与 consent 规则恢复或保持关闭。
5. WHEN Windows Provider 不可用或 lease 过期 THEN System SHALL 由服务端按现有 `sourceOrder` 选择下一个可用来源，并 fence 旧 route epoch。

### 需求 2：电脑操作界面查看网络出口

**用户故事：** 作为正在操作 bot computer 的用户，我希望在电脑操作界面直接看到当前网络出口，以便判断浏览器请求从哪里发出。

#### 验收标准

1. WHEN 电脑操作界面打开 THEN System SHALL 显示当前路由状态、当前出口名称或 Server connection、出口不可用状态和切换中的状态。
2. WHEN 当前出口为 Desktop Provider THEN System SHALL 显示该 Provider 的名称和当前使用状态；WHEN 当前出口为 server THEN System SHALL 显示 Server connection。
3. WHEN route epoch 或服务端 revision 变化 THEN System SHALL 刷新显示并丢弃旧的异步结果，不展示过期的出口名称。

### 需求 3：电脑操作界面手动选择出口

**用户故事：** 作为正在操作 bot computer 的用户，我希望从电脑操作界面手动选择一个出口，以便把后续网络请求切换到指定的 Desktop 或 Server connection。

#### 验收标准

1. WHEN 用户打开网络出口选择器 THEN System SHALL 显示当前 `sourceOrder` 中的来源、名称、可用状态和当前活动来源。
2. WHEN 用户选择一个可用来源并确认 THEN System SHALL 使用当前 `revision` 提交 `mode: ranked` 和重排后的 `sourceOrder`，将选择的来源提升到第一项。
3. WHEN 服务端接受手动选择 THEN System SHALL 立即复用现有 `reconcile(true, true)`、route epoch 和 fencing 行为，不创建 Computer 本地的隐藏 override 状态。
4. WHEN 用户选择不可用来源、revision 冲突或来源在提交前离线 THEN System SHALL 拒绝切换、刷新当前状态并提示用户，不能声称切换成功。
5. WHEN 手动选择会中断现有下载或网络连接 THEN System SHALL 在确认前明确提示用户连接会被中断且请求不会自动重放。
6. WHEN 用户在电脑操作界面手动选择后进入 Server 网络设置 THEN System SHALL 看到同一份已持久化的 `sourceOrder`。

### 需求 4：Android 与 iOS 可行性评估

**用户故事：** 作为项目维护者，我希望知道 Android 和 iOS 是否能作为网络出口，以便决定是否建立后续移动端 Provider Spec。

#### 验收标准

1. WHEN 评估 Android THEN 文档 SHALL 说明现有协议是否可复用、需要哪些 Android 生命周期和网络组件、前后台限制、权限与电量影响，以及可支持的最小产品形态。
2. WHEN 评估 iOS THEN 文档 SHALL 说明现有协议是否可复用、前后台挂起限制、Network Extension 或前台限定方案、权限和发布约束。
3. WHEN 评估完成 THEN 文档 SHALL 明确 Android/iOS 是否进入本 Spec 实现范围，并给出后续 Spec 的边界和验收前提。

## 非功能需求

### 非功能需求 1：性能

1. WHEN 手动选择可用出口 THEN System SHALL 在服务端接受请求后进入 `applying` 或等价过渡状态，不额外引入轮询等待。
2. WHEN Desktop Provider 发送 lease renewal THEN System SHALL 继续使用现有 6 秒 lease、2 秒 renewal 和连接窗口限制。

### 非功能需求 2：可靠性

1. WHEN Windows Provider 断线、睡眠或网络改变 THEN System SHALL 不复用过期的 connection ID、deployment identity 或 route epoch。
2. WHEN 手动切换提交发生 revision 冲突 THEN System SHALL 保留服务端最新顺序并要求用户基于最新状态重试。
3. WHEN Windows 未实现或不支持某类网络能力 THEN System SHALL 复用现有 public-only、TCP-only 和 fail-closed 规则。

### 非功能需求 3：可维护性

1. WHEN 新增支持平台 THEN System SHALL 通过 Desktop Provider 平台支持能力与测试扩展，不复制 server relay 协议。
2. WHEN 新增手动选择入口 THEN System SHALL 通过现有 `BrowserNetworkView` 和 `NetworkSettingsUpdate` 传输状态，不创建第二份出口模型。
3. WHEN 问题排查 THEN System SHALL 能区分平台不支持、未 consent、未 lease、route applying、来源不可用和 revision 冲突。

## 成功定义

- Windows Desktop 能在真实 Windows 环境中 enrollment、获得 consent、建立 provider lease，并被 Computer 用于公共 TCP 出口。
- 电脑操作界面能直接查看并手动提升一个可用出口，服务端和 Server 设置显示同一顺序。
- Android 与 iOS 的可行性、限制和后续工作边界已写入评估文档，未把未验证的移动端能力标记为已支持。
- 需求、设计、任务和验证方式可以逐项追踪。
