<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
require_admin($pdo);

$id = input('id', null);
if (!positive_int($id)) {
    respond_error('A valid application id is required.', 400);
}
$id = (int)$id;

$internshipId    = 0;
$internshipTitle = '';
$companyName     = '';

$pdo->beginTransaction();
try {
    $stmt = $pdo->prepare('SELECT id, student_id, internship_id, status
                           FROM applications WHERE id = ?');
    $stmt->execute([$id]);
    $application = $stmt->fetch();
    if (!$application) {
        $pdo->rollBack();
        respond_error('Application not found.', 404);
    }
    if ($application['status'] !== 'PENDING') {
        $pdo->rollBack();
        respond_error('Only pending applications can be accepted.', 400);
    }
    $internshipId = (int)$application['internship_id'];
    $studentId    = (int)$application['student_id'];

    // Lock the internship row so two concurrent acceptances cannot oversell positions.
    $stmt = $pdo->prepare('SELECT title, company_name, available_positions
                           FROM internships WHERE id = ? FOR UPDATE');
    $stmt->execute([$internshipId]);
    $internship = $stmt->fetch();
    if (!$internship) {
        $pdo->rollBack();
        respond_error('Internship not found.', 404);
    }
    if ((int)$internship['available_positions'] <= 0) {
        $pdo->rollBack();
        respond_error('No positions remain for this internship.', 400);
    }
    $internshipTitle = $internship['title'];
    $companyName     = $internship['company_name'];

    $stmt = $pdo->prepare('UPDATE internships
                           SET available_positions = available_positions - 1
                           WHERE id = ? AND available_positions > 0');
    $stmt->execute([$internshipId]);
    if ($stmt->rowCount() !== 1) {
        $pdo->rollBack();
        respond_error('No positions remain for this internship.', 400);
    }

    $stmt = $pdo->prepare("UPDATE applications SET status = 'ACCEPTED'
                           WHERE id = ? AND status = 'PENDING'");
    $stmt->execute([$id]);
    if ($stmt->rowCount() !== 1) {
        $pdo->rollBack();
        respond_error('Only pending applications can be accepted.', 400);
    }

    $pdo->commit();
} catch (PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    throw $e;
}

notify_user(
    $pdo,
    $studentId,
    'Application accepted',
    'Your application for ' . $internshipTitle . ' at ' . $companyName . ' has been accepted.',
    'APPLICATION_ACCEPTED'
);

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

respond_ok(['application' => $row], 'Application accepted successfully.');
