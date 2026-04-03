# FRONTEND GUIDELINES — Sifer

## Design Principles

- Minimal UI
- Fast interaction
- No clutter

---

## Screens

### 1. Home Screen
- List of zones
- Toggle ON/OFF
- Add new zone button

### 2. Add Zone Screen
- Map view
- "Use Current Location" button
- Radius slider (50m–500m)
- Save button

---

## UX Rules

- Always show permission status
- Show current state:
  - "Inside Zone (Silent Active)"
  - "Outside Zone (Normal Mode)"

- Provide feedback:
  - Toast/snackbar on mode change

---

## UI Components

- Google Maps view
- Slider (radius)
- Toggle switches
- Cards for zones

---

## Error Handling

- If permissions missing → block feature
- If GPS off → show alert