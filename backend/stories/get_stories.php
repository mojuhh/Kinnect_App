<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$conn = db();

$sql = '
    SELECT s.*, u.name, u.username, u.profile_pic
    FROM stories s
    JOIN users u ON s.user_id = u.id
    WHERE s.created_at > UTC_TIMESTAMP() - INTERVAL 24 HOUR
      AND (
          s.user_id = ?
          OR (
              s.audience = "story"
              AND s.user_id IN (
                  SELECT CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END
                  FROM friendships WHERE (sender_id = ? OR receiver_id = ?) AND status = "accepted"
              )
          )
          OR (
              s.audience = "close_friends"
              AND s.user_id IN (
                  SELECT user_id FROM close_friends WHERE friend_id = ?
              )
          )
      )
    ORDER BY s.id DESC
';

$st = $conn->prepare($sql);
$st->execute([$uid, $uid, $uid, $uid, $uid]);
$stories = $st->fetchAll();

out($stories);
?>
