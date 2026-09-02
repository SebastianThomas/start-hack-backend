# deploy/ + db/

K3s manifests for the start-hack backend (`start-hack-ws`). One deployment,
namespace `start-hack`. The `soil-metrics-web` frontend deploys into the same
namespace from its own repo.

| host | routes to | owned by |
|---|---|---|
| `soil-metrics-web.sthomas.ch` | `/` → SPA, `/v1` + `/public` → this backend (same origin, no CORS) | frontend repo's HTTPRoute |
| `soil-metrics-public.sthomas.ch` | `/*` → this backend `/public/*` (URL-rewritten, CORS `*`) | `deploy/httproute.yaml` here |

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

- `db.yml` (manual) — provision/reconcile the CNPG Cluster.
- `deploy.yml` — auto-deploys after `mvn_docker_image.yaml` builds a `v*` tag
  reachable from `main`; manual dispatch otherwise.

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

# 3. deploy (auto after the image build, or: gh workflow run deploy.yml -f tag=<tag>)

# 4. seed /data into the PVC via the running pod
ssh strato "cd /root/start-hack-backend/data && sudo tar czf - ." \
  | ssh strato "sudo k3s kubectl -n start-hack exec -i deploy/start-hack-ws -- tar xzf - -C /data"

# 5. restart so WsSchedulingConfig regenerates /public
ssh strato "sudo k3s kubectl -n start-hack rollout restart deploy/start-hack-ws"
```
