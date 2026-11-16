<?php
require_once __DIR__ . '/../common.php';

$callerId = auth();
$q = trim($_GET['q'] ?? '');
$filter = $_GET['filter'] ?? 'everyone'; // everyone | friends | requests

$conn = db();
$likeQuery = '%' . $q . '%';

$sql = 'SELECT id, name, username, bio, profile_pic, is_online, last_seen FROM users WHERE id != ? AND (name LIKE ? OR username LIKE ?) LIMIT 30';
$st = $conn->prepare($sql);
$st->execute([$callerId, $likeQuery, $likeQuery]);
$rows = $st->fetchAll();

$results = [];
foreach ($rows as $user) {
    // Check friend status
    $stF = $conn->prepare('SELECT status, sender_id FROM friendships WHERE (sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?)');
    $stF->execute([$callerId, $user['id'], $user['id'], $callerId]);
    $f = $stF->fetch();

    $friendStatus = 'none';
    if ($f) {
        if ($f['status'] == 'accepted') {
            $friendStatus = 'friends';
        } else if ($f['sender_id'] == $callerId) {
            $friendStatus = 'requested';
        } else {
            $friendStatus = 'incoming';
        }
    }

    // Apply filter chip logic
    if ($filter == 'friends' && $friendStatus != 'friends') continue;
    if ($filter == 'requests' && $friendStatus != 'incoming' && $friendStatus != 'requested') continue;

    $isOnline = ($user['is_online'] == 1 && $user['last_seen'] && (strtotime($user['last_seen']) > time() - 45));
    $user['friend_status'] = $friendStatus;
    $user['is_online'] = $isOnline ? 1 : 0;
    $results[] = $user;
}

out($results);
?>
