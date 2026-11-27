<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');
require_admin($pdo);

$idRaw = raw_input('id');
if (!positive_int($idRaw)) {
    respond_error('A valid material id is required.', 400, ['id' => 'Valid material id is required.']);
}
$id = (int)$idRaw;

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

$pdo->beginTransaction();
try {
    $lock = $pdo->prepare('SELECT id, total_quantity, available_quantity
                           FROM materials WHERE id = ? FOR UPDATE');
    $lock->execute([$id]);
    $current = $lock->fetch();
    if (!$current) {
        $pdo->rollBack();
        respond_error('Material not found.', 404);
    }

    // Units no longer available are committed to loans and cannot disappear.
    $loanedUnits = max(0, (int)$current['total_quantity'] - (int)$current['available_quantity']);
    if ($totalQuantity < $loanedUnits) {
        $pdo->rollBack();
        respond_error(
            'Total quantity cannot be lower than the ' . $loanedUnits . ' unit(s) currently on loan.',
            400,
            ['total_quantity' => 'Total quantity must be at least ' . $loanedUnits . '.']
        );
    }
    $availableQuantity = $totalQuantity - $loanedUnits;

    $update = $pdo->prepare('UPDATE materials
                             SET name = ?, description = ?, category = ?, total_quantity = ?,
                                 available_quantity = ?, image_url = ?
                             WHERE id = ?');
    $update->execute([
        $name,
        $description,
        $category,
        $totalQuantity,
        $availableQuantity,
        $imageUrl,
        $id,
    ]);

    $fetch = $pdo->prepare('SELECT id, name, description, category, total_quantity, available_quantity,
                                   image_url, created_at, updated_at
                            FROM materials WHERE id = ?');
    $fetch->execute([$id]);
    $row = $fetch->fetch();

    $pdo->commit();
} catch (Throwable $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    throw $e;
}

respond_ok(['material' => $formatMaterial($row)], 'Material updated successfully.');
