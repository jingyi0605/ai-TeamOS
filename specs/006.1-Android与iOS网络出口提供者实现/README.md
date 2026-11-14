# Spec 006.1：Android 与 iOS 网络出口提供者实现

状态：IN_PROGRESS

本 Spec 承接 Spec 006 的移动端可行性评估，落实 Android 与 iOS 作为浏览器网络出口 `device` peer 的第一阶段实现。实现目标是让移动端在用户明确开启共享后，通过现有 `outcomego-browser-network.v1` 协议接受服务端转发的公共 TCP 请求，并将请求从移动设备的网络出口发出。

本 Spec 采用 Kotlin Multiplatform 共享协议、复用、租约、复用流和状态机逻辑；Android 与 iOS 分别提供 WebSocket、TCP、网络变化、凭据存储和生命周期适配。Android 第一阶段使用 Foreground Service；iOS 第一阶段限定为应用前台共享。系统级 VPN、任意 UDP/ICMP 和 iOS 长期后台 provider 不在本 Spec 范围内。

文档关系：

- 需求：[`requirements.md`](requirements.md)
- 设计：[`design.md`](design.md)
- 任务：[`tasks.md`](tasks.md)
- 实现边界：[`docs/20261114-移动端网络出口实现边界.md`](docs/20261114-移动端网络出口实现边界.md)
- 上游 Spec：[`../006-Windows与移动端网络出口及电脑操作界面选择/README.md`](../006-Windows与移动端网络出口及电脑操作界面选择/README.md)
