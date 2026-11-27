<?php
/** Token authentication middleware. Always derives the acting user from the bearer token. */
const TOKEN_TTL_DAYS = 7;

function bearer_token(): string
{
    $header = '';
    if (isset($_SERVER['HTTP_AUTHORIZATION'])) {
        $header = $_SERVER['HTTP_AUTHORIZATION'];
    } elseif (isset($_SERVER['REDIRECT_HTTP_AUTHORIZATION'])) {
        $header = $_SERVER['REDIRECT_HTTP_AUTHORIZATION'];
    } elseif (function_exists('getallheaders')) {
        foreach (getallheaders() as $name => $value) {
            if (strcasecmp($name, 'Authorization') === 0) {
                $header = $value;
                break;
            }
        }
    }
    if (preg_match('/^Bearer\s+(.+)$/i', trim($header), $matches)) {
        return trim($matches[1]);
    }
    return '';
}

/** Issue a new token and store only its SHA-256 hash. */
function issue_token(PDO $pdo, int $userId): string
{
    $token = bin2hex(random_bytes(32));
    $hash = hash('sha256', $token);
    $stmt = $pdo->prepare('INSERT INTO auth_tokens (user_id, token_hash, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL ? DAY))');
    $stmt->execute([$userId, $hash, TOKEN_TTL_DAYS]);
    return $token;
}

function revoke_current_token(PDO $pdo): void
{
    $token = bearer_token();
    if ($token !== '') {
        $stmt = $pdo->prepare('UPDATE auth_tokens SET revoked = 1 WHERE token_hash = ?');
        $stmt->execute([hash('sha256', $token)]);
    }
}

/** @return array authenticated user row */
function require_auth(PDO $pdo): array
{
    $token = bearer_token();
    if ($token === '') {
        respond_error('Authentication token is missing.', 401);
    }
    $stmt = $pdo->prepare('SELECT u.id, u.first_name, u.last_name, u.cin, u.email, u.class_name,
                                  u.role, u.status, u.created_at, t.expires_at
                           FROM auth_tokens t JOIN users u ON u.id = t.user_id
                           WHERE t.token_hash = ? AND t.revoked = 0 AND t.expires_at > NOW()
                           LIMIT 1');
    $stmt->execute([hash('sha256', $token)]);
    $user = $stmt->fetch();
    if (!$user) {
        respond_error('Session is invalid or has expired. Please log in again.', 401);
    }
    if ($user['status'] !== 'APPROVED') {
        respond_error('Your account is not active.', 403, [], ['status' => $user['status']]);
    }
    unset($user['expires_at']);
    return $user;
}

/** @return array authenticated admin row */
function require_admin(PDO $pdo): array
{
    $user = require_auth($pdo);
    if ($user['role'] !== 'ADMIN') {
        respond_error('Administrator access is required.', 403);
    }
    return $user;
}

function require_student(PDO $pdo): array
{
    $user = require_auth($pdo);
    if ($user['role'] !== 'STUDENT') {
        respond_error('Student access is required.', 403);
    }
    return $user;
}

/** Public user representation used in API responses. */
function public_user(array $user): array
{
    return [
        'id'         => (int)$user['id'],
        'first_name' => $user['first_name'],
        'last_name'  => $user['last_name'],
        'cin'        => $user['cin'],
        'email'      => $user['email'],
        'class_name' => $user['class_name'],
        'role'       => $user['role'],
        'status'     => $user['status'],
        'created_at' => $user['created_at'] ?? null,
    ];
}
