#!/bin/sh
# With LITESTREAM_REPLICA_URL set, the database is restored from the replica
# on a fresh volume, then the app runs under Litestream supervision (every
# WAL segment streamed to object storage). Without it, plain start - dev or
# environments where continuous backup isn't wanted.
set -e

if [ -n "${LITESTREAM_REPLICA_URL}" ]; then
  litestream restore -if-db-not-exists -if-replica-exists \
    -o "${DATABASE_PATH}" "${LITESTREAM_REPLICA_URL}"
  exec litestream replicate -exec "bin/app start" \
    "${DATABASE_PATH}" "${LITESTREAM_REPLICA_URL}"
else
  exec bin/app start
fi
