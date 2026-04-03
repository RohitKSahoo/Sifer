# SYSTEM DESIGN — Sifer

## Architecture Overview

[ UI Layer ]
      ↓
[ Controller Layer ]
      ↓
[ Geofence Manager ] ←→ [ Android Location Services ]
      ↓
[ Event Receiver (BroadcastReceiver) ]
      ↓
[ Action Engine ]
      ↓
[ System Services (AudioManager, NotificationManager) ]

---

## Components

### 1. UI Layer
- Zone creation
- Radius selection
- Toggle automation

### 2. Geofence Manager
- Register geofences
- Remove/update geofences

### 3. BroadcastReceiver
- Listens for ENTER/EXIT transitions

### 4. Action Engine
- Applies system changes:
  - Silent mode
  - DND mode

### 5. Persistence Layer
- Stores zones locally

---

## Data Flow

User Input → Save Zone → Register Geofence  
Geofence Trigger → Receiver → Action Engine → System Change

---

## Fault Tolerance

- Retry geofence registration on failure
- Validate permissions before execution
- Fail-safe: revert to NORMAL mode if error