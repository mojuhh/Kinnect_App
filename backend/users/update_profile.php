<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$name = trim($in['name'] ?? '');
$username = strtolower(trim($in['username'] ?? ''));
$bio = trim($in['bio'] ?? '');

$conn = db();

if (!empty($username)) {
    $st = $conn->prepare('SELECT id FROM users WHERE username=? AND id!=?');
    $st->execute([$username, $uid]);
    if ($st->fetch()) {
        fail('Username is already taken');
    }
}

$profilePicPath = save_upload('profile_pic', 'profiles', ['jpg', 'jpeg', 'png']);
$coverPicPath = save_upload('cover_pic', 'covers', ['jpg', 'jpeg', 'png']);

if ($name !== '') {
    $st = $conn->prepare('UPDATE users SET name=? WHERE id=?');
    $st->execute([$name, $uid]);
}

if ($username !== '') {
    $st = $conn->prepare('UPDATE users SET username=? WHERE id=?');
    $st->execute([$username, $uid]);
}

if ($bio !== '') {
    $st = $conn->prepare('UPDATE users SET bio=? WHERE id=?');
    $st->execute([$bio, $uid]);
}

if ($profilePicPath) {
    $st = $conn->prepare('UPDATE users SET profile_pic=? WHERE id=?');
    $st->execute([$profilePicPath, $uid]);
}

if ($coverPicPath) {
    $st = $conn->prepare('UPDATE users SET cover_pic=? WHERE id=?');
    $st->execute([$coverPicPath, $uid]);
}

out(['message' => 'Profile updated successfully']);
?>
