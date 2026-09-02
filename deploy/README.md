# deploy/ + db/

K3s manifests for the start-hack backend (`start-hack-ws`). One deployment,
namespace `start-hack`. The `soil-metrics-web` frontend deploys into the same
namespace from its own repo.

Hostnames (all `*.sthomas.ch`, wildcard cert, Traefik edge):

| host | routes to | HTTPRoute |
|---|---|---|
| `soil-metrics-ws.sthomas.ch` | the API — `/v1`, `/public`, Swagger — everything → `start-hack-ws` | `deploy/httproute.yaml` |
| `soil-metrics-public.sthomas.ch` | `/*` → `start-hack-ws` `/public/*` (`addPrefix` Middleware), CORS `*` | `deploy/httproute.yaml` |
| `soil-metrics.sthomas.ch`, `soil-metrics-web.sthomas.ch` | the SPA | frontend repo |

CORS for `/v1/**` and `/public/**` is `*` (`WsMvcConfig`) — the SPA calls
`soil-metrics-ws.sthomas.ch` cross-origin.

## Storage

- **DB** — CNPG Postgres + PostGIS (`db/`). The legacy dump carries **no app
  data** (empty `flyway_schema_history`, only PostGIS reference tables), so a
  fresh Cluster from `db/` is equivalent — see "First bring-up".
- **`/data`** — input reference datasets (~50 MB), a `local-path` PVC
  (`start-hack-data`) seeded once from the legacy VPS. The app only reads it.
- **`/public`** — the derived geojson the frontend fetches. **Not persisted**:
  `WsSchedulingConfig` regenerates it from `/data` into an `emptyDir` a few
  seconds after every pod start. Served via `WsMvcConfig` (`/public/**`).

## Workflows

- `db.yml` (manual only) — provision/reconcile the CNPG Cluster. Never a side
  effect of a release.
- `deploy.yml` — reusable (`workflow_call`) + `workflow_dispatch`.
  `mvn_docker_image.yaml` calls it after it pushes the image for a `v*` tag;
  run it standalone to redeploy any already-built tag.

## First bring-up

```bash
# 1. provision the DB (postInitApplicationSQL creates postgis + fuzzystrmatch)
gh workflow run db.yml -f tag=<tag>

# 2. (optional) replay the legacy DB. The dump has no app rows - only PostGIS
#    reference data + the tiger/topology extensions, which the app doesn't use.
#    Skip unless you want byte parity. If you do restore, ignore the
#    "extension already exists" / "schema already exists" errors:
ssh strato "sudo k3s kubectl -n start-hack exec -i start-hack-db-1 -- \
  pg_restore -d start_hack --no-owner --no-privileges --role=start_hack --no-acl" \
  < ~/Downloads/db_dumps/start-hack.pg17-postgis35.dump || true

# 3. tag + push -> mvn_docker_image.yaml builds and then calls deploy.yml
#    (or redeploy an existing tag:  gh workflow run deploy.yml -f tag=<tag>)

# 4. seed /data into the PVC via the running pod. The container has tar but no
#    gzip, so pipe an uncompressed stream:
ssh strato "sudo tar cf - -C /root/start-hack-backend/data . \
  | sudo k3s kubectl -n start-hack exec -i deploy/start-hack-ws -- tar xf - -C /data"

# 5. restart so WsSchedulingConfig regenerates /public (gpp-ranking-*.geojson)
ssh strato "sudo k3s kubectl -n start-hack rollout restart deploy/start-hack-ws"
```
