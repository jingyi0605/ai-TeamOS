#!/usr/bin/env bash
# Quick start for the host-run debug stack (server + worker + desktop on this Mac).
# The database and the `computer` container run elsewhere and stay up; see .env.
#
#   scripts/dev/debug.sh check      verify mount, database, remote computer, ports
#   scripts/dev/debug.sh up         server + worker in one terminal (Ctrl+C stops both)
#   scripts/dev/debug.sh server     server only (bun --watch, port from .env)
#   scripts/dev/debug.sh worker     worker only
#   scripts/dev/debug.sh desktop    Electron + Vite
#   scripts/dev/debug.sh db-push    sync packages/db schema (stop server and worker first)
#   scripts/dev/debug.sh account    create the owner login (or replace its username/password)
#   scripts/dev/debug.sh mount      mount the shared workspace and agent-data
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
if [[ -n "${OUTCOMEGO_DEBUG_STATE_DIR:-}" ]]; then
  state_dir=$OUTCOMEGO_DEBUG_STATE_DIR
elif [[ -f "$HOME/.outcomego-dev/smb-password" ]]; then
  state_dir=$HOME/.outcomego-dev
else
  # The current shared debug host still uses the pre-rename local state directory.
  state_dir=$HOME/.openteam-dev
fi
mount_point=$state_dir/mnt
remote_host=${OUTCOMEGO_DEBUG_REMOTE_HOST:-10.255.0.45}
remote_user=${OUTCOMEGO_DEBUG_REMOTE_USER:-ubuntu}
share=${OUTCOMEGO_DEBUG_SHARE:-openteam-debug}

die() { echo "error: $*" >&2; exit 1; }

load_env() {
  [[ -f $root/.env ]] || die "$root/.env is missing"
  set -a; . "$root/.env"; set +a
  case "${DATABASE_URL:-}" in *FILL_IN*|"") die "set the database password in DATABASE_URL (.env)" ;; esac
  # The server reports whatever OUTCOMEGO_VERSION holds. Follow the code unless .env pins a value,
  # so the debug stack never drifts from the installed desktop. "+dev" marks it as a source run
  # and is ignored by version-compatibility checks (same convention as scripts/compose.sh).
  if [[ -z "${OUTCOMEGO_VERSION:-}" ]]; then
    local package_version
    package_version=$(sed -n 's/.*"version": *"\([^"]*\)".*/\1/p' "$root/apps/server/package.json" | head -n 1)
    [[ -n $package_version ]] || die "cannot read the version from apps/server/package.json"
    export OUTCOMEGO_VERSION="$package_version+dev"
  fi
}

mount_share() {
  mkdir -p "$mount_point"
  if mount | grep -q " on $mount_point "; then return 0; fi
  [[ -f $state_dir/smb-password ]] || die "missing $state_dir/smb-password"
  mount_smbfs "//$remote_user:$(cat "$state_dir/smb-password")@$remote_host/$share" "$mount_point" \
    || die "could not mount //$remote_host/$share (is this Mac's IP still allowed by Samba?)"
  echo "mounted $mount_point"
}

prepare() { mount_share; load_env; cd "$root"; }

check() {
  local ok=1
  pass() { echo "ok    $*"; }
  fail() { echo "FAIL  $*"; ok=0; }
  load_env
  mount_share >/dev/null 2>&1 && pass "share mounted at $mount_point" || fail "share not mounted"
  [[ -d ${OUTCOMEGO_WORKSPACE_ROOT:-} && -d ${OUTCOMEGO_AGENT_DATA_ROOT:-} ]] \
    && pass "workspace and agent-data directories exist" || fail "workspace/agent-data path missing"

  local health
  health=$(curl --noproxy '*' -s -m 8 "${OUTCOMEGO_COMPUTER_URL%/}/health" \
    -H "authorization: Bearer ${OUTCOMEGO_CONTROL_TOKEN}") || true
  [[ $health == *'"status":"ready"'* ]] && pass "computer ready at $OUTCOMEGO_COMPUTER_URL" \
    || fail "computer not ready at $OUTCOMEGO_COMPUTER_URL"
  [[ $health == *'"authenticated":false'* ]] && echo "note  computer has no model login yet (see outcomego-pi-auth)"

  (cd "$root/packages/db" && bun -e '
    import pg from "pg";
    const c = new pg.Client({ connectionString: process.env.DATABASE_URL, connectionTimeoutMillis: 8000 });
    await c.connect(); await c.query("select 1"); await c.end();' >/dev/null 2>&1) \
    && pass "database reachable" || fail "database unreachable or bad credentials"

  local mac_ip port=${OUTCOMEGO_PORT:-8788} listener
  mac_ip=$(ipconfig getifaddr "$(route -n get "$remote_host" 2>/dev/null | awk '/interface:/{print $2}')" 2>/dev/null || true)
  if [[ -n $mac_ip ]]; then
    local configured
    configured=$(ssh -o BatchMode=yes -o ConnectTimeout=6 "$remote_user@$remote_host" \
      "awk -F= '/^(OUTCOMEGO|AITEAMOS)_DEBUG_SERVER_URL=/{print \$2; exit}' ~/openteam-debug/.env" 2>/dev/null || true)
    if [[ $configured == "http://$mac_ip:$port" ]]; then pass "computer calls back to $configured"
    else fail "this Mac is $mac_ip but the computer calls ${configured:-<unknown>}; update ~/openteam-debug/.env and Samba 'hosts allow', then restart the computer"; fi
  fi
  listener=$(lsof -nP -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | awk 'NR==2{print $1" (pid "$2")"}' || true)
  [[ -z $listener ]] && pass "port $port is free" || echo "note  port $port is in use by $listener"
  return $((1 - ok))
}

kill_tree() {
  local child
  for child in $(pgrep -P "$1" 2>/dev/null || true); do kill_tree "$child"; done
  kill "$1" 2>/dev/null || true
}

up() {
  prepare
  local pids=()
  # `bun --filter` spawns `bun --watch` as a grandchild, so stop the whole tree.
  trap 'for p in "${pids[@]}"; do kill_tree "$p"; done; wait 2>/dev/null || true' EXIT
  trap 'exit 130' INT
  trap 'exit 143' TERM
  bun --filter @outcomego/server dev & pids+=($!)
  bun --filter @outcomego/worker dev & pids+=($!)
  # bash 3.2 (macOS) has no `wait -n`: stop everything as soon as either process exits.
  while kill -0 "${pids[@]}" 2>/dev/null; do sleep 1; done
}

account() {
  prepare
  local user pass
  # "setup" creates the owner, or replaces the username and password of an existing one.
  read -r -p "Owner username [outcomego]: " user; user=${user:-outcomego}
  read -r -s -p "Password (8-128 chars): " pass; echo
  OUTCOMEGO_OWNER_USER=$user OUTCOMEGO_OWNER_PASS=$pass \
    bun -e 'process.stdout.write(JSON.stringify({operation:"setup",username:process.env.OUTCOMEGO_OWNER_USER,password:process.env.OUTCOMEGO_OWNER_PASS}))' \
    | (cd apps/server && bun src/main.ts owner-credentials)
}

case "${1:-}" in
  check)   check ;;
  mount)   mount_share ;;
  up)      up ;;
  server)  prepare; exec bun --filter @outcomego/server dev ;;
  worker)  prepare; exec bun --filter @outcomego/worker dev ;;
  # Some terminal hosts export ELECTRON_RUN_AS_NODE=1, which turns Electron into plain Node and
  # makes `import { BrowserWindow } from "electron"` fail.
  desktop) prepare; unset ELECTRON_RUN_AS_NODE; exec bun --filter @outcomego/desktop dev ;;
  db-push) prepare; exec bun --filter @outcomego/db db:push ;;
  account) account ;;
  *) sed -n '2,12p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//' >&2; exit 2 ;;
esac
