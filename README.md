# 🛡️ Sifer (v4.0.2)

Sifer is an Android app that automatically changes your phone’s sound settings based on where you are.

You can create location-based zones called **Havens**, and Sifer will:
- Silence your phone
- Enable vibrate
- Mute media
- Turn on DND

All of this happens automatically when you enter or leave those places.

### 🎓 **For when your phone forgets it's in class and your reels become the main lecture.**

---

## 📱 Screenshots

<table align="center">
  <tr>
    <td align="center">
      <img src="assets/Homepage.png" width="250"/><br/>
      <b>Home Screen</b>
    </td>
    <td width="20"></td>
    <td align="center">
      <img src="assets/Havenpage.png" width="250"/><br/>
      <b>Add Haven</b>
    </td>
    <td width="20"></td>
    <td align="center">
      <img src="assets/Settingpage.png" width="250"/><br/>
      <b>Settings</b>
    </td>
  </tr>
</table>

---

## ⭐ Features

- 📍 **Havens (Location Zones)**  
  Create zones on the map where rules apply automatically.

- 🔇 **Auto Sound Control**  
  Control:
  - Ringer
  - Vibrate
  - Media volume
  - Do Not Disturb

- ⚡ **Instant Activation**  
  Works immediately when you enter a location.

- 🔄 **Auto Restore**  
  Restores normal sound settings when you leave.

- 🔋 **Battery Efficient**  
  Uses Android geofencing for low battery usage.

- 🔒 **Privacy Friendly**  
  Uses OpenStreetMap (no Google tracking for maps).

- 📊 **Status Dashboard**  
  Shows if permissions are enabled properly.

- 📝 **Activity Logs**  
  Keeps track of entries and exits from Havens.

---

## 🛠️ Tech Stack

- Kotlin
- Jetpack Compose
- Room Database
- Coroutines & Flow

### APIs Used
- Google Geofencing API
- OSMdroid (OpenStreetMap)
- Android System APIs

---

## 🏗️ Architecture

- MVVM (Model-View-ViewModel)
- Event-driven (handles system events like boot and location changes)

---

## 🚀 Getting Started

### Requirements

- Android 8.0 (API 26) or higher
- Location permission (Always Allow recommended)
- Do Not Disturb access

---

### Installation

👉 [Download APK](https://github.com/RohitKSahoo/sifer/releases)

---

## 👤 Author

Rohit K Sahoo  
GitHub: https://github.com/RohitKSahoo

---

## 📄 License

This project is licensed under the MIT License.

---

## 🧾 Summary

Sifer helps your phone automatically adjust its sound settings based on your location, so you don’t have to do it manually.
