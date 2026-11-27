<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
require_admin($pdo);

$id = input('id', null);
if (!positive_int($id)) {
    respond_error('A valid internship id is required.', 400);
}
$id = (int)$id;

$stmt = $pdo->prepare('SELECT id FROM internships WHERE id = ?');
$stmt->execute([$id]);
if (!$stmt->fetch()) {
    respond_error('Internship not found.', 404);
}

$stmt = $pdo->prepare('SELECT COUNT(*) FROM applications WHERE internship_id = ?');
$stmt->execute([$id]);
if ((int)$stmt->fetchColumn() > 0) {
    respond_error('This internship cannot be deleted because applications exist for it.', 400);
}

$stmt = $pdo->prepare('DELETE FROM internships WHERE id = ?');
$stmt->execute([$id]);

respond_ok(null, 'Internship deleted successfully.');
