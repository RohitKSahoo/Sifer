# BACKEND SCHEMA — Sifer

## Room Database Schema

### Table: `zones`

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | INTEGER (PK) | Auto-incrementing ID |
| `name` | TEXT | Name of the zone |
| `latitude` | REAL | Latitude of the center |
| `longitude` | REAL | Longitude of the center |
| `radius` | REAL | Radius in meters |
| `isEnabled` | INTEGER | Boolean (0/1) to enable/disable the zone |

---

## Data Access Objects (DAO)

### `ZoneDao`
- `getAllZones()`: Returns all zones as Flow.
- `insertZone(zone)`: Adds a new zone.
- `deleteZone(zone)`: Removes a zone.
- `updateZone(zone)`: Updates zone details.
