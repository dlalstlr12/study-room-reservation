-- 로드맵 8단계: 정기 구독권.
-- subscriptions(회원당 1개) · payments(멱등키 UNIQUE) · outbox_events(트랜잭션 아웃박스).
-- 결제·상태변경·outbox 저장이 한 트랜잭션 → 릴레이가 outbox를 읽어 Kafka로 발행.

CREATE TABLE subscriptions (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    member_id       BIGINT      NOT NULL,
    plan            VARCHAR(20) NOT NULL,   -- FREE | PRO
    status          VARCHAR(20) NOT NULL,   -- ACTIVE | PAST_DUE | CANCELLED
    price_krw       INT         NOT NULL,
    next_billing_at DATETIME(6) NULL,
    started_at      DATETIME(6) NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_subscription_member UNIQUE (member_id),
    CONSTRAINT fk_subscription_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_subscription_due ON subscriptions (status, next_billing_at);

CREATE TABLE payments (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    subscription_id BIGINT       NOT NULL,
    member_id       BIGINT       NOT NULL,
    amount_krw      INT          NOT NULL,
    status          VARCHAR(20)  NOT NULL,   -- SUCCEEDED | FAILED
    idempotency_key VARCHAR(100) NOT NULL,
    failure_reason  VARCHAR(200) NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_idem UNIQUE (idempotency_key),
    CONSTRAINT fk_payment_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_payment_member ON payments (member_id, id);

CREATE TABLE outbox_events (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    aggregate_type VARCHAR(40) NOT NULL,   -- SUBSCRIPTION
    aggregate_id   BIGINT      NOT NULL,
    event_type     VARCHAR(40) NOT NULL,   -- PAYMENT_SUCCEEDED | PAYMENT_FAILED
    payload        VARCHAR(2000) NOT NULL, -- JSON (SubscriptionEventMessage)
    published_at   DATETIME(6) NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_outbox_unpublished ON outbox_events (published_at, id);
