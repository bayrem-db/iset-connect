<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('GET');
require_admin($pdo);

// Let the shared input() helpers also read parameters passed in the query string.
$_POST = $_POST + $_GET;

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

$stmt = $pdo->prepare('SELECT l.id, l.student_id, u.first_name, u.last_name, l.material_id,
                              m.name AS material_name, m.category AS material_category, l.quantity,
                              l.request_date, l.borrowing_date, l.expected_return_date, l.actual_return_date, l.status
                       FROM loans l
                       JOIN users u ON u.id = l.student_id
                       JOIN materials m ON m.id = l.material_id
                       WHERE l.student_id = ?
                       ORDER BY l.request_date DESC, l.id DESC');
$stmt->execute([(int)$id]);

$loans = [];
foreach ($stmt->fetchAll() as $row) {
    $loans[] = [
        'id'                   => (int)$row['id'],
        'student_id'           => (int)$row['student_id'],
        'student_name'         => trim($row['first_name'] . ' ' . $row['last_name']),
        'material_id'          => (int)$row['material_id'],
        'material_name'        => $row['material_name'],
        'material_category'    => $row['material_category'],
        'quantity'             => (int)$row['quantity'],
        'request_date'         => $row['request_date'],
        'borrowing_date'       => $row['borrowing_date'],
        'expected_return_date' => $row['expected_return_date'],
        'actual_return_date'   => $row['actual_return_date'],
        'status'               => $row['status'],
    ];
}

$stmt = $pdo->prepare('SELECT a.id, a.student_id, u.first_name, u.last_name, u.cin, u.class_name,
                              a.internship_id, i.title AS internship_title, i.company_name,
                              a.motivation_message, a.application_date, a.status
                       FROM applications a
                       JOIN users u ON u.id = a.student_id
                       JOIN internships i ON i.id = a.internship_id
                       WHERE a.student_id = ?
                       ORDER BY a.application_date DESC, a.id DESC');
$stmt->execute([(int)$id]);

$applications = [];
foreach ($stmt->fetchAll() as $row) {
    $applications[] = [
        'id'                 => (int)$row['id'],
        'student_id'         => (int)$row['student_id'],
        'student_name'       => trim($row['first_name'] . ' ' . $row['last_name']),
        'cin'                => $row['cin'],
        'class_name'         => $row['class_name'],
        'internship_id'      => (int)$row['internship_id'],
        'internship_title'   => $row['internship_title'],
        'company_name'       => $row['company_name'],
        'motivation_message' => $row['motivation_message'],
        'application_date'   => $row['application_date'],
        'status'             => $row['status'],
    ];
}

respond_ok([
    'student'      => public_user($student),
    'loans'        => $loans,
    'applications' => $applications,
]);
