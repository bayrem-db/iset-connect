<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('GET');
$user = require_auth($pdo);

$q            = isset($_GET['q']) && is_scalar($_GET['q']) ? trim((string)$_GET['q']) : '';
$status       = isset($_GET['status']) && is_scalar($_GET['status']) ? strtoupper(trim((string)$_GET['status'])) : '';
$internshipId = isset($_GET['internship_id']) && is_scalar($_GET['internship_id']) ? trim((string)$_GET['internship_id']) : '';

if ($internshipId !== '' && !positive_int($internshipId)) {
    respond_error('A valid internship filter is required.', 400);
}

$sql = "SELECT a.id, a.student_id,
               CONCAT(u.first_name, ' ', u.last_name) AS student_name,
               u.cin, u.class_name,
               a.internship_id, i.title AS internship_title, i.company_name,
               a.motivation_message, a.application_date, a.status
        FROM applications a
        JOIN users u ON u.id = a.student_id
        JOIN internships i ON i.id = a.internship_id";

$where  = [];
$params = [];

if ($user['role'] !== 'ADMIN') {
    $where[]  = 'a.student_id = ?';
    $params[] = (int)$user['id'];
}
if ($status !== '') {
    $where[]  = 'a.status = ?';
    $params[] = $status;
}
if ($internshipId !== '') {
    $where[]  = 'a.internship_id = ?';
    $params[] = (int)$internshipId;
}
if ($q !== '') {
    $like    = '%' . mb_substr($q, 0, 200) . '%';
    $where[] = "(CONCAT(u.first_name, ' ', u.last_name) LIKE ? OR u.cin LIKE ?
                 OR i.title LIKE ? OR i.company_name LIKE ?)";
    $params[] = $like;
    $params[] = $like;
    $params[] = $like;
    $params[] = $like;
}
if ($where) {
    $sql .= ' WHERE ' . implode(' AND ', $where);
}
$sql .= ' ORDER BY a.application_date DESC, a.id DESC';

$stmt = $pdo->prepare($sql);
$stmt->execute($params);

$applications = [];
foreach ($stmt->fetchAll() as $row) {
    $row['id']            = (int)$row['id'];
    $row['student_id']    = (int)$row['student_id'];
    $row['internship_id'] = (int)$row['internship_id'];
    $applications[] = $row;
}

respond_ok(['applications' => $applications]);
