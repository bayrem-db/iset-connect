<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
require_admin($pdo);

$idRaw = raw_input('id');
if (!positive_int($idRaw)) {
    respond_error('A valid loan id is required.', 400, ['id' => 'Valid loan id is required.']);
}
$id = (int)$idRaw;

$pdo->beginTransaction();
try {
    $loanStmt = $pdo->prepare('SELECT id, student_id, material_id, quantity, status
                               FROM loans WHERE id = ? FOR UPDATE');
    $loanStmt->execute([$id]);
    $loan = $loanStmt->fetch();
    if (!$loan) {
        $pdo->rollBack();
        respond_error('Loan not found.', 404);
    }
    if ($loan['status'] === 'RETURNED') {
        $pdo->rollBack();
        respond_error('This loan has already been returned.', 400);
    }
    if ($loan['status'] !== 'APPROVED') {
        $pdo->rollBack();
        respond_error('Only approved loans can be marked as returned.', 400);
    }

    $materialStmt = $pdo->prepare('SELECT id, name, total_quantity, available_quantity
                                   FROM materials WHERE id = ? FOR UPDATE');
    $materialStmt->execute([(int)$loan['material_id']]);
    $material = $materialStmt->fetch();
    if (!$material) {
        $pdo->rollBack();
        respond_error('Material not found.', 404);
    }

    $quantity = (int)$loan['quantity'];
    $restock = $pdo->prepare('UPDATE materials
                              SET available_quantity = LEAST(total_quantity, available_quantity + ?)
                              WHERE id = ?');
    $restock->execute([$quantity, (int)$material['id']]);

    $return = $pdo->prepare("UPDATE loans
                             SET status = 'RETURNED', actual_return_date = NOW()
                             WHERE id = ?");
    $return->execute([$id]);

    notify_user(
        $pdo,
        (int)$loan['student_id'],
        'Loan returned',
        'Your loan for ' . $material['name'] . ' (quantity: ' . $quantity . ') has been marked as returned.',
        'LOAN_RETURNED'
    );

    $fetch = $pdo->prepare("SELECT l.id, l.student_id,
                                   CONCAT(u.first_name, ' ', u.last_name) AS student_name,
                                   l.material_id, m.name AS material_name, m.category AS material_category,
                                   l.quantity, l.request_date, l.borrowing_date, l.expected_return_date,
                                   l.actual_return_date, l.status
                            FROM loans l
                            JOIN users u ON u.id = l.student_id
                            JOIN materials m ON m.id = l.material_id
                            WHERE l.id = ?");
    $fetch->execute([$id]);
    $row = $fetch->fetch();

    $pdo->commit();
} catch (Throwable $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    throw $e;
}

$loanData = [
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

respond_ok(['loan' => $loanData], 'Loan marked as returned successfully.');
