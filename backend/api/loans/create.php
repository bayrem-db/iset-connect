<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
$user = require_student($pdo);

$errors = [];
$materialIdRaw = raw_input('material_id');
$quantityRaw = raw_input('quantity');
$borrowingDate = raw_input('borrowing_date');
$expectedReturnDate = raw_input('expected_return_date');

if (!positive_int($materialIdRaw)) {
    $errors['material_id'] = 'A valid material id is required.';
}
if (!positive_int($quantityRaw)
    || filter_var($quantityRaw, FILTER_VALIDATE_INT, ['options' => ['max_range' => 4294967295]]) === false) {
    $errors['quantity'] = 'Quantity must be a whole number of one or more.';
}
if (!valid_date($borrowingDate)) {
    $errors['borrowing_date'] = 'Borrowing date must be in YYYY-MM-DD format.';
}
if (!valid_date($expectedReturnDate)) {
    $errors['expected_return_date'] = 'Expected return date must be in YYYY-MM-DD format.';
}

if ($errors !== []) {
    respond_error('Please correct the highlighted fields.', 400, $errors);
}

$quantity = (int)$quantityRaw;
if (strcmp($expectedReturnDate, $borrowingDate) <= 0) {
    respond_error('Expected return date must be after the borrowing date.', 400, [
        'expected_return_date' => 'Expected return date must be after the borrowing date.',
    ]);
}
if (strcmp($borrowingDate, date('Y-m-d')) < 0) {
    respond_error('Borrowing date cannot be in the past.', 400, [
        'borrowing_date' => 'Borrowing date must be today or later.',
    ]);
}

$materialStmt = $pdo->prepare('SELECT id, available_quantity FROM materials WHERE id = ?');
$materialStmt->execute([(int)$materialIdRaw]);
$material = $materialStmt->fetch();
if (!$material) {
    respond_error('Material not found.', 404);
}

$available = (int)$material['available_quantity'];
if ($available < $quantity) {
    respond_error('Only ' . $available . ' unit(s) are currently available.', 400, [
        'quantity' => 'Requested quantity exceeds the available stock.',
    ]);
}

// Inventory is only reserved when an administrator approves the request.
$insert = $pdo->prepare("INSERT INTO loans
    (student_id, material_id, quantity, request_date, borrowing_date, expected_return_date, status)
    VALUES (?, ?, ?, NOW(), ?, ?, 'PENDING')");
$insert->execute([
    (int)$user['id'],
    (int)$materialIdRaw,
    $quantity,
    $borrowingDate,
    $expectedReturnDate,
]);
$loanId = (int)$pdo->lastInsertId();

$fetch = $pdo->prepare("SELECT l.id, l.student_id,
                               CONCAT(u.first_name, ' ', u.last_name) AS student_name,
                               l.material_id, m.name AS material_name, m.category AS material_category,
                               l.quantity, l.request_date, l.borrowing_date, l.expected_return_date,
                               l.actual_return_date, l.status
                        FROM loans l
                        JOIN users u ON u.id = l.student_id
                        JOIN materials m ON m.id = l.material_id
                        WHERE l.id = ?");
$fetch->execute([$loanId]);
$row = $fetch->fetch();

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

respond_created(['loan' => $loan], 'Loan request submitted successfully.');
