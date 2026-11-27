<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('GET');
$student = require_student($pdo);
$studentId = (int)$student['id'];

/** Count rows scoped to the authenticated student. */
$count = static function (string $sql, array $params = []) use ($pdo): int {
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    return (int)$stmt->fetchColumn();
};

respond_ok([
    'available_materials'   => $count('SELECT COUNT(*) FROM materials WHERE available_quantity > 0'),
    'my_loans'              => $count('SELECT COUNT(*) FROM loans WHERE student_id = ?', [$studentId]),
    'my_borrowed'           => $count("SELECT COUNT(*) FROM loans WHERE student_id = ? AND status = 'APPROVED'", [$studentId]),
    'available_internships' => $count("SELECT COUNT(*) FROM internships WHERE status = 'PUBLISHED'"),
    'my_applications'       => $count('SELECT COUNT(*) FROM applications WHERE student_id = ?', [$studentId]),
    'unread_notifications'  => $count('SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0', [$studentId]),
    'user'                  => public_user($student),
]);
