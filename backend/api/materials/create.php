<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
require_admin($pdo);

$errors = [];

$name = clean('name', 150);
$description = clean('description', 5000);
$category = clean('category', 100);
$totalRaw = raw_input('total_quantity');
$totalQuantity = filter_var($totalRaw, FILTER_VALIDATE_INT, [
    'options' => ['min_range' => 0, 'max_range' => 4294967295],
]);

$imageUrl = raw_input('image_url');
$imageUrl = $imageUrl === '' ? null : mb_substr($imageUrl, 0, 255);

if ($name === '') {
    $errors['name'] = 'Name is required.';
}
if ($description === '') {
    $errors['description'] = 'Description is required.';
}
if ($category === '') {
    $errors['category'] = 'Category is required.';
}
if ($totalQuantity === false) {
    $errors['total_quantity'] = 'Total quantity must be a whole number of zero or more.';
}

if ($errors !== []) {
    respond_error('Please correct the highlighted fields.', 400, $errors);
}

$stmt = $pdo->prepare('INSERT INTO materials
                       (name, description, category, total_quantity, available_quantity, image_url)
                       VALUES (?, ?, ?, ?, ?, ?)');
$stmt->execute([
    $name,
    $description,
    $category,
    $totalQuantity,
    $totalQuantity,
    $imageUrl,
]);
$id = (int)$pdo->lastInsertId();

$fetch = $pdo->prepare('SELECT id, name, description, category, total_quantity, available_quantity,
                               image_url, created_at, updated_at
                        FROM materials WHERE id = ?');
$fetch->execute([$id]);
$row = $fetch->fetch();

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

respond_created(['material' => $material], 'Material created successfully.');
