<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('GET');
$user = require_auth($pdo);

$where = [];
$params = [];

// Students may only browse materials that are currently in stock.
if ($user['role'] !== 'ADMIN') {
    $where[] = 'available_quantity > 0';
}

$search = isset($_GET['q']) && is_string($_GET['q']) ? trim($_GET['q']) : '';
if ($search !== '') {
    $where[] = '(name LIKE ? OR description LIKE ? OR category LIKE ?)';
    $term = '%' . $search . '%';
    $params[] = $term;
    $params[] = $term;
    $params[] = $term;
}

$category = isset($_GET['category']) && is_string($_GET['category']) ? trim($_GET['category']) : '';
if ($category !== '') {
    $where[] = 'category = ?';
    $params[] = $category;
}

$sql = 'SELECT id, name, description, category, total_quantity, available_quantity,
               image_url, created_at, updated_at
        FROM materials';
if ($where !== []) {
    $sql .= ' WHERE ' . implode(' AND ', $where);
}
$sql .= ' ORDER BY id DESC';

$stmt = $pdo->prepare($sql);
$stmt->execute($params);

$formatMaterial = static function (array $row): array {
    return [
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
};

respond_ok(['materials' => array_map($formatMaterial, $stmt->fetchAll())]);
