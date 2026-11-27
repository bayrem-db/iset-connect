<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
require_admin($pdo);

$id = raw_input('id');
if (!positive_int($id)) {
    respond_error('A valid student id is required.', 400, ['id' => 'Provide a valid positive student id.']);
}

$stmt = $pdo->prepare('SELECT id, first_name, last_name, cin, email, class_name, role, status, created_at
                       FROM users WHERE id = ? AND role = ? LIMIT 1');
$stmt->execute([(int)$id, 'STUDENT']);
$student = $stmt->fetch();
if (!$student) {
    respond_error('Student not found.', 404);
}

if ($student['status'] !== 'APPROVED') {
    $detail = $student['status'] === 'SUSPENDED'
        ? 'This account is already suspended.'
        : 'Only an approved student account can be suspended.';
    respond_error('This student cannot be suspended from its current status.', 400, ['id' => $detail]);
}

$reason = clean('reason', 300);
$message = 'Your ISET Connect account has been suspended.';
if (!is_blank($reason)) {
    $message .= ' Reason: ' . $reason;
}

try {
    $pdo->beginTransaction();

    $stmt = $pdo->prepare("UPDATE users SET status = 'SUSPENDED' WHERE id = ? AND role = 'STUDENT'");
    $stmt->execute([(int)$id]);

    $stmt = $pdo->prepare('UPDATE auth_tokens SET revoked = 1 WHERE user_id = ?');
    $stmt->execute([(int)$id]);

    notify_user($pdo, (int)$id, 'Account suspended', $message, 'ACCOUNT_SUSPENDED');

    $pdo->commit();
} catch (PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('[ISET Connect] Student suspension failed: ' . $e->getMessage());
    respond_error('Unable to suspend this student at this time.', 500);
}

$stmt = $pdo->prepare('SELECT id, first_name, last_name, cin, email, class_name, role, status, created_at
                       FROM users WHERE id = ? LIMIT 1');
$stmt->execute([(int)$id]);

respond_ok(['student' => public_user($stmt->fetch() ?: $student)], 'Student account suspended.');
