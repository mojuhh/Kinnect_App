<?php
require_once __DIR__ . '/config.php';
header('Content-Type: application/json; charset=utf-8');
date_default_timezone_set('UTC');

function db() {
    static $pdo;
    if (!$pdo) {
        try {
            $pdo = new PDO('mysql:host='.DB_HOST.';dbname='.DB_NAME.';charset=utf8mb4',
                DB_USER, DB_PASS, [
                    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
                ]);
            $pdo->exec("SET time_zone = '+00:00'");
        } catch (PDOException $e) {
            fail('Database connection failed: ' . $e->getMessage(), 500);
        }
    }
    return $pdo;
}

function out($data = [], $code = 200) {
    http_response_code($code);
    echo json_encode(['ok' => true, 'data' => $data]);
    exit;
}

function fail($msg, $code = 400) {
    http_response_code($code);
    echo json_encode(['ok' => false, 'error' => $msg]);
    exit;
}

function body() {
    $j = json_decode(file_get_contents('php://input'), true);
    return is_array($j) ? array_merge($_POST, $j) : $_POST;
}

function auth() {
    $h = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';
    if (!preg_match('/Bearer\s+([a-f0-9]{64})/', $h, $m)) {
        fail('unauthorized', 401);
    }
    $st = db()->prepare('SELECT user_id FROM sessions WHERE token=?');
    $st->execute([$m[1]]);
    $row = $st->fetch();
    if (!$row) {
        fail('unauthorized', 401);
    }
    db()->prepare('UPDATE users SET last_seen=UTC_TIMESTAMP() WHERE id=?')
        ->execute([$row['user_id']]);
    return (int)$row['user_id'];
}

function save_upload($field, $subdir, $allowed = ['jpg', 'jpeg', 'png', 'mp4', 'pdf', 'doc', 'docx']) {
    if (empty($_FILES[$field]) || $_FILES[$field]['error'] !== UPLOAD_ERR_OK) {
        return null;
    }
    $f = $_FILES[$field];
    $ext = strtolower(pathinfo($f['name'], PATHINFO_EXTENSION));
    if (!in_array($ext, $allowed)) {
        fail('file type not allowed');
    }
    if ($f['size'] > 50 * 1024 * 1024) {
        fail('file too large');
    }
    $dir = UPLOAD_DIR . $subdir . '/';
    if (!is_dir($dir)) {
        mkdir($dir, 0775, true);
    }
    $name = bin2hex(random_bytes(12)) . '.' . $ext;
    move_uploaded_file($f['tmp_name'], $dir . $name);
    return 'uploads/' . $subdir . '/' . $name;
}
?>
