<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');

$user = require_auth($pdo);

$current = raw_input('current_password');
$new     = raw_input('new_password');
$confirm = raw_input('confirm_password');

$errors = [];

if (is_blank($current)) {
    $errors['current_password'] = 'Current password is required.';
}

if (is_blank($new)) {
    $errors['new_password'] = 'New password is required.';
} elseif (!valid_password($new)) {
    $errors['new_password'] = 'Password must be between 8 and 128 characters.';
}

if (is_blank($confirm)) {
    $errors['confirm_password'] = 'Please confirm your new password.';
} elseif ($new !== $confirm) {
    $errors['confirm_password'] = 'Passwords do not match.';
}

if ($errors) {
    respond_error('Please correct the highlighted fields.', 400, $errors);
}

$stmt = $pdo->prepare('SELECT password_hash FROM users WHERE id = ? LIMIT 1');
$stmt->execute([(int)$user['id']]);
$row = $stmt->fetch();

if (!$row || !password_verify($current, (string)$row['password_hash'])) {
    respond_error('Current password is incorrect.', 400, ['current_password' => 'Current password is incorrect.']);
}

if (password_verify($new, (string)$row['password_hash'])) {
    respond_error('New password must be different from your current password.', 400,
        ['new_password' => 'Choose a password different from your current one.']);
}

$passwordHash = password_hash($new, PASSWORD_BCRYPT, ['cost' => 12]);

try {
    $pdo->beginTransaction();

    $stmt = $pdo->prepare('UPDATE users SET password_hash = ? WHERE id = ?');
    $stmt->execute([$passwordHash, (int)$user['id']]);

    // Force a fresh login on every device that knew the old password.
    $stmt = $pdo->prepare('UPDATE auth_tokens SET revoked = 1 WHERE user_id = ?');
    $stmt->execute([(int)$user['id']]);

    $pdo->commit();
} catch (PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('[ISET Connect] Password change failed: ' . $e->getMessage());
    respond_error('Unable to change your password at this time.', 500);
}

respond_ok(null, 'Password changed successfully. Please log in again.');
