<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('GET');
require_admin($pdo);

// Let the shared input() helpers also read parameters passed in the query string.
$_POST = $_POST + $_GET;

$q      = raw_input('q');
$status = raw_input('status');

$where = ["role = 'STUDENT'"];
$params = [];

if ($q !== '') {
    $escaped = str_replace(['!', '%', '_'], ['!!', '!%', '!_'], $q);
    $like = '%' . $escaped . '%';
    $where[] = "(first_name LIKE ? ESCAPE '!' OR last_name LIKE ? ESCAPE '!' OR cin LIKE ? ESCAPE '!' OR email LIKE ? ESCAPE '!')";
    $params = array_merge($params, [$like, $like, $like, $like]);
}

if ($status !== '') {
    $allowed = ['PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'];
    if (!in_array($status, $allowed, true)) {
        respond_error('Invalid status filter.', 400, ['status' => 'Status must be PENDING, APPROVED, REJECTED or SUSPENDED.']);
    }
    $where[] = 'status = ?';
    $params[] = $status;
}

$sql = 'SELECT id, first_name, last_name, cin, email, class_name, role, status, created_at
        FROM users WHERE ' . implode(' AND ', $where) . '
        ORDER BY created_at DESC, id DESC';

$stmt = $pdo->prepare($sql);
$stmt->execute($params);

$students = [];
foreach ($stmt->fetchAll() as $row) {
    $students[] = public_user($row);
}

respond_ok(['students' => $students]);
