<?php
require_once __DIR__ . '/../common.php';

$username = strtolower(trim($_GET['u'] ?? $_GET['username'] ?? ''));

if (empty($username)) {
    fail('Username parameter required');
}

$conn = db();
$st = $conn->prepare('SELECT id FROM users WHERE username=?');
$st->execute([$username]);
$exists = $st->fetch() ? true : false;

out(['available' => !$exists]);
?>
