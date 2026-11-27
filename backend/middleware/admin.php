<?php
/** Alias kept for the documented middleware structure; authorization lives in auth.php. */
if (!function_exists('admin_guard')) {
    function admin_guard(PDO $pdo): array
    {
        return require_admin($pdo);
    }
}
