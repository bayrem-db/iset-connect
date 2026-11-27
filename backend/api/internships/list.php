<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('GET');
$user = require_auth($pdo);

$q      = isset($_GET['q']) && is_scalar($_GET['q']) ? trim((string)$_GET['q']) : '';
$domain = isset($_GET['domain']) && is_scalar($_GET['domain']) ? trim((string)$_GET['domain']) : '';
$status = isset($_GET['status']) && is_scalar($_GET['status']) ? strtoupper(trim((string)$_GET['status'])) : '';

$sql = "SELECT i.id, i.title, i.company_name, i.company_address, i.domain, i.description,
               i.required_skills, i.start_date, i.end_date, i.application_deadline,
               i.total_positions, i.available_positions, i.status, i.created_at,
               EXISTS (SELECT 1 FROM applications a
                       WHERE a.internship_id = i.id AND a.student_id = ?) AS has_applied
        FROM internships i";

$where  = [];
$params = [(int)$user['id']];

// Students may only ever see published offers, regardless of a supplied status filter.
if ($user['role'] !== 'ADMIN') {
    $where[] = "i.status = 'PUBLISHED'";
}

if ($q !== '') {
    $like    = '%' . mb_substr($q, 0, 200) . '%';
    $where[] = '(i.title LIKE ? OR i.company_name LIKE ?)';
    $params[] = $like;
    $params[] = $like;
}
if ($domain !== '') {
    $where[]  = 'i.domain = ?';
    $params[] = mb_substr($domain, 0, 100);
}
if ($status !== '') {
    $where[]  = 'i.status = ?';
    $params[] = $status;
}
if ($where) {
    $sql .= ' WHERE ' . implode(' AND ', $where);
}
$sql .= ' ORDER BY i.created_at DESC, i.id DESC';

$stmt = $pdo->prepare($sql);
$stmt->execute($params);

$internships = [];
foreach ($stmt->fetchAll() as $row) {
    $row['id']                 = (int)$row['id'];
    $row['total_positions']    = (int)$row['total_positions'];
    $row['available_positions'] = (int)$row['available_positions'];
    $row['has_applied']        = (int)$row['has_applied'];
    $internships[] = $row;
}

respond_ok(['internships' => $internships]);
