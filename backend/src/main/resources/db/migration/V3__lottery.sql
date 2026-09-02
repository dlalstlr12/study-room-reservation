-- 로드맵 5단계: 이벤트 추첨 (lottery_events / lottery_entries)
-- target_at 시점에 이용 중이던 회원이 응모 대상, draw_at 이 지나면 스케줄러가 추첨.
-- seed 는 추첨 시 기록해 결과를 재현·검증할 수 있게 한다.

CREATE TABLE lottery_events (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    title        VARCHAR(100) NOT NULL,
    prize        VARCHAR(200) NOT NULL,
    target_at    DATETIME(6)  NOT NULL,
    draw_at      DATETIME(6)  NOT NULL,
    winner_count INT          NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    seed         BIGINT,
    drawn_at     DATETIME(6),
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE lottery_entries (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    event_id   BIGINT      NOT NULL,
    member_id  BIGINT      NOT NULL,
    winner     BIT(1)      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_lottery_entry UNIQUE (event_id, member_id),
    CONSTRAINT fk_lottery_entry_event FOREIGN KEY (event_id) REFERENCES lottery_events (id),
    CONSTRAINT fk_lottery_entry_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_lottery_entry_member ON lottery_entries (member_id);
CREATE INDEX idx_lottery_events_status_draw ON lottery_events (status, draw_at);
