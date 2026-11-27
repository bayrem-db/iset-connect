<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('GET');
$admin = require_admin($pdo);

/** Count rows with a fully prepared statement. */
$count = static function (string $sql, array $params = []) use ($pdo): int {
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    return (int)$stmt->fetchColumn();
};

respond_ok([
    'total_students'         => $count("SELECT COUNT(*) FROM users WHERE role = 'STUDENT'"),
    'pending_students'       => $count("SELECT COUNT(*) FROM users WHERE role = 'STUDENT' AND status = 'PENDING'"),
    'approved_students'      => $count("SELECT COUNT(*) FROM users WHERE role = 'STUDENT' AND status = 'APPROVED'"),
    'total_materials'        => $count('SELECT COUNT(*) FROM materials'),
    'available_materials'    => $count('SELECT COUNT(*) FROM materials WHERE available_quantity > 0'),
    'borrowed_materials'     => $count('SELECT COUNT(*) FROM materials WHERE available_quantity < total_quantity'),
    'total_loans'            => $count('SELECT COUNT(*) FROM loans'),
    'pending_loans'          => $count("SELECT COUNT(*) FROM loans WHERE status = 'PENDING'"),
    'total_internships'      => $count('SELECT COUNT(*) FROM internships'),
    'published_internships'  => $count("SELECT COUNT(*) FROM internships WHERE status = 'PUBLISHED'"),
    'pending_applications'   => $count("SELECT COUNT(*) FROM applications WHERE status = 'PENDING'"),
    'unread_notifications'   => $count('SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0',
        [(int)$admin['id']]),
]);
