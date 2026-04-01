# BharatScan

<p align="center">
  <img width="562" height="70" alt="BharatScan" src="https://github.com/user-attachments/assets/9f121c4e-72ae-4e87-8e55-c7d391a5bbe7" />
</p>

<p align="center">
  <img width="720" alt="madeindia" src="https://github.com/user-attachments/assets/07f24fdd-fb4f-48e0-8951-8be6293bd40d" />
</p>

<p align="center">
  <img alt="App" src="https://img.shields.io/badge/App-BharatScan-0B3D91?style=for-the-badge" />
  <img alt="Android" src="https://img.shields.io/badge/Android-26%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-2026.03.00-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img alt="Gradle" src="https://img.shields.io/badge/Gradle-8.13.2-02303A?style=for-the-badge&logo=gradle&logoColor=white" />
  <img alt="OpenCV" src="https://img.shields.io/badge/OpenCV-4.12.0-5C3EE8?style=for-the-badge&logo=opencv&logoColor=white" />
  <img alt="ML Kit" src="https://img.shields.io/badge/ML%20Kit-Text%20Recognition-34A853?style=for-the-badge&logo=google&logoColor=white" />
  <img alt="PDF" src="https://img.shields.io/badge/PDF-PDFBox%20Android-EE3E3E?style=for-the-badge" />
  <img alt="License" src="https://img.shields.io/badge/License-GPL--3.0--or--later-9C27B0?style=for-the-badge" />
  <a href="https://github.com/ahansardar/BharatScan/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/ahansardar/BharatScan/actions/workflows/ci.yml/badge.svg?branch=main" /></a>
</p>

<p align="center">
  <a href="#features">Features</a> | <a href="#screenshots">Screenshots</a> | <a href="#quick-start">Quick Start</a> | <a href="#tech-stack">Tech Stack</a> | <a href="#license">License</a>
</p>

BharatScan is a Made in India Android document scanning app built with Jetpack Compose, CameraX, and on-device ML for segmentation and OCR. This repo includes the Android app plus supporting JVM modules for image processing and evaluation.

## Version Updates
### v1.1.0 (Current)
- Tricolor Modern UI refresh with improved visual separation and India-inspired styling.
- Background image support for Home, Documents, and Settings with tint overlays.
- OCR Search Index: search inside scanned text across documents.
- Guided walkthrough across Home → Scan → Edit → Export → Search.
- Update flow polish with GitHub Releases checks, progress UI, and formatted notes.
- Scanner and crop refinements (retake per page, manual crop fallback).

## Features
- Camera-based document capture and scanning workflow.
- Automatic document boundary detection and auto-cropping via an on-device segmentation model.
- PDF export with optional OCR text layer (ML Kit Text Recognition).
- Built-in PDF viewer with zoom and search highlights.
- PDF password handling and optional security gate via device biometrics.
- Export formats and quality controls (see settings and export screens in the app).
- External intent support for `org.bharatscan.app.action.SCAN_TO_PDF` and PDF `VIEW/EDIT` intents.

## Screenshots
<table>
  <tr>
    <td><b>Home</b><br><img width="240" alt="Home" src="https://github.com/user-attachments/assets/7a3d56dc-05c1-49d9-8fd7-7c3e60c5e30c" /></td>
    <td><b>Document</b><br><img width="240" alt="Document" src="https://github.com/user-attachments/assets/69188702-6476-496b-aef9-ad413b35507c" /></td>
    <td><b>Camera</b><br><img width="240" alt="Camera" src="https://github.com/user-attachments/assets/3d0d4437-e5c3-4059-8f54-664bf708c015" /></td>
  </tr>
  <tr>
    <td><b>Export</b><br><img width="240" alt="Export" src="https://github.com/user-attachments/assets/9370243a-0e64-4e0e-b199-7c0172d7b5ef" /></td>
    <td><b>Success</b><br><img width="240" alt="Success" src="https://github.com/user-attachments/assets/1ff35846-f014-402c-95c1-bb41914ba1d7" /></td>
    <td><b>PDF Viewer</b><br><img width="240" alt="PDF Viewer" src="https://github.com/user-attachments/assets/1ba7e027-b8bf-4616-8b26-fea7aab166a0" /></td>
  </tr>
  <tr>
    <td><b>Settings</b><br><img width="240" alt="Settings" src="https://github.com/user-attachments/assets/f2a799de-9f76-4915-8ede-0bc7f89e7532" /></td>
    <td><b>Settings Detail</b><br><img width="240" alt="Settings Detail" src="https://github.com/user-attachments/assets/1afc1070-8681-444b-8f8e-ddd5332b62f3" /></td>
    <td></td>
  </tr>
</table>

## Tech Stack
- Kotlin 2.3.10 and Java 11 bytecode.
- Android Gradle Plugin 8.13.2.
- Jetpack Compose (BOM `2026.03.00`).
- CameraX, ML Kit Text Recognition, PDFBox-Android.
- LiteRT (TFLite runtime) for on-device segmentation.
- OpenCV (JVM bindings) in `imageprocessing`.

## Project Structure
- `app` Android application module (Compose UI, CameraX, PDF, OCR, settings, export).
- `imageprocessing` JVM library with OpenCV-backed image utilities and transforms.
- `evaluation` JVM module for evaluation workflows built on `imageprocessing`.
- `gradle` Version catalog and Gradle plugin configuration.
- `metadata` Play/metadata assets (if used for releases).

## Requirements
- JDK 11.
- Android Studio (recommended) or command-line Gradle.
- Android SDK with API 36 installed.
- Network access during first build to download the segmentation model.

## Quick Start
The project uses the Gradle wrapper.

```bash
# Debug build
./gradlew :app:assembleDebug

# Install to a connected device
./gradlew :app:installDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

## Model Download
The segmentation model is downloaded during `preBuild`.

- Task: `downloadTFLiteModel` in `app/download-tflite.gradle.kts`.
- Model name: `fairscan-segmentation-model.tflite`.
- Download location: `app/build/downloads/` and copied into generated assets.

If you are building offline, run once with network access or pre-seed the model file into `app/build/downloads/`.

## Build Configuration
- `minSdk`: 26
- `targetSdk`: 36
- `compileSdk`: 36
- ABI splits are enabled for APKs. Output names follow `BharatScan-<versionName>-<abi>.apk`.

## Signing
Release signing is enabled when these Gradle properties are present:
- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Without these, the release build type will still be configured but unsigned.

## Permissions
The app uses:
- `android.permission.CAMERA`
- `android.permission.WRITE_EXTERNAL_STORAGE` (Android 9 and below, for saving files)

## Intents
MainActivity handles:
- Launcher entry point.
- Custom action: `org.bharatscan.app.action.SCAN_TO_PDF`.
- PDF view/edit intents for `application/pdf` with `file` and `content` schemes.

## Testing
Unit and instrumentation tests are configured in the `app` module.

```bash
./gradlew :app:test
./gradlew :app:connectedAndroidTest
```

## Contributing
Issues and PRs are welcome. Please keep changes small and focused, and include test updates where appropriate. See `CONTRIBUTING.md` for details.

## License
This project is licensed under the GNU General Public License, version 3 or later (GPL-3.0-or-later).
