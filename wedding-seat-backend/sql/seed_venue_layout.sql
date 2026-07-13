-- ============================================
-- 场地布局测试数据：给已有的3张桌子设置坐标位置，并插入舞台/屏幕/入口/出口示例
-- 画布尺寸：1000 x 800（wedding_event.layout_width/height 默认值）
-- 前提：已执行过 migration_venue_layout.sql 加完字段，且之前的 seed_test_data.sql 已插入了event_id=1和3张桌子
-- ============================================
USE wedding_seat;

-- 1. 更新3张桌子的坐标（假设table_id是4,5,6，如果不是这个ID，先查一下 SELECT id, table_no FROM table_info; 再调整这里的id）
UPDATE table_info SET pos_x = 200, pos_y = 500 WHERE event_id = 1 AND table_no = '1';
UPDATE table_info SET pos_x = 500, pos_y = 500 WHERE event_id = 1 AND table_no = '2';
UPDATE table_info SET pos_x = 800, pos_y = 500 WHERE event_id = 1 AND table_no = '3';

-- 2. 插入场地元素：舞台在画布上方居中，屏幕在舞台后方，主入口在下方左侧，出口在下方右侧
INSERT INTO venue_element (event_id, type, label, pos_x, pos_y, width, height, rotation, sort_order) VALUES
(1, 'stage',    '舞台',   350, 50,  300, 120, 0, 0),
(1, 'screen',   '投影幕布', 400, 20,  200, 30,  0, 1),
(1, 'entrance', '主入口',  50,  700, 120, 60,  0, 2),
(1, 'exit',     '出口',    850, 700, 120, 60,  0, 3);
