<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$uuid = $in['client_uuid'] ?? fail('client_uuid required');
$text = trim($in['text'] ?? '');

$conn = db();

$st = $conn->prepare('SELECT p.*, u.name, u.username, u.profile_pic FROM posts p JOIN users u ON p.user_id = u.id WHERE p.client_uuid=?');
$st->execute([$uuid]);
if ($row = $st->fetch()) {
    $stImg = $conn->prepare('SELECT path FROM post_images WHERE post_id=? ORDER BY sort_order ASC');
    $stImg->execute([$row['id']]);
    $row['images'] = $stImg->fetchAll(PDO::FETCH_COLUMN);
    out($row);
}

try {
    $conn->beginTransaction();
    $st = $conn->prepare('INSERT INTO posts (user_id, text, client_uuid) VALUES (?, ?, ?)');
    $st->execute([$uid, $text, $uuid]);
    $postId = (int)$conn->lastInsertId();

    $images = [];
    if (!empty($_FILES['images'])) {
        $files = $_FILES['images'];
        $count = is_array($files['name']) ? count($files['name']) : 1;

        for ($i = 0; $i < $count; $i++) {
            $_FILES['single_img'] = [
                'name' => is_array($files['name']) ? $files['name'][$i] : $files['name'],
                'type' => is_array($files['type']) ? $files['type'][$i] : $files['type'],
                'tmp_name' => is_array($files['tmp_name']) ? $files['tmp_name'][$i] : $files['tmp_name'],
                'error' => is_array($files['error']) ? $files['error'][$i] : $files['error'],
                'size' => is_array($files['size']) ? $files['size'][$i] : $files['size'],
            ];
            $path = save_upload('single_img', 'posts', ['jpg', 'jpeg', 'png']);
            if ($path) {
                $stImg = $conn->prepare('INSERT INTO post_images (post_id, path, sort_order) VALUES (?, ?, ?)');
                $stImg->execute([$postId, $path, $i]);
                $images[] = $path;
            }
        }
    }

    $conn->commit();

    $stUser = $conn->prepare('SELECT name, username, profile_pic FROM users WHERE id=?');
    $stUser->execute([$uid]);
    $uData = $stUser->fetch();

    out([
        'id' => $postId,
        'user_id' => $uid,
        'text' => $text,
        'client_uuid' => $uuid,
        'created_at' => date('Y-m-d H:i:s'),
        'images' => $images,
        'name' => $uData['name'],
        'username' => $uData['username'],
        'profile_pic' => $uData['profile_pic']
    ]);

} catch (Exception $e) {
    if ($conn->inTransaction()) $conn->rollBack();
    fail('Failed to create post: ' . $e->getMessage());
}
?>
