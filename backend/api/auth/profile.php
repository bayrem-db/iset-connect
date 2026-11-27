<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
if ($method !== 'GET' && $method !== 'POST') {
    respond_error('Method not allowed. Use GET or POST.', 405);
}

$user = require_auth($pdo);

if ($method === 'GET') {
    respond_ok(['user' => public_user($user)]);
}

$firstName = clean('first_name', 100);
$lastName  = clean('last_name', 100);
$className = clean('class_name', 100);
$email     = mb_strtolower(raw_input('email'));

$errors = [];

if (is_blank($firstName)) {
    $errors['first_name'] = 'First name is required.';
} elseif (mb_strlen($firstName) < 2) {
    $errors['first_name'] = 'First name must contain at least 2 characters.';
}

if (is_blank($lastName)) {
    $errors['last_name'] = 'Last name is required.';
} elseif (mb_strlen($lastName) < 2) {
    $errors['last_name'] = 'Last name must contain at least 2 characters.';
}

if (is_blank($className)) {
    $errors['class_name'] = 'Class name is required.';
}

if (is_blank($email)) {
    $errors['email'] = 'Email address is required.';
} elseif (!valid_email($email)) {
    $errors['email'] = 'Enter a valid email address.';
}

if ($errors) {
    respond_error('Please correct the highlighted fields.', 400, $errors);
}

$stmt = $pdo->prepare('SELECT id FROM users WHERE email = ? AND id <> ? LIMIT 1');
$stmt->execute([$email, (int)$user['id']]);
if ($stmt->fetch()) {
    respond_error('This email is already in use.', 400, ['email' => 'This email is already registered by another account.']);
}

try {
    $stmt = $pdo->prepare('UPDATE users SET first_name = ?, last_name = ?, class_name = ?, email = ? WHERE id = ?');
    $stmt->execute([$firstName, $lastName, $className, $email, (int)$user['id']]);
} catch (PDOException $e) {
    if ((string)$e->getCode() === '23000') {
        respond_error('This email is already in use.', 400, ['email' => 'This email is already registered by another account.']);
    }
    error_log('[ISET Connect] Profile update failed: ' . $e->getMessage());
    respond_error('Unable to update your profile at this time.', 500);
}

$stmt = $pdo->prepare('SELECT id, first_name, last_name, cin, email, class_name, role, status, created_at
                       FROM users WHERE id = ? LIMIT 1');
$stmt->execute([(int)$user['id']]);
$updated = $stmt->fetch();

respond_ok(['user' => public_user($updated ?: $user)], 'Profile updated successfully.');
