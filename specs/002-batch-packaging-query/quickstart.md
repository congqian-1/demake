# Quickstart: 批次与包装层级查询

## Prerequisites

- `mes-service1` running locally (default port 8080 or your configured port).
- Target part data exists in database.

## Batch Hierarchy Query (via Part Code)

```bash
curl -s "http://localhost:8080/api/v1/production/part/PART-001/work-order-and-batch"
```

Expected response shape:

```json
{
  "code": "0",
  "message": "OK",
  "data": {
    "batch": { "batchNum": "BATCH-001", "batchType": 1, "productTime": "2026-02-01T00:00:00" },
    "optimizingFiles": []
  }
}
```

## Prepackage Hierarchy Query (via Part Code)

```bash
curl -s "http://localhost:8080/api/v1/production/part/PART-001/package"
```

## Error Handling

- If the part code does not exist, returns HTTP 404 with `{ code, message }`.
- Missing child levels are returned as empty arrays `[]` (never omitted).
