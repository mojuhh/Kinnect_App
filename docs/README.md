# Kinnect Application Repository

This repository contains the complete Kinnect Android mobile application and PHP backend service.

## Repository Structure
```
kinnect/
├── android/               # Android Studio project source code
│   ├── app/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── ...
├── backend/               # PHP REST API service (Deploy to htdocs/kinnect)
│   ├── auth/              # Login and Registration APIs
│   ├── users/             # User profile endpoints
│   ├── friends/           # Friends management endpoints
│   ├── posts/             # Posts & feed endpoints
│   ├── stories/           # 24h stories management endpoints
│   ├── chat/              # Direct messaging & chat endpoints
│   ├── calls/             # Agora Voice & Video call signaling endpoints
│   ├── notifications/     # Notifications endpoints
│   ├── uploads/           # Runtime media uploads (gitignored)
│   ├── common.php         # Common helper functions and DB connector
│   ├── config.sample.php  # Database configuration template
│   ├── config.php         # Local database configuration (gitignored)
│   └── schema.sql         # MySQL database creation script
└── docs/                  # Documentation
```

## Setup Instructions

### Backend (PHP / MySQL)
1. Import `backend/schema.sql` into MySQL.
2. Copy `backend/config.sample.php` to `backend/config.php` and set your MySQL credentials.
3. Deploy `backend/` contents to your web server (e.g. `C:/xampp/htdocs/kinnect`).

### Android App
1. Open the `android/` folder in Android Studio.
2. Place `google-services.json` inside `android/app/`.
3. Sync Gradle and run on device or emulator.
