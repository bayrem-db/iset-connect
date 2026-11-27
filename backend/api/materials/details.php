<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('GET');
require_auth($pdo);

$idRaw = isset($_GET['id']) && is_scalar($_GET['id']) ? (string)$_GET['id'] : '';
if (!positive_int($idRaw)) {
    respond_error('A valid material id is required.', 400, ['id' => 'Valid material id is required.']);
}

$stmt = $pdo->prepare('SELECT id, name, description, category, total_quantity, available_quantity,
                              image_url, created_at, updated_at
                       FROM materials WHERE id = ?');
$stmt->execute([(int)$idRaw]);
$row = $stmt->fetch();
if (!$row) {
    respond_error('Material not found.', 404);
}

$material = [
    'id'                 => (int)$row['id'],
    'name'               => (string)$row['name'],
    'description'        => (string)$row['description'],
    'category'           => (string)$row['category'],
    'total_quantity'     => (int)$row['total_quantity'],
    'available_quantity' => (int)$row['available_quantity'],
    'image_url'          => $row['image_url'] === null ? null : (string)$row['image_url'],
    'created_at'         => $row['created_at'],
    'updated_at'         => $row['updated_at'],
];

respond_ok(['material' => $material]);
