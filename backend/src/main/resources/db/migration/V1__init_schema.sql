-- 로드맵 1단계: 코어 도메인 스키마 (members / rooms / reservations)
-- 엔티티 매핑과 1:1로 맞춘다 (spring.jpa.hibernate.ddl-auto=validate).

CREATE TABLE members (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(320) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_members_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rooms (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    capacity    INT          NOT NULL,
    description VARCHAR(500),
    status      VARCHAR(20)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE reservations (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    member_id  BIGINT      NOT NULL,
    room_id    BIGINT      NOT NULL,
    start_at   DATETIME(6) NOT NULL,
    end_at     DATETIME(6) NOT NULL,
    status     VARCHAR(20) NOT NULL,
    version    BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reservations_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_reservations_room FOREIGN KEY (room_id) REFERENCES rooms (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 룸별 시간 겹침 검사(existsOverlap) 최적화
CREATE INDEX idx_reservations_room_time ON reservations (room_id, start_at, end_at);
CREATE INDEX idx_reservations_member ON reservations (member_id);
