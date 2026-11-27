# ISET Connect

Smart university equipment & internship management system — Android (Java/XML) + PHP 8 REST API + MySQL.

## What is included

| Deliverable | Location |
|---|---|
| Android Studio project (Java, XML, Material Design 3) | `app/` |
| PHP 8 REST API (PDO, tokens, RBAC, transactions) | `backend/` |
| MySQL schema, FKs, secure admin seed, sample data | `iset_connect.sql` |
| Postman collection | `postman/ISET_Connect.postman_collection.json` |
| API documentation | `docs/API_DOCUMENTATION.md` |
| XAMPP installation guide | `docs/INSTALLATION_GUIDE.md` |
| Testing guide + 38 automated API checks | `docs/TESTING_GUIDE.md`, `tests/api_smoke_test.ps1` |
| Backend deployment helper | `deploy_backend.ps1` |
| Installable APK (debug-signed) | `apk/ISET-Connect-v1.0-debug.apk` |
| Release APK (unsigned — sign before distribution) | `apk/ISET-Connect-v1.0-release-unsigned.apk` |

## Quick start

1. Start XAMPP Apache + MySQL, import `iset_connect.sql`.
2. Run `powershell -ExecutionPolicy Bypass -File .\deploy_backend.ps1`.
3. Open the folder in Android Studio, sync Gradle, run on emulator or device.
4. Administrator: `admin@iset.tn` / `Admin@ISET2026`. Student demo: `sara.benali@iset.tn` / `Student@123`.

See `docs/INSTALLATION_GUIDE.md` for all details, including physical-device networking and APK builds.

## Architecture

```
Android (Java + Retrofit + SharedPreferences)
        │  HTTPS/HTTP JSON + Bearer token
        ▼
PHP REST API (config / middleware / api / utils)
        │  PDO prepared statements
        ▼
MySQL iset_connect (InnoDB, foreign keys, transactions)
```

No SQLite, Firebase, Kotlin, Compose or Laravel is used. Android never connects to MySQL directly.

## Feature summary

- Registration with admin approval/rejection/suspension and notifications
- Token auth, role-based authorization, session persistence, logout, token expiry
- Admin & student dashboards with live statistics
- Student management with search/filter and account actions
- Equipment CRUD, inventory, borrowing requests, approval/rejection/cancellation/returns with transactional stock updates
- Internship CRUD, publishing/closing, applications, accept/reject/cancel with position management
- Centralized notifications and profile/password management
- 28 responsive screens with Material cards, bottom navigation (students), navigation drawer (admin), search, filters, FABs, dialogs, loading and empty states
