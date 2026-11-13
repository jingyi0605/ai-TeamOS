import { expect, test } from "bun:test";
import { chmod, mkdir, mkdtemp, readFile, readdir, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

const scripts = new URL("../bitwarden/skills/bitwarden/scripts/", import.meta.url).pathname;
async function fixture(run: (root: string, target: string) => Promise<void>) {
  const root = await mkdtemp(join(tmpdir(), "bitwarden-installer-"));
  try { await run(root, join(root, "managed CLI")); }
  finally { await rm(root, { recursive: true, force: true }); }
}
async function executable(path: string, body: string) {
  await writeFile(path, `#!/usr/bin/env bash\n${body}\n`);
  await chmod(path, 0o700);
}
async function execute(target: string, mode: string, env: Record<string, string> = {}) {
  const child = Bun.spawn(["bash", join(scripts, "ensure-cli.sh"), mode], {
    env: { ...process.env, OUTCOMEGO_BW_CLI_DIR: target, ...env }, stdout: "pipe", stderr: "pipe",
  });
  const [code, stdout, stderr] = await Promise.all([child.exited, new Response(child.stdout).text(), new Response(child.stderr).text()]);
  return { code, stdout, stderr };
}
test("Bitwarden CLI --check never downloads, reuses the pinned cache and refuses version replacement", async () => {
  await fixture(async (_root, target) => {
    expect((await execute(target, "--check")).code).toBe(1);
    expect(await Bun.file(join(target, "bw")).exists()).toBe(false);
    await mkdir(target);
    await executable(join(target, "bw"), "echo 2026.10.1");
    expect(await execute(target, "--check")).toEqual({ code: 0, stdout: `${join(target, "bw")}\n`, stderr: "" });
    await executable(join(target, "bw"), "echo unsupported-version");
    const failure = await execute(target, "--install");
    expect(failure.code).toBe(1);
    expect(failure.stderr).toContain("Unexpected CLI version");
    expect(await readFile(join(target, "bw"), "utf8")).toContain("unsupported-version");
  });
});
test("Bitwarden installer rejects unsupported platforms and relative cache paths", async () => {
  await fixture(async (root, target) => {
    await mkdir(join(root, "bin"));
    await executable(join(root, "bin/uname"), "echo unsupported");
    const failure = await execute(target, "--install", { PATH: `${root}/bin:${process.env.PATH}` });
    expect(failure.code).toBe(2);
    expect(failure.stderr).toContain("Supported hosts");
    expect((await execute("relative", "--install")).code).toBe(2);
  });
});
test("a corrupted CLI archive is never executed or installed and staging is cleaned", async () => {
  await fixture(async (root, target) => {
    const mocks = join(root, "bin");
    await mkdir(mocks);
    await executable(join(mocks, "uname"), 'if [[ "$1" == -s ]]; then echo Linux; else echo x86_64; fi');
    await executable(join(mocks, "curl"), 'while [[ $# -gt 0 ]]; do if [[ "$1" == --output ]]; then echo corrupted > "$2"; exit 0; fi; shift; done; exit 2');
    await executable(join(mocks, "unzip"), `touch '${root}/unzip-ran'; exit 1`);
    const failure = await execute(target, "--install", { PATH: `${mocks}:${process.env.PATH}` });
    expect(failure.code).toBe(1);
    expect(failure.stderr).toContain("checksum mismatch");
    expect(failure.stdout).toBe("");
    expect(await readdir(target)).toEqual([]);
    expect(await Bun.file(join(root, "unzip-ran")).exists()).toBe(false);
  });
});
test("the wrapper's bounded subprocess runner times out and reaps its child", async () => {
  const code = `
import importlib.util, os, sys
spec = importlib.util.spec_from_file_location('bw_wrapper', sys.argv[1])
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
runner = module.Runner()
try:
    runner.run([sys.executable, '-c', 'import time; time.sleep(60)'], os.environ.copy(), 'sync', timeout=0.05)
    raise AssertionError('timeout did not stop the command')
except module.Failure as error:
    assert error.result['detail']['reason'] == 'timeout'
    assert runner.child is None
`;
  const child = Bun.spawn(["python3", "-I", "-c", code, join(scripts, "bw.py")], { stdout: "pipe", stderr: "pipe" });
  expect(await child.exited).toBe(0);
  expect(await new Response(child.stderr).text()).toBe("");
});
