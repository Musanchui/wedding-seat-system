-- ============================================
-- 测试数据：管理员1个 + 婚礼1场(已发布) + 照片3张 + 桌位3张(共28座) + 来宾2个
-- 管理员测试账号：username=spruce  password=123456 (下面存的是BCrypt加密后的值)
-- 婚礼访问链接：http://localhost:前端端口/w/zhang-li-0815
-- ============================================
USE wedding_seat;

-- 1. 管理员账号
INSERT INTO admin_user (username, password, phone, nickname, status)
VALUES ('spruce', '$2b$12$klonVXAC1HiWaB5e4EhYHOi5avdqmduEKwGT/paqy08333/GHS2KS', '13800000000', 'Spruce', 1);

-- 2. 婚礼实例（admin_id=1，对应上面插入的管理员；status=1表示已发布，来宾端可以访问）
INSERT INTO wedding_event (admin_id, slug, groom_name, bride_name, event_time, location, greeting_message, music_url, status)
VALUES (
           1,
           'zhang-li-0815',
           '张三',
           '李四',
           '2026-08-15 18:00:00',
           'XX大酒店 3楼宴会厅',
           '感谢您百忙之中抽空来参加我们的婚礼，愿您在这里度过愉快的一晚！',
           '/uploads/music/test-bgm.mp3',
           1
       );

-- 3. 轮播照片（event_id=1，对应上面插入的婚礼）
INSERT INTO photo (event_id, url, sort_order) VALUES
                                                  (1, '/uploads/photo/test-1.jpg', 0),
                                                  (1, '/uploads/photo/test-2.jpg', 1),
                                                  (1, '/uploads/photo/test-3.jpg', 2);

-- 4. 桌位（event_id=1）：1号桌新郎同学10人、2号桌新娘同学10人、3号桌长辈亲戚8人
INSERT INTO table_info (event_id, table_no, seat_count, remark, status) VALUES
                                                                            (1, '1', 10, '新郎大学同学', 1),
                                                                            (1, '2', 10, '新娘大学同学', 1),
                                                                            (1, '3', 8, '长辈亲戚', 1);

-- 5. 座位：按上面3张桌子的table_id(假设是1,2,3)和seat_count自动生成对应数量的座位
INSERT INTO seat (table_id, seat_no, status, guest_id, version) VALUES
                                                                    (1, 1, 0, NULL, 0),
                                                                    (1, 2, 0, NULL, 0),
                                                                    (1, 3, 0, NULL, 0),
                                                                    (1, 4, 0, NULL, 0),
                                                                    (1, 5, 0, NULL, 0),
                                                                    (1, 6, 0, NULL, 0),
                                                                    (1, 7, 0, NULL, 0),
                                                                    (1, 8, 0, NULL, 0),
                                                                    (1, 9, 0, NULL, 0),
                                                                    (1, 10, 0, NULL, 0),
                                                                    (2, 1, 0, NULL, 0),
                                                                    (2, 2, 0, NULL, 0),
                                                                    (2, 3, 0, NULL, 0),
                                                                    (2, 4, 0, NULL, 0),
                                                                    (2, 5, 0, NULL, 0),
                                                                    (2, 6, 0, NULL, 0),
                                                                    (2, 7, 0, NULL, 0),
                                                                    (2, 8, 0, NULL, 0),
                                                                    (2, 9, 0, NULL, 0),
                                                                    (2, 10, 0, NULL, 0),
                                                                    (3, 1, 0, NULL, 0),
                                                                    (3, 2, 0, NULL, 0),
                                                                    (3, 3, 0, NULL, 0),
                                                                    (3, 4, 0, NULL, 0),
                                                                    (3, 5, 0, NULL, 0),
                                                                    (3, 6, 0, NULL, 0),
                                                                    (3, 7, 0, NULL, 0),
                                                                    (3, 8, 0, NULL, 0);

-- 6. 来宾测试数据：2个来宾，其中guest1已经选座(占用1号桌1号座)，guest2只登记还没选座
INSERT INTO guest (event_id, name, phone, category, seat_id, register_time) VALUES
                                                                                (1, '王小明', '13900001111', '新郎大学同学', 1, NOW()),
                                                                                (1, '刘小红', '13900002222', '新娘大学同学', NULL, NOW());

-- 7. 同步把guest1对应的座位状态改成已占用（因为guest表和seat表要保持双向一致）
UPDATE seat SET status = 1, guest_id = 1, version = 1 WHERE id = 1;