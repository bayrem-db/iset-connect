<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
require_admin($pdo);

$idRaw = raw_input('id');
if (!positive_int($idRaw)) {
    respond_error('A valid loan id is required.', 400, ['id' => 'Valid loan id is required.']);
}
$id = (int)$idRaw;
$reason = clean('reason', 500);

$pdo->beginTransaction();
try {
    $loanStmt = $pdo->prepare('SELECT l.id, l.student_id, l.quantity, l.status,
                                      m.name AS material_name
                               FROM loans l
                               JOIN materials m ON m.id = l.material_id
                               WHERE l.id = ? FOR UPDATE');
    $loanStmt->execute([$id]);
    $loan = $loanStmt->fetch();
    if (!$loan) {
        $pdo->rollBack();
        respond_error('Loan not found.', 404);
    }
    if ($loan['status'] !== 'PENDING') {
        $pdo->rollBack();
        respond_error('Only pending loan requests can be rejected.', 400);
    }

    $reject = $pdo->prepare("UPDATE loans SET status = 'REJECTED' WHERE id = ?");
    $reject->execute([$id]);

    $message = 'Your request for ' . $loan['material_name']
        . ' (quantity: ' . (int)$loan['quantity'] . ') has been rejected.';
    if ($reason !== '') {
        $message .= ' Reason: ' . $reason;
    }
    notify_user($pdo, (int)$loan['student_id'], 'Loan request rejected', $message, 'LOAN_REJECTED');

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

respond_ok(['loan' => $loanData], 'Loan request rejected.');
