-- ============================================
-- 迁移：guest 与 seat 从"一对一"改为"一对多"（一个来宾最多可选3个座位）
-- 原来 guest.seat_id 是单一外键，只能存一个座位，现在一个来宾可能对应多个座位，
-- 单一列已经无法表达这种关系，改为完全依赖 seat.guest_id 做反向查询
-- （查某个来宾选了哪些座位，用 SELECT * FROM seat WHERE guest_id = ? 即可，不再需要guest.seat_id）
-- ============================================
USE wedding_seat;

ALTER TABLE guest DROP COLUMN seat_id;
