<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
$user = require_admin($pdo);

$errors = [];

$id             = input('id', null);
$title          = clean('title', 190);
$companyName    = clean('company_name', 190);
$companyAddress = clean('company_address', 240);
$domain         = clean('domain', 90);
$description    = clean('description', 5000);
$requiredSkills = clean('required_skills', 5000);
$startDate      = raw_input('start_date');
$endDate        = raw_input('end_date');
$deadline       = raw_input('application_deadline');
$totalRaw       = input('total_positions', null);
$status         = raw_input('status');

if (!positive_int($id))        { $errors['id'] = 'A valid internship id is required.'; }
if (is_blank($title))          { $errors['title'] = 'Title is required.'; }
if (is_blank($companyName))    { $errors['company_name'] = 'Company name is required.'; }
if (is_blank($domain))         { $errors['domain'] = 'Domain is required.'; }
if (is_blank($description))    { $errors['description'] = 'Description is required.'; }
if (is_blank($requiredSkills)) { $errors['required_skills'] = 'Required skills are required.'; }
if (!valid_date($startDate))   { $errors['start_date'] = 'Start date must be a valid date (YYYY-MM-DD).'; }
if (!valid_date($endDate))     { $errors['end_date'] = 'End date must be a valid date (YYYY-MM-DD).'; }
if (!valid_date($deadline))    { $errors['application_deadline'] = 'Application deadline must be a valid date (YYYY-MM-DD).'; }
if (!positive_int($totalRaw))  { $errors['total_positions'] = 'Total positions must be at least 1.'; }

if ($status === '') {
    $status = 'DRAFT';
}
if (!in_array($status, ['DRAFT', 'PUBLISHED', 'CLOSED', 'EXPIRED'], true)) {
    $errors['status'] = 'Status must be DRAFT, PUBLISHED, CLOSED or EXPIRED.';
}

if (valid_date($startDate) && valid_date($endDate) && $endDate < $startDate) {
    $errors['end_date'] = 'End date cannot be earlier than the start date.';
}
if (valid_date($endDate) && valid_date($deadline) && $deadline > $endDate) {
    $errors['application_deadline'] = 'Application deadline cannot be after the end date.';
}

if ($errors) {
    respond_error('Please correct the highlighted fields.', 400, $errors);
}

$id    = (int)$id;
$total = (int)$totalRaw;
$newStatus = $status;
$previousStatus = null;

$pdo->beginTransaction();
try {
    $stmt = $pdo->prepare('SELECT total_positions, available_positions, status
                           FROM internships WHERE id = ? FOR UPDATE');
    $stmt->execute([$id]);
    $current = $stmt->fetch();
    if (!$current) {
        $pdo->rollBack();
        respond_error('Internship not found.', 404);
    }
    $previousStatus = $current['status'];

    $stmt = $pdo->prepare("SELECT COUNT(*) FROM applications
                           WHERE internship_id = ? AND status = 'ACCEPTED'");
    $stmt->execute([$id]);
    $accepted = (int)$stmt->fetchColumn();
    if ($total < $accepted) {
        $pdo->rollBack();
        respond_error(
            'Total positions cannot be lower than the number of accepted applications (' . $accepted . ').',
            400
        );
    }

    $oldTotal = (int)$current['total_positions'];
    $oldAvailable = (int)$current['available_positions'];
    $available = $oldAvailable + ($total - $oldTotal);
    $available = max(0, min($total, $available));

    $stmt = $pdo->prepare("UPDATE internships SET
                              title = ?, company_name = ?, company_address = ?, domain = ?,
                              description = ?, required_skills = ?, start_date = ?, end_date = ?,
                              application_deadline = ?, total_positions = ?, available_positions = ?,
                              status = ?
                           WHERE id = ?");
    $stmt->execute([
        $title, $companyName, $companyAddress, $domain, $description, $requiredSkills,
        $startDate, $endDate, $deadline, $total, $available, $status, $id,
    ]);

    $pdo->commit();
} catch (PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    throw $e;
}

if ($newStatus === 'PUBLISHED' && $previousStatus !== 'PUBLISHED') {
    notify_all_students(
        $pdo,
        'New internship offer',
        'A new internship offer is available: ' . $title . ' at ' . $companyName . '.',
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

respond_ok(['internship' => $row], 'Internship updated successfully.');
