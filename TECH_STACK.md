# TECH STACK — Sifer

## Platform
- Android (Kotlin)

---

## Core APIs

### Location
- Google Play Services Geofencing API

### System Controls
- AudioManager (ringer mode)
- NotificationManager (DND)

---

## Architecture
- MVVM (recommended)
- Clean separation of concerns

---

## Storage
- Room Database (preferred)
  OR
- SharedPreferences (MVP)

---

## Background Processing
- BroadcastReceiver (geofence events)

---

## Optional
- Foreground Service (for reliability)
- WorkManager (for retries)

---

## Dependencies

- play-services-location
- androidx.lifecycle
- room (optional)