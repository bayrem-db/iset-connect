<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('GET');
$user = require_auth($pdo);

$where = [];
$params = [];

if ($user['role'] !== 'ADMIN') {
    $where[] = 'l.student_id = ?';
    $params[] = (int)$user['id'];
}

$search = isset($_GET['q']) && is_string($_GET['q']) ? trim($_GET['q']) : '';
if ($search !== '') {
    $where[] = "(u.first_name LIKE ? OR u.last_name LIKE ?
                 OR CONCAT(u.first_name, ' ', u.last_name) LIKE ? OR m.name LIKE ?)";
    $term = '%' . $search . '%';
    $params[] = $term;
    $params[] = $term;
    $params[] = $term;
    $params[] = $term;
}

$status = isset($_GET['status']) && is_string($_GET['status']) ? strtoupper(trim($_GET['status'])) : '';
if (in_array($status, ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'RETURNED'], true)) {
    $where[] = 'l.status = ?';
    $params[] = $status;
}

$scope = isset($_GET['scope']) && is_string($_GET['scope']) ? strtolower(trim($_GET['scope'])) : '';
if ($scope === 'history') {
    $where[] = "l.status IN ('RETURNED', 'REJECTED', 'CANCELLED')";
}

$sql = "SELECT l.id, l.student_id,
               CONCAT(u.first_name, ' ', u.last_name) AS student_name,
               l.material_id, m.name AS material_name, m.category AS material_category,
               l.quantity, l.request_date, l.borrowing_date, l.expected_return_date,
               l.actual_return_date, l.status
        FROM loans l
        JOIN users u ON u.id = l.student_id
        JOIN materials m ON m.id = l.material_id";
if ($where !== []) {
    $sql .= ' WHERE ' . implode(' AND ', $where);
}
$sql .= ' ORDER BY l.request_date DESC, l.id DESC';

$stmt = $pdo->prepare($sql);
$stmt->execute($params);

$formatLoan = static function (array $row): array {
    return [
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
};

respond_ok(['loans' => array_map($formatLoan, $stmt->fetchAll())]);
