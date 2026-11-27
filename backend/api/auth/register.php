<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');

$firstName = clean('first_name', 100);
$lastName  = clean('last_name', 100);
$cin       = clean('cin', 50);
$className = clean('class_name', 100);
$email     = mb_strtolower(raw_input('email'));
$password  = raw_input('password');
$confirm   = raw_input('confirm_password');

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

if (is_blank($cin)) {
    $errors['cin'] = 'CIN is required.';
} elseif (mb_strlen($cin) < 3 || mb_strlen($cin) > 50) {
    $errors['cin'] = 'CIN must be between 3 and 50 characters.';
} elseif (preg_match('/[\x00-\x1F\x7F]/', $cin)) {
    $errors['cin'] = 'CIN contains invalid characters.';
}

if (is_blank($className)) {
    $errors['class_name'] = 'Class name is required.';
}

if (is_blank($email)) {
    $errors['email'] = 'Email address is required.';
} elseif (!valid_email($email)) {
    $errors['email'] = 'Enter a valid email address.';
}

if (is_blank($password)) {
    $errors['password'] = 'Password is required.';
} elseif (!valid_password($password)) {
    $errors['password'] = 'Password must be between 8 and 128 characters.';
}

if (is_blank($confirm)) {
    $errors['confirm_password'] = 'Please confirm your password.';
} elseif ($password !== $confirm) {
    $errors['confirm_password'] = 'Passwords do not match.';
}

if ($errors) {
    respond_error('Please correct the highlighted fields.', 400, $errors);
}

// Pre-check uniqueness to return field-level errors before attempting the insert.
$stmt = $pdo->prepare('SELECT id FROM users WHERE cin = ? LIMIT 1');
$stmt->execute([$cin]);
if ($stmt->fetch()) {
    respond_error('An account with this CIN already exists.', 400, ['cin' => 'This CIN is already registered.']);
}

$stmt = $pdo->prepare('SELECT id FROM users WHERE email = ? LIMIT 1');
$stmt->execute([$email]);
if ($stmt->fetch()) {
    respond_error('An account with this email already exists.', 400, ['email' => 'This email is already registered.']);
}

$passwordHash = password_hash($password, PASSWORD_BCRYPT, ['cost' => 12]);

try {
    $stmt = $pdo->prepare("INSERT INTO users (first_name, last_name, cin, email, password_hash, class_name, role, status)
                           VALUES (?, ?, ?, ?, ?, ?, 'STUDENT', 'PENDING')");
    $stmt->execute([$firstName, $lastName, $cin, $email, $passwordHash, $className]);
    $userId = (int)$pdo->lastInsertId();
} catch (PDOException $e) {
    if ((string)$e->getCode() === '23000') {
        // A concurrent registration may have won the race on a unique index.
        $cinCheck = $pdo->prepare('SELECT id FROM users WHERE cin = ? LIMIT 1');
        $cinCheck->execute([$cin]);
        if ($cinCheck->fetch()) {
            respond_error('An account with this CIN already exists.', 400, ['cin' => 'This CIN is already registered.']);
        }

        $emailCheck = $pdo->prepare('SELECT id FROM users WHERE email = ? LIMIT 1');
        $emailCheck->execute([$email]);
        if ($emailCheck->fetch()) {
            respond_error('An account with this email already exists.', 400, ['email' => 'This email is already registered.']);
        }

        respond_error('This account already exists.', 400);
    }

    error_log('[ISET Connect] Registration failed: ' . $e->getMessage());
    respond_error('Unable to create your account at this time.', 500);
}

$stmt = $pdo->prepare('SELECT id, first_name, last_name, cin, email, class_name, role, status, created_at
                       FROM users WHERE id = ? LIMIT 1');
$stmt->execute([$userId]);
$user = $stmt->fetch();

respond_created(
    ['user' => public_user($user ?: ['id' => $userId, 'first_name' => $firstName, 'last_name' => $lastName,
        'cin' => $cin, 'email' => $email, 'class_name' => $className, 'role' => 'STUDENT',
        'status' => 'PENDING', 'created_at' => null])],
    'Registration successful. Your account is pending administrator approval.'
);
