-- 로드맵 7단계: 이용 로그 (랭킹 집계의 원천).
-- 퇴실 시 회원의 실제 이용 분을 기록한다. reservation_id UNIQUE 가 at-least-once 재처리에서
-- 중복 집계를 막는 멱등 가드이자, Redis Sorted Set 랭킹이 유실됐을 때 재구축 소스다.

ALTER TABLE reservations ADD COLUMN checked_out_at DATETIME(6) NULL;

CREATE TABLE usage_logs (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    reservation_id BIGINT      NOT NULL,
    member_id      BIGINT      NOT NULL,
    room_id        BIGINT      NOT NULL,
    minutes        INT         NOT NULL,
    occurred_at    DATETIME(6) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usage_log_reservation UNIQUE (reservation_id),
    CONSTRAINT fk_usage_log_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_usage_log_member ON usage_logs (member_id);
