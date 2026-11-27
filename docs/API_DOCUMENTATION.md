# ISET Connect REST API — documentation v1

Base URL: `http://<host>/iset_connect/backend/api/`
All requests and responses use UTF-8. Request bodies are form-encoded (`application/x-www-form-urlencoded`); the backend also accepts a JSON body. Every response has this envelope:

```json
{ "success": true, "message": "OK", "data": { ... } }
```

Errors add an `errors` object keyed by field name when validation fails.

## Authorization

Send the token returned by `login.php` on every protected endpoint:

```
Authorization: Bearer <token>
```

Tokens are random 256-bit values; only their SHA-256 hash is stored in `auth_tokens`. They expire after 7 days and are revoked on logout, password change, rejection or suspension. The acting identity is **always** taken from the token; client-supplied user ids are treated only as target ids and are checked against the token's role.

| Code | Meaning |
|---|---|
| 200 | Success |
| 201 | Created |
| 400 | Validation error / business rule violated |
| 401 | Missing, invalid or expired token |
| 403 | Wrong role, not the owner, or account not APPROVED |
| 404 | Resource not found |
| 405 | Wrong HTTP method |
| 500 | Server error (details are logged, never returned) |

---

## Authentication

### POST `auth/register.php`
Fields: `first_name`, `last_name`, `cin`, `email`, `password`, `confirm_password`, `class_name`.
Rules: all required, CIN/email unique, password ≥ 8 chars, bcrypt cost 12 on the backend, new account `PENDING` / `STUDENT`.

```json
{ "success": true, "message": "Registration submitted. Awaiting administrator approval.", "data": { "id": 5 } }
```

### POST `auth/login.php`
Fields: `email`, `password`. Approved accounts only; blocked accounts return 403 with `data.status`.

```json
{
  "success": true, "message": "Login successful.",
  "data": {
    "token": "64 hex characters",
    "user": { "id": 1, "first_name": "ISET", "last_name": "Administrator", "cin": "ADMIN001",
              "email": "admin@iset.tn", "class_name": "Administration", "role": "ADMIN", "status": "APPROVED" }
  }
}
```

### POST `auth/logout.php` — auth required
Revokes the current token.

### GET / POST `auth/profile.php` — auth required
GET returns `data.user`. POST accepts `first_name`, `last_name`, `class_name`, `email` (unique).

### POST `auth/change_password.php` — auth required
Fields: `current_password`, `new_password`, `confirm_password`. Revokes all tokens afterwards.

---

## Dashboards

### GET `dashboard/admin.php` — admin
```json
{ "success": true, "message": "OK", "data": {
  "total_students": 3, "pending_students": 1, "approved_students": 2,
  "total_materials": 7, "available_materials": 6, "borrowed_materials": 1,
  "total_loans": 2, "pending_loans": 1, "total_internships": 4,
  "published_internships": 3, "pending_applications": 1, "unread_notifications": 2 } }
```

### GET `dashboard/student.php` — student
Returns `available_materials`, `my_loans`, `my_borrowed`, `available_internships`, `my_applications`, `unread_notifications` and `user`.

---

## Students — admin only

| Endpoint | Method | Parameters |
|---|---|---|
| `students/list.php` | GET | `q` (name/CIN/email), `status` |
| `students/details.php` | GET | `id` → `student`, `loans`, `applications` |
| `students/approve.php` | POST | `id` |
| `students/reject.php` | POST | `id`, optional `reason` |
| `students/suspend.php` | POST | `id`, optional `reason` |
| `students/reactivate.php` | POST | `id` |

Every status change inserts a notification for the student and revokes active tokens on reject/suspend.

---

## Materials

| Endpoint | Method | Auth | Parameters |
|---|---|---|---|
| `materials/list.php` | GET | user | `q`, `category` — students see only items in stock |
| `materials/details.php` | GET | user | `id` |
| `materials/create.php` | POST | admin | `name`, `description`, `category`, `total_quantity`, optional `image_url` |
| `materials/update.php` | POST | admin | `id` + create fields (total cannot drop below units already on loan) |
| `materials/delete.php` | POST | admin | `id` — refused when loan history exists |

Material object:
```json
{ "id": 1, "name": "Laptop HP", "description": "…", "category": "Computers",
  "total_quantity": 10, "available_quantity": 7, "image_url": null,
  "created_at": "2026-11-26 10:00:00", "updated_at": "2026-11-26 10:00:00" }
```

---

## Loans

| Endpoint | Method | Auth | Parameters |
|---|---|---|---|
| `loans/create.php` | POST | student | `material_id`, `quantity`, `borrowing_date`, `expected_return_date` (YYYY-MM-DD) |
| `loans/list.php` | GET | user | `q`, `status`, `scope=history` |
| `loans/details.php` | GET | user | `id` (owner or admin) |
| `loans/approve.php` | POST | admin | `id` |
| `loans/reject.php` | POST | admin | `id`, optional `reason` |
| `loans/cancel.php` | POST | owner/admin | `id` (PENDING only) |
| `loans/return.php` | POST | admin | `id` (APPROVED only) |

Status flow: `PENDING → APPROVED → RETURNED`, or `PENDING → REJECTED/CANCELLED`.
Inventory changes only at approval (`available -= quantity`) and return (`available += quantity`), inside transactions with `SELECT … FOR UPDATE` and a fresh availability check. Students cannot request more than the current stock; return dates must be strictly after borrowing dates. Notifications are created for approval, rejection and return.

Loan object includes `id, student_id, student_name, material_id, material_name, material_category, quantity, request_date, borrowing_date, expected_return_date, actual_return_date, status`.

---

## Internships

| Endpoint | Method | Auth | Parameters |
|---|---|---|---|
| `internships/list.php` | GET | user | `q`, `domain`, `status`; students see PUBLISHED only |
| `internships/details.php` | GET | user | `id` |
| `internships/create.php` | POST | admin | 11 fields (see below) |
| `internships/update.php` | POST | admin | `id` + 11 fields |
| `internships/publish.php` | POST | admin | `id` |
| `internships/close.php` | POST | admin | `id` |
| `internships/delete.php` | POST | admin | `id` — refused when applications exist |

Create/update fields: `title`, `company_name`, `company_address`, `domain`, `description`, `required_skills`, `start_date`, `end_date`, `application_deadline`, `total_positions`, `status` (`DRAFT`/`PUBLISHED`/`CLOSED`/`EXPIRED`). Publishing notifies every approved student.

---

## Applications

| Endpoint | Method | Auth | Parameters |
|---|---|---|---|
| `applications/create.php` | POST | student | `internship_id`, `motivation_message` |
| `applications/list.php` | GET | user | `q`, `status`, `internship_id` |
| `applications/accept.php` | POST | admin | `id` |
| `applications/reject.php` | POST | admin | `id`, optional `reason` |
| `applications/cancel.php` | POST | student | `id` (PENDING only) |

Rules: PUBLISHED offer, deadline not passed, one application per student+internship, accept only while positions remain (transaction + row lock), positions decrement atomically, same application cannot be accepted twice, notification on accept/reject.

---

## Notifications

### GET `notifications/list.php` — auth
Optional `unread_only=1`. Returns `data.notifications` and `data.unread_count`.

### POST `notifications/mark_read.php` — auth
Send `id` or `all=1`. Only the authenticated user's rows are updated.

Notification object: `id, title, message, notification_type, is_read, created_at`.
Types: `ACCOUNT_APPROVED`, `ACCOUNT_REJECTED`, `ACCOUNT_SUSPENDED`, `ACCOUNT_REACTIVATED`, `LOAN_APPROVED`, `LOAN_REJECTED`, `LOAN_RETURNED`, `INTERNSHIP_PUBLISHED`, `APPLICATION_ACCEPTED`, `APPLICATION_REJECTED`.

---

## Security notes

- All SQL uses PDO prepared statements; all user text is sanitized.
- Passwords hashed with `password_hash()` and checked with `password_verify()`.
- Database credentials live only in the backend configuration (overridable by environment variables).
- PHP errors are logged, never displayed to clients.
- Use HTTPS in production and disable cleartext traffic in the Android manifest.
