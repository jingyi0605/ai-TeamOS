#!/usr/bin/env bash
# Pinned official release assets: https://github.com/bitwarden/clients/releases/tag/cli-v2026.10.1
set -euo pipefail
version="2026.10.1"
mode="${1:---check}"
if [[ $# -gt 1 || ( "$mode" != --check && "$mode" != --install ) ]]; then
  echo 'Usage: bash ensure-cli.sh [--check|--install]' >&2
  exit 2
fi
case "$(uname -s)/$(uname -m)" in
  Linux/x86_64) platform=linux; checksum=eb09c3715291c21cf744819b873a255c1247b1b8b45bae138621a644ec7ad8f5 ;;
  Linux/aarch64|Linux/arm64) platform=linux-arm64; checksum=79ecb3293fb259660961c949d3e32fdab8682b2bcff0f6764ce38a411ca1ac5d ;;
  Darwin/x86_64) platform=macos; checksum=0f96f40975972dc0ec568e689d21cdb3cb8caa30ec9bb066a55bf254512fb3c7 ;;
  Darwin/arm64) platform=macos-arm64; checksum=4e595f9da80ef8b30abca808d0c78a1560d9589466d1bccd2b3e70f321fb12bf ;;
  *) echo 'Supported hosts: Linux and macOS on x64 or ARM64.' >&2; exit 2 ;;
esac
install_dir="${OUTCOMEGO_BW_CLI_DIR:-$HOME/.local/share/outcomego/bitwarden-cli/$version}"
if [[ -z "${OUTCOMEGO_BW_CLI_DIR:-}" && ! -w "$HOME" && -d /workspace && -w /workspace ]]; then
  install_dir="/workspace/.local/share/outcomego/bitwarden-cli/$version"
fi
[[ "$install_dir" == /* ]] || { echo 'OUTCOMEGO_BW_CLI_DIR must be absolute.' >&2; exit 2; }
executable="$install_dir/bw"
if [[ -e "$executable" ]]; then
  [[ -x "$executable" && "$("$executable" --version 2>/dev/null)" == "$version" ]] || {
    echo 'Unexpected CLI version; inspect the managed installation before replacing it.' >&2; exit 1;
  }
  printf '%s\n' "$executable"
  exit 0
fi
[[ "$mode" == --install ]] || { echo 'Managed Bitwarden CLI is missing; use --install.' >&2; exit 1; }
for dependency in curl unzip python3; do
  command -v "$dependency" >/dev/null || { echo "Install $dependency first." >&2; exit 1; }
done
umask 077
mkdir -p "$install_dir"
temporary="$(mktemp -d "$install_dir/.install.XXXXXX")"
trap 'rm -rf "$temporary"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
url="https://github.com/bitwarden/clients/releases/download/cli-v$version/bw-$platform-$version.zip"
curl --fail --silent --show-error --location --proto '=https' --proto-redir '=https' \
  --connect-timeout 15 --max-time 120 --output "$temporary/bw.zip" "$url"
python3 -I - "$temporary/bw.zip" "$checksum" <<'PY'
import hashlib, sys
with open(sys.argv[1], 'rb') as archive:
    actual = hashlib.file_digest(archive, 'sha256').hexdigest() if hasattr(hashlib, 'file_digest') else hashlib.sha256(archive.read()).hexdigest()
if actual != sys.argv[2]:
    sys.exit('Bitwarden CLI archive checksum mismatch; nothing installed.')
PY
unzip -q "$temporary/bw.zip" bw -d "$temporary"
chmod 700 "$temporary/bw"
[[ "$("$temporary/bw" --version 2>/dev/null)" == "$version" ]] || { echo 'CLI version mismatch; nothing installed.' >&2; exit 1; }
# Stage on the destination filesystem; the computer may mount /tmp noexec.
mv -f "$temporary/bw" "$executable"
printf '%s\n' "$executable"
