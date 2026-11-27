# ISET Connect — REST API contract (v1)

Base URL (emulator): `http://10.0.2.2/iset_connect/backend/api/`
Base URL (physical device): `http://<PC-LAN-IP>/iset_connect/backend/api/`

Every response is JSON with this exact envelope:

```json
{ "success": true, "message": "OK", "data": { } }
```

Errors use `success: false`, an explanatory `message`, HTTP status code and optionally
an `errors` object keyed by field name. Request bodies are `application/x-www-form-urlencoded`
(or JSON — endpoints read `$_POST` first, then a JSON body). Protected endpoints require
`Authorization: Bearer <token>`; admin endpoints also require role `ADMIN`. Tokens expire
after 7 days and are revoked on logout. The backend always derives the acting user from the
token — **never** from a client-supplied user id.

## Authentication — `api/auth/`

| Endpoint | Method | Auth | Fields / Query | Notes |
|---|---|---|---|---|
| `register.php` | POST | no | `first_name`, `last_name`, `cin`, `email`, `password`, `confirm_password`, `class_name` | Creates `PENDING` student. Unique CIN/email. Password hashed with `password_hash`. |
| `login.php` | POST | no | `email`, `password` | Returns `data.token`, `data.user`. Blocks PENDING/REJECTED/SUSPENDED with a distinct `data.status` value. |
| `logout.php` | POST | user | – | Revokes current token. |
| `profile.php` | GET | user | – | Returns current user. |
| `profile.php` | POST | user | `first_name`, `last_name`, `class_name`, `email` | Editable profile fields, email uniqueness checked. |
| `change_password.php` | POST | user | `current_password`, `new_password`, `confirm_password` | Revokes all tokens after success. |

`user` object: `{id, first_name, last_name, cin, email, class_name, role, status, created_at}`
`full_name` is computed on the client from first_name + last_name.

## Dashboards — `api/dashboard/`

| Endpoint | Method | Auth | Returns |
|---|---|---|---|
| `admin.php` | GET | admin | `total_students`, `pending_students`, `approved_students`, `total_materials`, `available_materials`, `borrowed_materials`, `total_loans`, `pending_loans`, `total_internships`, `published_internships`, `pending_applications`, `unread_notifications` |
| `student.php` | GET | student | `available_materials`, `my_loans`, `my_borrowed`, `available_internships`, `my_applications`, `unread_notifications`, plus `user` |

## Students — `api/students/` (all admin)

| Endpoint | Method | Query / Fields |
|---|---|---|
| `list.php` | GET | `q` (name/CIN/email), `status` optional → `data.students` array |
| `details.php?id=` | GET | `data.student` + `data.loans` + `data.applications` |
| `approve.php` | POST | `id` → status APPROVED + notification |
| `reject.php` | POST | `id`, optional `reason` → status REJECTED + notification |
| `suspend.php` | POST | `id`, optional `reason` → status SUSPENDED + notification |
| `reactivate.php` | POST | `id` → status APPROVED + notification |

## Materials — `api/materials/`

| Endpoint | Method | Auth | Fields / Query |
|---|---|---|---|
| `list.php` | GET | user | `q`, `category` optional → `data.materials`. Students see only `available_quantity > 0`. |
| `details.php?id=` | GET | user | `data.material` |
| `create.php` | POST | admin | `name`, `description`, `category`, `total_quantity`, `image_url` optional |
| `update.php` | POST | admin | `id` + same fields as create; `total_quantity` cannot be lower than already loaned units |
| `delete.php` | POST | admin | `id`; rejected if any loan references the material |

`material`: `{id, name, description, category, total_quantity, available_quantity, image_url, created_at, updated_at}`

## Loans — `api/loans/`

| Endpoint | Method | Auth | Fields / Query |
|---|---|---|---|
| `create.php` | POST | student | `material_id`, `quantity`, `borrowing_date` (YYYY-MM-DD), `expected_return_date` (YYYY-MM-DD) |
| `list.php` | GET | user | Admin: all loans, student: own loans. `q`, `status`, `scope=history` optional → `data.loans` |
| `details.php?id=` | GET | user | `data.loan` (owner or admin only) |
| `approve.php` | POST | admin | `id`; transaction + availability re-check, decrements `available_quantity`, notification |
| `reject.php` | POST | admin | `id`, optional `reason`; notification |
| `cancel.php` | POST | student|admin | `id`; only PENDING, student can cancel only own request |
| `return.php` | POST | admin | `id`; only APPROVED, increments `available_quantity`, prevents double return, notification |

`loan`: `{id, student_id, student_name, material_id, material_name, material_category, quantity,
request_date, borrowing_date, expected_return_date, actual_return_date, status}`

## Internships — `api/internships/`

| Endpoint | Method | Auth | Fields / Query |
|---|---|---|---|
| `list.php` | GET | user | `q`, `domain`, `status`. Students see PUBLISHED only (admin sees all) → `data.internships` |
| `details.php?id=` | GET | user | `data.internship` |
| `create.php` | POST | admin | `title`, `company_name`, `company_address`, `domain`, `description`, `required_skills`, `start_date`, `end_date`, `application_deadline`, `total_positions`, `status` |
| `update.php` | POST | admin | `id` + create fields |
| `delete.php` | POST | admin | `id`; rejected if applications exist |
| `publish.php` | POST | admin | `id` → status PUBLISHED + notifications for all approved students |
| `close.php` | POST | admin | `id` → status CLOSED |

`internship`: `{id, title, company_name, company_address, domain, description, required_skills,
start_date, end_date, application_deadline, total_positions, available_positions, status, created_at,
has_applied}` (`has_applied` is 1 for the current student)

## Applications — `api/applications/`

| Endpoint | Method | Auth | Fields / Query |
|---|---|---|---|
| `create.php` | POST | student | `internship_id`, `motivation_message` |
| `list.php` | GET | user | Admin: all (filters `q`, `status`, `internship_id`); student: own → `data.applications` |
| `accept.php` | POST | admin | `id`; transaction, position re-check, decrements `available_positions`, notification |
| `reject.php` | POST | admin | `id`, optional `reason`; notification |
| `cancel.php` | POST | student | `id`; only own + PENDING |

`application`: `{id, student_id, student_name, cin, class_name, internship_id, internship_title,
company_name, motivation_message, application_date, status}`

## Notifications — `api/notifications/`

| Endpoint | Method | Auth | Fields / Query |
|---|---|---|---|
| `list.php` | GET | user | `unread_only=1` optional → `data.notifications`, `data.unread_count` |
| `mark_read.php` | POST | user | `id` **or** `all=1` |

`notification`: `{id, title, message, notification_type, is_read, created_at}`

## Status codes

- 200 success · 201 created · 400 validation/business error · 401 missing/invalid token
- 403 forbidden (wrong role / not owner / account blocked) · 404 not found · 405 wrong method · 500 server error

## Notification types

`ACCOUNT_APPROVED`, `ACCOUNT_REJECTED`, `ACCOUNT_SUSPENDED`, `ACCOUNT_REACTIVATED`,
`LOAN_APPROVED`, `LOAN_REJECTED`, `LOAN_RETURNED`, `INTERNSHIP_PUBLISHED`,
`APPLICATION_ACCEPTED`, `APPLICATION_REJECTED`
