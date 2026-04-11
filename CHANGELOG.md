# CHANGELOG — Sifer

## 2026-04-11

### Iteration 11
#### Modified
- **Header Refinement:** Centered the "SIFER" branding and removed redundant menu and profile icons for a cleaner, focused experience.
- **Settings Footer:** Replaced technical mockup text with a functional GitHub link to the developer's profile (RohitKSahoo).
- **Navigation UI:** Fully transitioned to icon-only navigation with increased touch targets.
#### Fixed
- **Input Contrast:** Guaranteed black text visibility in the "Haven Name" and "Search" fields on the map page.

### Iteration 10
#### Added
- **Paper Grid UI:** Implemented a persistent square grid background across all pages.
#### Fixed
- **Navigation UI:** Removed footer labels and increased icon sizes.
#### Modified
- **Rule Layout:** Compressed automation tiles into a single row.

### Iteration 9
#### Modified
- **Quick Tile UI:** Replaced switches with interactive tile cards that highlight in green when active.
- **Native Silent Mode:** Updated DND to use `RINGER_MODE_SILENT`.

### Iteration 8
#### Modified
- **Rule Consolidation:** Consolidated Silence into DND using volume-based silencing.
- **Forced Restoration:** Disabling rules now forces Ring Mode + 50% volume.

### Iteration 7
#### Fixed
- **Volume-Based Silence:** Switched to granular stream control.

### Iteration 6
#### Fixed
- **Atomic State Management:** Prevented original state overwrites.
- **Correct DND Filter:** Switched to PRIORITY filter.

### Iteration 5
#### Fixed
- **Rule Independence:** Decoupled Silence and DND.
- **Real-Time Restore:** Instant restoration of system state.

### Iteration 4
#### Added
- **Status Notification:** Persistent protection notification.

### Iteration 3
#### Fixed
- **Dark Mode Fix:** Forced black text for high-contrast legibility.

### Iteration 2
#### Fixed
- **Map Interaction:** Disabled horizontal swipe.
- **Map Performance:** Increased thread count.

### Iteration 1
#### Fixed
- **Map Foundation:** Single-Host AndroidView model fix.
