#!/usr/bin/env python3
"""Run a safe external Bitwarden replay without printing credential values."""
import json
import os
import subprocess
import sys
from pathlib import Path

if len(sys.argv) not in (2, 3):
    raise SystemExit("usage: bitwarden-replay.py HOSTNAME [ITEM_UUID]")
root = Path(__file__).resolve().parents[2]
wrapper = root / "bitwarden" / "skills" / "bitwarden" / "scripts" / "bw.sh"
args = ["search", sys.argv[1]] if len(sys.argv) == 2 else ["password", sys.argv[2]]
result = subprocess.run(["bash", str(wrapper), *args], env=os.environ.copy(), text=True, capture_output=True)
try:
    payload = json.loads(result.stdout)
except json.JSONDecodeError:
    payload = {"ok": False, "errorCode": "invalid_replay_output"}
if payload.get("ok") and "value" in payload:
    payload["value"] = "[REDACTED]"
print(json.dumps(payload, ensure_ascii=True))
raise SystemExit(result.returncode)
