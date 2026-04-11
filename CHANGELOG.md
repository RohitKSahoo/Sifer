# CHANGELOG — Sifer

## 2026-04-11

### Iteration 8
#### Modified
- **Rule Simplification:** Removed the redundant "Silence" toggle and consolidated its behavior into the "DND" toggle.
- **Ghost Silence Implementation:** The DND toggle now uses volume-based silencing (setting Ring/Notification to 0 while keeping phone in Normal mode) to prevent system-level Vibrate/DND conflicts.
- **Forced Protocol Restoration:** Disabling any automation rule now forces the device into Ring Mode with 50% Ringer and Media volume for a guaranteed consistent state.
- **Mutual Exclusion:** Programmed the UI to prevent DND and Vibrate from being active at the same time.

### Iteration 7
#### Fixed
- **Volume-Based Silence:** Switched from system ringer modes to granular stream volume control to avoid automatic DND triggering.
- **System State Recovery:** Implemented a forced restoration to Ring mode when rules are deactivated.

### Iteration 6
#### Fixed
- **Atomic State Management:** Ensured original phone state is captured only once upon entry.
- **Correct DND Filter:** Switched to PRIORITY filter for standard behavior.

### Iteration 5
#### Fixed
- **Rule Independence:** Decoupled Silence from DND.
- **Real-Time State Restoration:** Fixed issue where toggling a rule OFF didn't restore previous state.
- **Notification Panel:** Improved visibility and priority.

### Iteration 4
#### Added
- **Status Notification:** Persistent protection notification.
- **Modular Rules:** DND, Vibrate, Silence, and Media 0 controls.

### Iteration 3
#### Fixed
- **Settings Functionality:** Live system setting links.
- **Dark Mode Fix:** Forced black text for high-contrast legibility.

### Iteration 2
#### Fixed
- **Map UX/Performance:** Disabled horizontal swipe, increased thread count, and implemented instant snapping.

### Iteration 1
#### Fixed
- **Map Foundation:** Single-Host AndroidView model fix for blank map tiles.
