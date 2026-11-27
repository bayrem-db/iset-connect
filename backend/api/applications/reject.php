<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
require_admin($pdo);

$id = input('id', null);
if (!positive_int($id)) {
    respond_error('A valid application id is required.', 400);
}
$id = (int)$id;

$reason = clean('reason', 500);

$stmt = $pdo->prepare("SELECT a.student_id, a.status, i.title AS internship_title, i.company_name
                       FROM applications a
                       JOIN internships i ON i.id = a.internship_id
                       WHERE a.id = ?");
$stmt->execute([$id]);
$application = $stmt->fetch();
if (!$application) {
    respond_error('Application not found.', 404);
}
if ($application['status'] !== 'PENDING') {
    respond_error('Only pending applications can be rejected.', 400);
}

$stmt = $pdo->prepare("UPDATE applications SET status = 'REJECTED' WHERE id = ? AND status = 'PENDING'");
$stmt->execute([$id]);
if ($stmt->rowCount() !== 1) {
    respond_error('Only pending applications can be rejected.', 400);
}

$message = 'Your application for ' . $application['internship_title'] . ' at '
    . $application['company_name'] . ' has been rejected.';
if (!is_blank($reason)) {
    $message .= ' Reason: ' . $reason;
}
notify_user($pdo, (int)$application['student_id'], 'Application rejected', $message, 'APPLICATION_REJECTED');

$stmt = $pdo->prepare("SELECT a.id, a.student_id,
                              CONCAT(u.first_name, ' ', u.last_name) AS student_name,
                              u.cin, u.class_name,
                              a.internship_id, i.title AS internship_title, i.company_name,
                              a.motivation_message, a.application_date, a.status
                       FROM applications a
                       JOIN users u ON u.id = a.student_id
                       JOIN internships i ON i.id = a.internship_id
                       WHERE a.id = ?");
$stmt->execute([$id]);
$row = $stmt->fetch();
$row['id']            = (int)$row['id'];
$row['student_id']    = (int)$row['student_id'];
$row['internship_id'] = (int)$row['internship_id'];

respond_ok(['application' => $row], 'Application rejected successfully.');
