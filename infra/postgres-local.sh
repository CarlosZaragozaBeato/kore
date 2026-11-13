#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
data_dir="$repo_root/data/postgres"
pg_ctl="$repo_root/data/postgres-runtime/usr/bin/pg_ctl"

if [[ ! -x "$pg_ctl" || ! -d "$data_dir" ]]; then
  echo "Local PostgreSQL runtime or data is missing; use infra/docker-compose.yml or install PostgreSQL locally." >&2
  exit 1
fi

case "${1:-}" in
  start)
    "$pg_ctl" -D "$data_dir" -l "$data_dir/server.log" -o '-h 127.0.0.1 -p 55432 -k /tmp' start
    ;;
  stop)
    "$pg_ctl" -D "$data_dir" stop
    ;;
  status)
    "$pg_ctl" -D "$data_dir" status
    ;;
  *)
    echo "Usage: $0 {start|stop|status}" >&2
    exit 2
    ;;
esac
