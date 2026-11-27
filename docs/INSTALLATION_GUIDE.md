# ISET Connect — Installation & setup guide

## 1. Requirements

- Windows with **XAMPP** (PHP 8.2+, MySQL/MariaDB, Apache)
- **Android Studio** (Ladybug or newer) with Android SDK 36/37 installed
- JDK 17+ (Gradle wrapper 9.6 is included)
- A physical Android device or emulator with API 24+
- This project folder

## 2. Database setup (MySQL)

1. Start **Apache** and **MySQL** in the XAMPP control panel.
2. Open `http://localhost/phpmyadmin`.
3. Import `iset_connect.sql` from the project root (Import → Choose file → Go).
   - Or from a terminal: `mysql -u root -p < iset_connect.sql`
4. Confirm the `iset_connect` database contains: `users`, `auth_tokens`, `materials`, `loans`, `internships`, `applications`, `notifications`.

The script creates the administrator account and optional demo data.

| Account | Email | Password |
|---|---|---|
| Administrator | `admin@iset.tn` | `Admin@ISET2026` |
| Demo approved student | `sara.benali@iset.tn` | `Student@123` |
| Demo pending student | `mohamed.t@iset.tn` | `Student@123` |
| Demo approved student | `ines.k@iset.tn` | `Student@123` |

> The SQL file stores only a bcrypt hash of these passwords. Change the administrator password after first login using the app or `password_hash()` in PHP.

## 3. Backend deployment

Run from the project root in PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy_backend.ps1
```

This copies `backend/` to `C:\xampp\htdocs\iset_connect\backend\`.

Verify in a browser: `http://localhost/iset_connect/backend/index.php`
Expected: `{"success":true,"message":"OK","data":{"service":"ISET Connect REST API",...}}`

Database credentials default to `root` / empty password. To override, set environment variables
`ISET_DB_HOST`, `ISET_DB_NAME`, `ISET_DB_USER`, `ISET_DB_PASS` for the Apache/PHP process
(or edit `backend/config/database.php`).

Optional quick test without Apache:

```powershell
php -S 127.0.0.1:8765 -t C:\xampp\htdocs\iset_connect\backend
powershell -ExecutionPolicy Bypass -File .\tests\api_smoke_test.ps1 -BaseUrl "http://127.0.0.1:8765/api/"
```

## 4. Android project setup

1. Open the project root folder in Android Studio and let Gradle sync.
2. `app/build.gradle.kts` defines `BASE_URL` per build type:
   - debug: `http://10.0.2.2/iset_connect/backend/api/` (emulator)
   - release: `http://10.0.2.2/iset_connect/backend/api/` (change to your HTTPS domain in production)
3. **Physical device:** tap **Server settings** on the login screen and enter your computer's LAN IP,
   e.g. `http://192.168.1.12/iset_connect/backend/api/`. The address is stored locally and survives sign-out,
   so one APK works on both emulator and device without recompiling. Phone and PC must be on the same Wi‑Fi;
   allow Apache through Windows Firewall. The manifest already enables cleartext traffic for local HTTP.
4. Run on a device/emulator. Android Studio installs the app automatically, or install
   `apk/ISET-Connect-v1.0-debug.apk` (already built and debug-signed).

## 5. Build a release APK

Android Studio: **Build → Generate Signed Bundle / APK → APK**, create a keystore, select `release`.
Or via CLI:

```powershell
.\gradlew.bat assembleRelease
```

APK output: `app\build\outputs\apk\release\app-release.apk`

> Use HTTPS and a real domain in production; cleartext HTTP must never be used with real student data.

## 6. Common problems

| Problem | Fix |
|---|---|
| "Unable to reach the server" on emulator | XAMPP Apache running? URL must be `10.0.2.2`, not `localhost`, inside the emulator. |
| Same error on physical device | Use LAN IP, same Wi‑Fi, firewall allows port 80. |
| Login 500 error | Check `C:\xampp\apache\logs\error.log`; confirm database imported. |
| Pending login rejected | Expected: administrator must approve the account first. |
| Token invalid after reinstall | App session is local; log in again. Tokens expire after 7 days. |
| 403 on admin screens | Log in with the administrator account, not a student account. |

## 7. Project layout

```
iset_connect.sql              MySQL schema + secure admin seed + sample data
backend/                      PHP REST API (deploy with deploy_backend.ps1)
app/src/main/java/...         Android Java sources (activities, adapters, models, network, utils)
app/src/main/res/             XML layouts, drawables, values
docs/API_DOCUMENTATION.md     Full endpoint reference
docs/TESTING_GUIDE.md         Test scenarios
postman/ISET_Connect.postman_collection.json
tests/api_smoke_test.ps1      38 automated API checks
```
