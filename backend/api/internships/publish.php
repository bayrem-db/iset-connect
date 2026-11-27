<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
$user = require_admin($pdo);

$id = input('id', null);
if (!positive_int($id)) {
    respond_error('A valid internship id is required.', 400);
}
$id = (int)$id;

$stmt = $pdo->prepare('SELECT status, title, company_name FROM internships WHERE id = ?');
$stmt->execute([$id]);
$internship = $stmt->fetch();
if (!$internship) {
    respond_error('Internship not found.', 404);
}

$wasPublished = $internship['status'] === 'PUBLISHED';

$stmt = $pdo->prepare("UPDATE internships SET status = 'PUBLISHED' WHERE id = ?");
$stmt->execute([$id]);

if (!$wasPublished) {
    notify_all_students(
        $pdo,
        'New internship offer',
        'A new internship offer is available: ' . $internship['title'] . ' at ' . $internship['company_name'] . '.',
        'INTERNSHIP_PUBLISHED'
    );
}

$stmt = $pdo->prepare("SELECT i.id, i.title, i.company_name, i.company_address, i.domain, i.description,
                              i.required_skills, i.start_date, i.end_date, i.application_deadline,
                              i.total_positions, i.available_positions, i.status, i.created_at,
                              EXISTS (SELECT 1 FROM applications a
                                      WHERE a.internship_id = i.id AND a.student_id = ?) AS has_applied
                       FROM internships i WHERE i.id = ?");
$stmt->execute([(int)$user['id'], $id]);
$row = $stmt->fetch();
$row['id']                  = (int)$row['id'];
$row['total_positions']     = (int)$row['total_positions'];
$row['available_positions'] = (int)$row['available_positions'];
$row['has_applied']         = (int)$row['has_applied'];

respond_ok(['internship' => $row], 'Internship published successfully.');
