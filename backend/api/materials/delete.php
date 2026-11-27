<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
require_admin($pdo);

$idRaw = raw_input('id');
if (!positive_int($idRaw)) {
    respond_error('A valid material id is required.', 400, ['id' => 'Valid material id is required.']);
}
$id = (int)$idRaw;

$pdo->beginTransaction();
try {
    $lock = $pdo->prepare('SELECT id FROM materials WHERE id = ? FOR UPDATE');
    $lock->execute([$id]);
    if (!$lock->fetch()) {
        $pdo->rollBack();
        respond_error('Material not found.', 404);
    }

    $check = $pdo->prepare('SELECT COUNT(*) FROM loans WHERE material_id = ?');
    $check->execute([$id]);
    if ((int)$check->fetchColumn() > 0) {
        $pdo->rollBack();
        respond_error('This material cannot be deleted because loan records reference it.', 400);
    }

    $delete = $pdo->prepare('DELETE FROM materials WHERE id = ?');
    $delete->execute([$id]);
    $pdo->commit();
} catch (Throwable $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    throw $e;
}

respond_ok(null, 'Material deleted successfully.');
