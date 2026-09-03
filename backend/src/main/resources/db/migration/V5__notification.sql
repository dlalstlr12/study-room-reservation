-- 로드맵 6단계: 비동기 알림 이력.
-- Kafka 컨슈머(워커)가 발송에 성공하면 SENT 로, 재시도까지 소진하면 DLT 핸들러가 FAILED 로 남긴다.
-- dedup_key 는 at-least-once 재처리에서 중복 저장을 막는 멱등 키 (예: lottery:{eventId}:{memberId}).

CREATE TABLE notifications (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    member_id  BIGINT        NOT NULL,
    type       VARCHAR(40)   NOT NULL,   -- LOTTERY_WON | LOTTERY_LOST | ANNOUNCEMENT
    title      VARCHAR(200)  NOT NULL,
    body       VARCHAR(1000) NOT NULL,
    ref_id     BIGINT,                   -- 관련 엔티티 id (추첨 이벤트 등)
    status     VARCHAR(20)   NOT NULL,   -- SENT | FAILED
    dedup_key  VARCHAR(150)  NOT NULL,
    read_at    DATETIME(6),
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_dedup UNIQUE (dedup_key),
    CONSTRAINT fk_notification_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_notification_member ON notifications (member_id, id);
