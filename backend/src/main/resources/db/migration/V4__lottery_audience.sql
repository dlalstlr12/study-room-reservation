-- 로드맵 5단계 개편: 추첨을 "기준 시각 스냅샷 + 예약된 추첨 시각" 모델에서
-- "대상(현재 이용중 / 전체) + ADMIN 수동 추첨" 모델로 바꾼다.
-- target_at / draw_at 제거, audience 추가.

ALTER TABLE lottery_events DROP INDEX idx_lottery_events_status_draw;
ALTER TABLE lottery_events DROP COLUMN target_at;
ALTER TABLE lottery_events DROP COLUMN draw_at;
ALTER TABLE lottery_events ADD COLUMN audience VARCHAR(20) NOT NULL DEFAULT 'CURRENT_USERS';
