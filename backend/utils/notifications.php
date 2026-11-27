<?php
/** Notification helpers shared by all endpoints. */
function notify_user(PDO $pdo, $userId, string $title, string $message, string $type): void
{
    $stmt = $pdo->prepare('INSERT INTO notifications (user_id, title, message, notification_type, is_read, created_at)
                           VALUES (?, ?, ?, ?, 0, NOW())');
    $stmt->execute([(int)$userId, $title, $message, $type]);
}

/** Notify every approved student (used when an internship is published). */
function notify_all_students(PDO $pdo, string $title, string $message, string $type): void
{
    $users = $pdo->query("SELECT id FROM users WHERE role = 'STUDENT' AND status = 'APPROVED'")->fetchAll();
    $stmt = $pdo->prepare('INSERT INTO notifications (user_id, title, message, notification_type, is_read, created_at)
                           VALUES (?, ?, ?, ?, 0, NOW())');
    foreach ($users as $user) {
        $stmt->execute([(int)$user['id'], $title, $message, $type]);
    }
}
