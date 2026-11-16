<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$conn = db();

$sql = '
    SELECT
        m.id as last_message_id,
        CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END as peer_id,
        m.content as last_message,
        m.type as last_message_type,
        m.created_at as last_message_time,
        m.sender_id as last_sender_id,
        u.name as peer_name,
        u.username as peer_username,
        u.profile_pic as peer_profile_pic,
        u.is_online as peer_is_online,
        u.last_seen as peer_last_seen
    FROM messages m
    JOIN users u ON u.id = (CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END)
    INNER JOIN (
        SELECT MAX(id) as max_id
        FROM messages
        WHERE sender_id = ? OR receiver_id = ?
        GROUP BY CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END
    ) max_msgs ON m.id = max_msgs.max_id
    ORDER BY m.id DESC
';

$st = $conn->prepare($sql);
$st->execute([$uid, $uid, $uid, $uid, $uid]);
$convs = $st->fetchAll();

foreach ($convs as &$c) {
    $c['peer_is_online'] = ($c['peer_is_online'] == 1 && $c['peer_last_seen'] && (strtotime($c['peer_last_seen']) > time() - 45)) ? 1 : 0;
}

out($convs);
?>
