<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');

$email    = mb_strtolower(raw_input('email'));
$password = raw_input('password');

$missing = [];
if (is_blank($email)) {
    $missing['email'] = 'Email is required.';
}
if (is_blank($password)) {
    $missing['password'] = 'Password is required.';
}
if ($missing) {
    respond_error('Email and password are required.', 400, $missing);
}

if (!valid_email($email)) {
    // Same generic failure as a wrong password or unknown account.
    respond_error('Invalid email or password.', 401);
}

$stmt = $pdo->prepare('SELECT id, first_name, last_name, cin, email, class_name, role, status, created_at, password_hash
                       FROM users WHERE email = ? LIMIT 1');
$stmt->execute([$email]);
$user = $stmt->fetch();

// Verify against a real bcrypt hash even for unknown emails to avoid trivial user enumeration by timing.
$dummyHash = '$2y$12$WyJFZFGFXHxQnvKCpkcKb.OefTLqciSFlnm9D0o7y.CS1odfq8uUm';
$storedHash = $user ? (string)$user['password_hash'] : $dummyHash;
$passwordOk = password_verify($password, $storedHash);

if (!$user || !$passwordOk) {
    respond_error('Invalid email or password.', 401);
}

if ($user['status'] !== 'APPROVED') {
    $messages = [
        'PENDING'   => 'Your account is pending administrator approval.',
        'REJECTED'  => 'Your account registration was rejected by an administrator.',
        'SUSPENDED' => 'Your account has been suspended. Please contact an administrator.',
    ];
    $message = $messages[$user['status']] ?? 'Your account is not active.';
    respond_error($message, 403, [], ['status' => $user['status']]);
}

$token = issue_token($pdo, (int)$user['id']);

respond_ok([
    'token' => $token,
    'user'  => public_user($user),
], 'Login successful.');
