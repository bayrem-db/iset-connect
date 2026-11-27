<?php
/** API root / health check. */
require __DIR__ . '/bootstrap.php';

if (($_SERVER['REQUEST_URI'] ?? '/') === '/backend/' || ($_SERVER['REQUEST_URI'] ?? '/') === '/backend') {
    respond_ok(['service' => 'ISET Connect REST API', 'version' => '1.0.0', 'status' => 'running']);
}
respond_ok(['service' => 'ISET Connect REST API', 'version' => '1.0.0', 'endpoints_base' => 'api/']);
