<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$conn = db();

$sql = '
    SELECT n.*, u.name as from_user_name, u.username as from_user_username, u.profile_pic as from_user_profile_pic
    FROM notifications n
    LEFT JOIN users u ON n.from_user_id = u.id
    WHERE n.user_id = ?
    ORDER BY n.id DESC
    LIMIT 50
';

$st = $conn->prepare($sql);
$st->execute([$uid]);
out($st->fetchAll());
?>
