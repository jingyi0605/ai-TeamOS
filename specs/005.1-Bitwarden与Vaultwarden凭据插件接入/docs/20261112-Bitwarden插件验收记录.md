# Bitwarden 插件验收记录

日期：2026-11-13。实现分支：`feat/005.1-bitwarden-vaultwarden`。工作树：`openteam-bitwarden`。

已完成：

- `bun run --cwd packages/plugins generate`：16 个插件生成并校验。
- `bun test packages/plugins/test packages/plugin-sdk/test packages/messaging/test/plugin-skill-catalog.test.ts apps/worker/test/plugins.test.ts apps/computer/test/process-secrets.test.ts apps/server/test/plugin/bitwarden-environment.test.ts`：101 tests passed。
- Bitwarden fake CLI 覆盖 package、四平台安装器、URL/参数、登录/解锁/同步、精确匹配、多匹配、username/password/TOTP、错误分类、并发目录和取消清理。
- server unit fixture 覆盖 master password 空白保留、字段级 URL 校验、1Password token trim、runtime environment 冲突。
- `bun run --cwd packages/plugin-sdk typecheck`、`bun run --cwd packages/plugins typecheck`、`bun run --cwd apps/server typecheck`：通过。

待执行：

- Disposable PostgreSQL integration test：当前未设置 `OUTCOMEGO_TEST_DATABASE_URL`，状态 SKIPPED。
- 官方 CLI release asset 实际下载：本机 TLS 建连失败，状态 PENDING。
- 真实 Vaultwarden、远程 computer 网络、浏览器测试站点：没有授权启动服务或重建远程 computer，状态 PENDING。

记录中不写入真实 API Key、主密码、session key、密码或 TOTP。
