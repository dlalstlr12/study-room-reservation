-- 로드맵 3단계: 룸 상태(status) 컬럼 제거.
-- 이 시스템은 시간대(start_at~end_at) 단위 예약이라 룸 단위 단일 status 값은 의미가 없다.
-- 특정 시점의 룸 가용성은 reservations 겹침 + Redis 홀딩으로 판단한다.

ALTER TABLE rooms DROP COLUMN status;
