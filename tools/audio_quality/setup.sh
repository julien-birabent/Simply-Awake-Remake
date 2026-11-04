#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

command -v ffmpeg >/dev/null || brew install ffmpeg

python3 -m venv .venv
.venv/bin/pip install -q -r requirements.txt
if [[ "${1:-}" == "--firestore" || "${1:-}" == "--all" ]]; then
  .venv/bin/pip install -q -r requirements-firestore.txt
fi

if [[ "${1:-}" == "--balance" || "${1:-}" == "--all" ]]; then
  PY=python3
  if ! python3 -c 'import sys; sys.exit(0 if sys.version_info < (3, 14) else 1)'; then
    command -v python3.12 >/dev/null || brew install python@3.12
    PY=python3.12
  fi
  "$PY" -m venv .venv-balance
  .venv-balance/bin/pip install -q -r requirements.txt -r requirements-balance.txt
  echo "demucs: $(.venv-balance/bin/demucs --help | head -1)"
fi
echo "ok: .venv/bin/python analyze.py --help"
