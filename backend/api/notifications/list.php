<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('GET');
$user = require_auth($pdo);

// Let the shared input() helpers also read parameters passed in the query string.
$_POST = $_POST + $_GET;

$unreadOnly = in_array(mb_strtolower(raw_input('unread_only')), ['1', 'true', 'yes'], true);

$sql = 'SELECT id, title, message, notification_type, is_read, created_at
        FROM notifications WHERE user_id = ?';
if ($unreadOnly) {
    $sql .= ' AND is_read = 0';
}
$sql .= ' ORDER BY created_at DESC, id DESC';

$stmt = $pdo->prepare($sql);
$stmt->execute([(int)$user['id']]);

$notifications = [];
foreach ($stmt->fetchAll() as $row) {
    $notifications[] = [
        'id'                => (int)$row['id'],
        'title'             => $row['title'],
        'message'           => $row['message'],
        'notification_type' => $row['notification_type'],
        'is_read'           => (int)$row['is_read'],
        'created_at'        => $row['created_at'],
    ];
}

$stmt = $pdo->prepare('SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0');
$stmt->execute([(int)$user['id']]);
$unreadCount = (int)$stmt->fetchColumn();

respond_ok([
    'notifications' => $notifications,
    'unread_count'  => $unreadCount,
]);
