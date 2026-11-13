# Spec 005.1：Bitwarden 与 Vaultwarden 凭据插件接入

规格编号：`005.1`

状态：实施中

日期：2026 年 11 月 12 日。

父规格：[Spec 005：插件扩展体系](../005-插件扩展体系/README.md)

本 Spec 为 OpenTeam 增加 Bitwarden 凭据插件，并以 Bitwarden CLI 兼容方式支持本地部署的 Vaultwarden。第一版沿用当前 1Password 的 Skills 运行模型，让 Bot 和 `computerUse` 子机器人能够在需要登录网站时查找用户名、密码和一次性验证码。

文档导航：

- [需求文档](requirements.md)
- [设计文档](design.md)
- [任务清单](tasks.md)
- [补充文档](docs/README.md)

## 决策

第一版采用“Skills + 受限 CLI 包装脚本”的实现，不新增 MCP connector，不把 Vault 内容复制到 OpenTeam 数据库，也不实现写入 Vault 的能力。

选择这个边界有三个原因：

1. 当前 1Password 已经证明 Skills、Shell、插件环境变量和 `computerUse` 可以组成完整的登录链路。
2. `computerUse` 子机器人不会直接继承父 Bot 的 connector 工具，但可以接收同一插件 Skill 并使用 Bot computer 上的 Shell，因此 Skills 是保持浏览器登录能力一致的最短路径。
3. Vaultwarden 没有与 1Password Service Account 完全等价的只读服务账户模型。第一版必须把只读边界放在插件命令接口、专用 Vaultwarden 用户和 Collection 权限上，并明确这不是主机级强制沙箱。

为支持 Vaultwarden 的服务器地址和登录邮箱等运行参数，本 Spec 同时扩展插件字段能力：带 `environment` 的字段可以是普通配置字段，也可以是 secret 字段。Bitwarden 五个字段标记为 `protected`，普通 Shell 不会获得它们，只有服务端授权 lease 的 Computer 私有 wrapper 路径可以使用；字段仍不进入公开配置视图。

## 可实施边界

本阶段实现以下完整主链路：

1. Marketplace 可以发现、安装、配置和卸载 `bitwarden` 插件。
2. 用户可以配置 Bitwarden Cloud 或任意可访问的 Vaultwarden server URL。
3. 插件使用固定版本且可校验完整性的 Bitwarden CLI。
4. 插件使用 API Key 登录并用主密码解锁 Vault。
5. Bot 可以按站点域名、登录项标题或显式 item id 查找登录项。
6. Bot 可以读取 username、password 和 TOTP，并把值用于用户请求的登录任务。
7. `computerUse` 可以复用 Skill 完成网页登录和验证码填充。
8. 插件的可用命令只提供读取操作，不提供创建、更新、删除、移动或共享 Vault 项目操作。
9. 插件可以报告配置缺失、网络不可达、TLS、登录、解锁、同步、匹配和字段读取错误。
10. 相关测试、文档和 Vaultwarden 兼容性验收脚本齐全。

## 明确不做

- 不实现 Vault 项目创建、更新、删除、移动、共享、组织成员管理或权限修改。
- 不主动创建凭据日志；CLI 必需的 token 和加密 Vault 使用短期私有目录并清理。direct read 会进入现有会话/终端历史，与 1Password 一致。
- 不引入新的 Vault 数据库、缓存表或 OpenTeam 内部凭据模型。
- 不承诺对 Vaultwarden 所有历史版本和所有未来 Bitwarden CLI 版本自动兼容。
- 不绕过 TLS 证书校验，不支持通过关闭证书校验解决内网证书问题。
- 不在第一版实现多个 Vaultwarden 账号或多个 server 的独立插件账户路由；一个已安装的 `bitwarden` Skills 插件对应一组运行配置。
- 不把 Bitwarden CLI 的任意命令暴露为 Bot 的通用 Shell 能力。

复核结论与实施基线见 [20261113-规格复核与覆盖矩阵](docs/20261113-规格复核与覆盖矩阵.md) 和 [设计 §8.2](design.md#82-已确定基线与外部验收)。
