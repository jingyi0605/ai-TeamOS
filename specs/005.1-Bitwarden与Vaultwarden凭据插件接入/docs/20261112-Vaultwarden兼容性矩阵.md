# Vaultwarden 兼容性矩阵

复核日期：2026-11-13。状态：PENDING，等待隔离 Vaultwarden 实例和 Bot computer 网络环境。

| 维度 | 目标基线 | 当前结果 | 证据/下一步 |
| --- | --- | --- | --- |
| Bitwarden CLI | 2026.10.1 | PENDING | 官方 Linux/macOS x64/ARM64 release 已固定；本机 release asset 下载在 TLS 建连阶段失败 |
| Vaultwarden | 待指定测试版本 | PENDING | 启动隔离实例后记录版本和日期 |
| Bot computer | 远程容器的 DNS、路由和 CA | PENDING | 从 computer 运行 `status/search`，不以桌面可达代替 |
| HTTPS root origin | HTTPS，无 userinfo/path/query/fragment | PASS（fixture） | SDK URL 校验与 wrapper fixture |
| 反向代理 HTTPS | 独立 origin，无路径前缀 | PENDING | 真实代理回放 |
| API Key 登录 | personal client ID/secret | PASS（fake CLI） | `bitwarden.test.ts`；真实 provider 待回放 |
| 主密码解锁 | `BW_MASTER_PASSWORD` 原样传递 | PASS（fake CLI） | whitespace fixture；真实 provider 待回放 |
| 同步 | 每次读取前同步，失败不返回值 | PASS（fake CLI） | sync failure fixture |
| exact hostname/完整 URI | 多候选只返回摘要 | PASS（fake CLI） | selector fixture |
| username/password/TOTP | 三字段独立读取 | PASS（fake CLI） | field fixture |
| TLS/DNS/timeout | 稳定安全错误类别 | PASS（fake CLI） | error classification fixture |
| 真实网页登录 | `computerUse` secret fill | PENDING | 需要浏览器和测试站点回放 |

PENDING 项不能标为 PASS。运行时使用 [Bitwarden replay fixture](../../../packages/plugins/test/fixtures/bitwarden-replay.py)，环境变量从外部注入，脚本会把返回值替换为 `[REDACTED]`。
