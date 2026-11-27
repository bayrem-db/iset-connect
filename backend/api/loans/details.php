<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('GET');
$user = require_auth($pdo);

$idRaw = isset($_GET['id']) && is_scalar($_GET['id']) ? (string)$_GET['id'] : '';
if (!positive_int($idRaw)) {
    respond_error('A valid loan id is required.', 400, ['id' => 'Valid loan id is required.']);
}

$stmt = $pdo->prepare("SELECT l.id, l.student_id,
                              CONCAT(u.first_name, ' ', u.last_name) AS student_name,
                              l.material_id, m.name AS material_name, m.category AS material_category,
                              l.quantity, l.request_date, l.borrowing_date, l.expected_return_date,
                              l.actual_return_date, l.status
                       FROM loans l
                       JOIN users u ON u.id = l.student_id
                       JOIN materials m ON m.id = l.material_id
                       WHERE l.id = ?");
$stmt->execute([(int)$idRaw]);
$row = $stmt->fetch();
if (!$row) {
    respond_error('Loan not found.', 404);
}

if ($user['role'] !== 'ADMIN' && (int)$row['student_id'] !== (int)$user['id']) {
    respond_error('You are not allowed to view this loan.', 403);
}

$loan = [
    'id'                   => (int)$row['id'],
    'student_id'           => (int)$row['student_id'],
    'student_name'         => (string)$row['student_name'],
    'material_id'          => (int)$row['material_id'],
    'material_name'        => (string)$row['material_name'],
    'material_category'    => (string)$row['material_category'],
    'quantity'             => (int)$row['quantity'],
    'request_date'         => $row['request_date'],
    'borrowing_date'       => $row['borrowing_date'],
    'expected_return_date' => $row['expected_return_date'],
    'actual_return_date'   => $row['actual_return_date'],
    'status'               => (string)$row['status'],
];

respond_ok(['loan' => $loan]);
