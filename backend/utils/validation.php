<?php
/** Input parsing, sanitization and validation helpers. */
$GLOBALS['iset_json_body'] = null;

function json_body(): array
{
    if ($GLOBALS['iset_json_body'] === null) {
        $raw = file_get_contents('php://input');
        $decoded = json_decode($raw, true);
        $GLOBALS['iset_json_body'] = is_array($decoded) ? $decoded : [];
    }
    return $GLOBALS['iset_json_body'];
}

/** Read a request field from POST data or a JSON body. */
function input(string $key, $default = null)
{
    if (isset($_POST[$key])) {
        return $_POST[$key];
    }
    $body = json_body();
    return array_key_exists($key, $body) ? $body[$key] : $default;
}

/** Trim + strip tags for plain text fields (never use on passwords). */
function clean(string $key, int $maxLength = 500): string
{
    $value = input($key, '');
    if (!is_scalar($value)) {
        return '';
    }
    $value = trim((string)$value);
    $value = strip_tags($value);
    $value = htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
    return mb_substr($value, 0, $maxLength);
}

function raw_input(string $key): string
{
    $value = input($key, '');
    return is_scalar($value) ? trim((string)$value) : '';
}

function is_blank(?string $value): bool
{
    return $value === null || trim($value) === '';
}

function valid_email(string $email): bool
{
    return filter_var($email, FILTER_VALIDATE_EMAIL) !== false && mb_strlen($email) <= 190;
}

function valid_date(string $date): bool
{
    $d = DateTime::createFromFormat('Y-m-d', $date);
    return $d !== false && $d->format('Y-m-d') === $date;
}

function valid_password(string $password): bool
{
    return mb_strlen($password) >= 8 && mb_strlen($password) <= 128;
}

function positive_int($value): bool
{
    return filter_var($value, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]) !== false;
}

function require_fields(array $fields, array &$errors): void
{
    foreach ($fields as $field => $label) {
        if (is_blank(raw_input($field))) {
            $errors[$field] = $label . ' is required.';
        }
    }
}
