-- posts 조회/삭제 경로 풀스캔 제거
-- team_id: 팀 피드 조회(id DESC = 최신순, InnoDB가 PK를 뒤에 붙여 filesort 불필요) 및 팀 삭제 cascade
-- user_id: 사용자 탈퇴 시 cascade soft-delete 및 notification cascade JOIN
CREATE INDEX idx_posts_team
    ON posts (team_id);

CREATE INDEX idx_posts_user
    ON posts (user_id);
