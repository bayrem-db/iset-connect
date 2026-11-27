<?php
/**
 * PDO database connection for ISET Connect.
 * Credentials can be overridden with environment variables in production.
 */
declare(strict_types=1);

$dbHost = getenv('ISET_DB_HOST') ?: '127.0.0.1';
$dbName = getenv('ISET_DB_NAME') ?: 'iset_connect';
$dbUser = getenv('ISET_DB_USER') ?: 'root';
$dbPass = getenv('ISET_DB_PASS');
if ($dbPass === false) {
    $dbPass = ''; // default XAMPP installation has an empty root password
}

$dsn = "mysql:host={$dbHost};dbname={$dbName};charset=utf8mb4";

try {
    $pdo = new PDO($dsn, $dbUser, $dbPass, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
        PDO::MYSQL_ATTR_INIT_COMMAND => "SET NAMES utf8mb4",
    ]);
} catch (PDOException $e) {
    error_log('[ISET Connect] DB connection failed: ' . $e->getMessage());
    http_response_code(500);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode([
        'success' => false,
        'message' => 'Database connection is currently unavailable.',
        'data'    => null,
    ]);
    exit;
}
