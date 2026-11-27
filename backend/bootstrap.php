<?php
/**
 * Single entry point for every API endpoint.
 * Usage in api/<group>/<file>.php:
 *   require dirname(__DIR__, 2) . '/bootstrap.php';
 */
declare(strict_types=1);

error_reporting(E_ALL);
ini_set('display_errors', '0');
ini_set('log_errors', '1');

require_once __DIR__ . '/config/cors.php';
require_once __DIR__ . '/utils/response.php';
require_once __DIR__ . '/utils/validation.php';
require_once __DIR__ . '/utils/notifications.php';
require_once __DIR__ . '/middleware/auth.php';
require_once __DIR__ . '/middleware/admin.php';
require_once __DIR__ . '/config/database.php';

set_exception_handler(function (Throwable $e): void {
    error_log('[ISET Connect] Unhandled exception: ' . $e->getMessage() . ' in ' . $e->getFile() . ':' . $e->getLine());
    if (!headers_sent()) {
        json_response(false, 'An internal server error occurred.', null, 500);
    }
});
