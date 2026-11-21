ALTER TABLE recruitments
    ADD COLUMN approved_count INT NOT NULL DEFAULT 0 AFTER `count`;

UPDATE recruitments r
SET r.approved_count = (
    SELECT COUNT(*)
    FROM applications a
    WHERE a.post_id = r.post_id
      AND a.position = r.position
      AND a.status = 'APPROVED'
);

UPDATE recruitments
SET status = 'CLOSED', updated_at = UTC_TIMESTAMP(6)
WHERE approved_count >= `count`
  AND status = 'RECRUITING';

UPDATE posts p
JOIN (
    SELECT post_id, MAX(updated_at) AS closed_at
    FROM recruitments
    GROUP BY post_id
    HAVING SUM(status = 'RECRUITING') = 0
) r ON r.post_id = p.id
SET p.expires_at = DATE(CONVERT_TZ(r.closed_at, '+00:00', '+09:00')) + INTERVAL 1 DAY - INTERVAL 9 HOUR
WHERE p.deleted_at IS NULL
  AND (p.expires_at IS NULL
       OR p.expires_at > DATE(CONVERT_TZ(r.closed_at, '+00:00', '+09:00')) + INTERVAL 1 DAY - INTERVAL 9 HOUR);
