<?php
require_once __DIR__ . '/../common.php';

$callerId = auth();
$targetId = (int)($_GET['id'] ?? $callerId);

$conn = db();
$st = $conn->prepare('SELECT id, name, username, email, bio, profile_pic, cover_pic, is_online, last_seen FROM users WHERE id=?');
$st->execute([$targetId]);
$user = $st->fetch();

if (!$user) {
    fail('User not found', 404);
}

// Compute friend status relative to caller
$friendStatus = 'none';
if ($callerId != $targetId) {
    $st = $conn->prepare('SELECT status, sender_id FROM friendships WHERE (sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?)');
    $st->execute([$callerId, $targetId, $targetId, $callerId]);
    if ($f = $st->fetch()) {
        if ($f['status'] == 'accepted') {
            $friendStatus = 'friends';
        } else if ($f['sender_id'] == $callerId) {
            $friendStatus = 'requested';
        } else {
            $friendStatus = 'incoming';
        }
    }
}

// Compute dynamic online status
$isOnline = ($user['is_online'] == 1 && $user['last_seen'] && (strtotime($user['last_seen']) > time() - 45));

// Friend count
$st = $conn->prepare('SELECT COUNT(*) as cnt FROM friendships WHERE (sender_id=? OR receiver_id=?) AND status="accepted"');
$st->execute([$targetId, $targetId]);
$friendCount = (int)$st->fetch()['cnt'];

$user['friend_status'] = $friendStatus;
$user['is_online'] = $isOnline ? 1 : 0;
$user['friend_count'] = $friendCount;

out($user);
?>
