# ISET Connect — Testing guide

## Automated backend test

1. Start MySQL and Apache (or the PHP built-in server shown in the installation guide).
2. Deploy the backend: `powershell -ExecutionPolicy Bypass -File .\deploy_backend.ps1`
3. Run: `powershell -ExecutionPolicy Bypass -File .\tests\api_smoke_test.ps1`

The script covers 47 assertions: authentication, blocked account states, token/role authorization,
both dashboards, student CRUD and search, registration uniqueness, material CRUD, borrowing lifecycle
(inventory decrement/restock, insufficient quantity, double return), internship publishing,
application lifecycle (duplicate, double accept, position decrement), notifications and profile/password.

## Manual test matrix

### Authentication
| Scenario | Expected |
|---|---|
| Admin `admin@iset.tn` / `Admin@ISET2026` | Admin dashboard, stats filled |
| Wrong password | "Invalid email or password", no token |
| Register duplicate email | 400, field error |
| Register duplicate CIN | 400, field error |
| Register new student | Waiting-for-approval screen |
| Login while PENDING | 403, waiting screen |
| Admin approves student | Notification generated; login succeeds |
| Admin rejects/suspends student | Login blocked; notification generated |
| Student opens admin screen / calls admin API | 403 |

### Equipment
| Scenario | Expected |
|---|---|
| Add / edit material | Reflected in list and MySQL |
| Delete unused material | Success |
| Delete material with loan history | Rejected (history preserved) |
| Search by name, filter by category | Correct subset |
| Request more than available | 400 |
| Request with return date before borrow date | 400 |
| Approve request | Stock decreases by requested quantity, notification |
| Reject request | Status REJECTED, notification |
| Cancel own PENDING request | Status CANCELLED |
| Return approved equipment | Stock increases, status RETURNED, notification |
| Return twice | 400 |

### Internships
| Scenario | Expected |
|---|---|
| Create DRAFT, then publish | All approved students get a notification |
| Edit / close / delete (no applications) | Success |
| Student applies | PENDING application, no second application allowed |
| Apply after deadline | Rejected by backend |
| Accept application | Positions decrease, notification |
| Accept same application twice | 400 |
| Reject application | Status REJECTED, notification |
| Cancel own PENDING application | Status CANCELLED |

### UI checks
- Bottom navigation works on student list/dashboard screens.
- Admin navigation drawer opens all six management sections and signs out.
- RecyclerView empty states appear when filters yield no rows.
- Loading indicators appear during API calls and disappear afterwards.
- Confirmation dialogs appear for destructive/state-changing actions.
- Notifications unread badge/count updates after marking as read.

## Verification in MySQL

After each manual test, verify persisted state:

```sql
USE iset_connect;
SELECT id, name, total_quantity, available_quantity FROM materials;
SELECT id, student_id, material_id, quantity, status, actual_return_date FROM loans ORDER BY id DESC;
SELECT id, title, available_positions, status FROM internships;
SELECT id, student_id, internship_id, status FROM applications;
SELECT id, user_id, title, notification_type, is_read FROM notifications ORDER BY id DESC;
SELECT u.email, t.expires_at, t.revoked FROM auth_tokens t JOIN users u ON u.id=t.user_id;
```

## Reset to a clean demo state

Re-import `iset_connect.sql`; this drops and recreates all tables and re-seeds the administrator.
