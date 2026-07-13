-- ============================================
-- 场地布局功能：数据库变更脚本
-- 目的：让 table_info 每张桌子有具体的坐标位置，
--      并新增 venue_element 表存放舞台/屏幕/出入口等非桌子元素
-- ============================================
USE wedding_seat;

-- 1. wedding_event 表加字段：画布整体尺寸（前端渲染场地图时的坐标系范围）
ALTER TABLE wedding_event
    ADD COLUMN layout_width  INT NOT NULL DEFAULT 1000 COMMENT '场地布局画布宽度（像素/相对单位，前端按这个比例渲染）',
    ADD COLUMN layout_height INT NOT NULL DEFAULT 800  COMMENT '场地布局画布高度';

-- 2. table_info 表加字段：每张桌子在画布上的坐标（管理端拖拽配置，来宾端只读展示）
ALTER TABLE table_info
    ADD COLUMN pos_x INT DEFAULT NULL COMMENT '桌子在画布上的X坐标，NULL表示管理员还没配置布局位置',
    ADD COLUMN pos_y INT DEFAULT NULL COMMENT '桌子在画布上的Y坐标',
    ADD COLUMN rotation INT NOT NULL DEFAULT 0 COMMENT '桌子旋转角度（0-359度），方便贴合场地形状摆放';

-- 3. 新建场地元素表：舞台、屏幕、主入口、副出口等
CREATE TABLE venue_element (
                               id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                               event_id BIGINT UNSIGNED NOT NULL COMMENT '所属婚礼ID，关联 wedding_event.id',
                               type VARCHAR(20) NOT NULL COMMENT '元素类型：stage=舞台，screen=屏幕，entrance=入口，exit=出口，other=其他自定义标注',
                               label VARCHAR(50) DEFAULT NULL COMMENT '展示文字，如"主入口""投影幕布"，type=other时必填，其他类型可选（不填就用type的默认图标+名称展示）',
                               pos_x INT NOT NULL COMMENT '在画布上的X坐标',
                               pos_y INT NOT NULL COMMENT '在画布上的Y坐标',
                               width INT NOT NULL DEFAULT 100 COMMENT '元素在画布上占的宽度',
                               height INT NOT NULL DEFAULT 60 COMMENT '元素在画布上占的高度',
                               rotation INT NOT NULL DEFAULT 0 COMMENT '旋转角度（0-359度）',
                               sort_order INT NOT NULL DEFAULT 0 COMMENT '同类型元素多个时的排序/层级',
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               KEY idx_event_id (event_id),
                               CONSTRAINT fk_venue_element_event FOREIGN KEY (event_id) REFERENCES wedding_event (id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='场地元素表：舞台/屏幕/出入口等非桌子的场地标注';