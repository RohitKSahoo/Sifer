# PRODUCT REQUIREMENTS DOCUMENT (PRD) — Sifer

## Product Name
Sifer

---

## Objective

Automatically switch phone to silent + DND mode when entering a predefined location, and revert when exiting.

---

## Problem Statement

Users forget to silence their phones in sensitive environments (e.g., classrooms), causing interruptions.

---

## Solution

A geofencing-based automation app that:
- Detects user location
- Applies silent mode automatically

---

## Target Users

- Students
- Office workers
- Anyone needing context-aware silence

---

## Core Features

### 1. Zone Creation
- Define location + radius

### 2. Automatic Mode Switching
- Enter → Silent + DND
- Exit → Normal mode

### 3. Multiple Zones
- Support multiple saved locations

---

## Non-Functional Requirements

- Low battery usage
- High reliability (background execution)
- Offline functionality

---

## Constraints

- Android permission limitations
- Background execution restrictions

---

## Success Metrics

- Accurate trigger rate (>95%)
- Low battery impact
- Zero manual intervention needed

---

## Future Scope

- AI-based location prediction
- Calendar integration
- Voice-based overrides