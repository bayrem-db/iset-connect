<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
$user = require_auth($pdo);

$all = mb_strtolower(raw_input('all'));
$id  = raw_input('id');

if (in_array($all, ['1', 'true', 'yes'], true)) {
    $stmt = $pdo->prepare('UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0');
    $stmt->execute([(int)$user['id']]);

    respond_ok(['marked_count' => (int)$stmt->rowCount()], 'All notifications marked as read.');
}

if ($id === '') {
    respond_error('Provide a notification id or set all=1.', 400,
        ['id' => 'A valid notification id is required when all=1 is not used.']);
}

if (!positive_int($id)) {
    respond_error('A valid notification id is required.', 400, ['id' => 'Provide a valid positive notification id.']);
}

// Confirm ownership before updating so another user's notification is never touched.
$stmt = $pdo->prepare('SELECT id, is_read FROM notifications WHERE id = ? AND user_id = ? LIMIT 1');
$stmt->execute([(int)$id, (int)$user['id']]);
$notification = $stmt->fetch();
if (!$notification) {
    respond_error('Notification not found.', 404);
}

$stmt = $pdo->prepare('UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?');
$stmt->execute([(int)$id, (int)$user['id']]);

respond_ok(
    ['notification_id' => (int)$id, 'already_read' => (int)$notification['is_read'] === 1],
    'Notification marked as read.'
);
