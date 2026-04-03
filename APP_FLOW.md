# APP FLOW — Sifer

## User Journey

1. User opens app
2. User grants required permissions:
   - Location (foreground + background)
   - Do Not Disturb (DND access)

3. User sets a zone:
   - Select location (map or current location)
   - Set radius (e.g., 50–200 meters)
   - Save zone

4. App activates geofencing

---

## Runtime Flow

### On Geofence ENTER
1. Receive geofence event
2. Validate zone ID
3. Trigger:
   - Set ringer mode → SILENT
   - Enable DND

### On Geofence EXIT
1. Receive geofence event
2. Validate zone ID
3. Trigger:
   - Set ringer mode → NORMAL
   - Disable DND

---

## Edge Cases

- App killed → geofence still active
- Multiple zones → handle independently
- Permission revoked → disable automation
- GPS off → notify user

---

## States

- IDLE
- MONITORING
- INSIDE_ZONE
- OUTSIDE_ZONE