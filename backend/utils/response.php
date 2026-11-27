<?php
/** Centralized JSON response helpers. */
function json_response(bool $success, string $message, $data = null, int $code = 200, array $extra = []): void
{
    http_response_code($code);
    header('Content-Type: application/json; charset=utf-8');
    $payload = array_merge([
        'success' => $success,
        'message' => $message,
        'data'    => $data,
    ], $extra);
    echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function respond_ok($data = null, string $message = 'OK', int $code = 200): void
{
    json_response(true, $message, $data, $code);
}

function respond_created($data = null, string $message = 'Created successfully.'): void
{
    json_response(true, $message, $data, 201);
}

function respond_error(string $message, int $code = 400, array $errors = [], $data = null): void
{
    $extra = $errors ? ['errors' => $errors] : [];
    json_response(false, $message, $data, $code, $extra);
}

function require_method(string $expected): void
{
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== $expected) {
        respond_error('Method not allowed. Use ' . $expected . '.', 405);
    }
}
