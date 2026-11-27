<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('GET');
$user = require_auth($pdo);

$id = isset($_GET['id']) ? $_GET['id'] : null;
if (!positive_int($id)) {
    respond_error('A valid internship id is required.', 400);
}

$sql = "SELECT i.id, i.title, i.company_name, i.company_address, i.domain, i.description,
               i.required_skills, i.start_date, i.end_date, i.application_deadline,
               i.total_positions, i.available_positions, i.status, i.created_at,
               EXISTS (SELECT 1 FROM applications a
                       WHERE a.internship_id = i.id AND a.student_id = ?) AS has_applied
        FROM internships i
        WHERE i.id = ?";

$params = [(int)$user['id'], (int)$id];
if ($user['role'] !== 'ADMIN') {
    $sql .= " AND i.status = 'PUBLISHED'";
}

$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$row = $stmt->fetch();
if (!$row) {
    respond_error('Internship not found.', 404);
}

$row['id']                  = (int)$row['id'];
$row['total_positions']     = (int)$row['total_positions'];
$row['available_positions'] = (int)$row['available_positions'];
$row['has_applied']         = (int)$row['has_applied'];

respond_ok(['internship' => $row]);
