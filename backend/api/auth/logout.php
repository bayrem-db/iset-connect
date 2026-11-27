<?php
require dirname(__DIR__, 2) . '/bootstrap.php';

require_method('POST');

require_auth($pdo);
revoke_current_token($pdo);

respond_ok(null, 'Logged out successfully.');
