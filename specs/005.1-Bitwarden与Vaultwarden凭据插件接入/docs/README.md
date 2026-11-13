# Spec 005.1 补充文档

本目录保存 Bitwarden/Vaultwarden 插件实现期间的兼容性、联调和验收记录。

建议补充以下文档：

- `20261113-规格复核与覆盖矩阵.md`：记录需求覆盖、代码证据、默认决策和安全边界。

- `20261112-Vaultwarden兼容性矩阵.md`：记录固定 Bitwarden CLI、Vaultwarden 版本、Bot computer 平台、网络、TLS 和读取结果。
- `20261112-Bitwarden插件验收记录.md`：记录插件安装、字段配置、Bot direct read、`computerUse` 登录、错误路径和现有 1Password 回归结果。
- `20261112-Bitwarden运行故障排查.md`：记录 server URL、DNS、证书、API Key、主密码、同步和 item 匹配的排查方式。

补充文档必须使用中文内容和带日期的中文文件名，不得写入真实 server 凭据、API Key、主密码、session key、完整密码或 TOTP。
