<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
$user = require_student($pdo);

$errors = [];

$internshipId = input('internship_id', null);
$motivation   = clean('motivation_message', 5000);

if (!positive_int($internshipId)) {
    $errors['internship_id'] = 'A valid internship id is required.';
}
if (is_blank($motivation)) {
    $errors['motivation_message'] = 'A motivation message is required.';
}
if ($errors) {
    respond_error('Please correct the highlighted fields.', 400, $errors);
}

$internshipId = (int)$internshipId;

$stmt = $pdo->prepare('SELECT id, status, application_deadline, title, company_name
                       FROM internships WHERE id = ?');
$stmt->execute([$internshipId]);
$internship = $stmt->fetch();
if (!$internship) {
    respond_error('Internship not found.', 404);
}
if ($internship['status'] !== 'PUBLISHED') {
    respond_error('This internship is not open for applications.', 400);
}
if ($internship['application_deadline'] < date('Y-m-d')) {
    respond_error('The application deadline for this internship has passed.', 400);
}

$stmt = $pdo->prepare('SELECT id FROM applications WHERE student_id = ? AND internship_id = ?');
$stmt->execute([(int)$user['id'], $internshipId]);
if ($stmt->fetch()) {
    respond_error('You have already applied for this internship.', 400);
}

try {
    $stmt = $pdo->prepare("INSERT INTO applications
        (student_id, internship_id, motivation_message, application_date, status)
        VALUES (?, ?, ?, NOW(), 'PENDING')");
    $stmt->execute([(int)$user['id'], $internshipId, $motivation]);
    $id = (int)$pdo->lastInsertId();
} catch (PDOException $e) {
    if ($e->getCode() === '23000') {
        respond_error('You have already applied for this internship.', 400);
    }
    throw $e;
}

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

respond_created(['application' => $row], 'Application submitted successfully.');
