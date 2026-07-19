-- ============================================
-- 婚礼选座系统 - 生产环境初始化脚本（最终版，整合了开发过程中的所有迁移）
-- 用途：全新服务器第一次部署时，一次性建好完整的最终表结构，不需要再手动跑之前的历史迁移文件
-- 数据库名：wedding_seat，字符集 utf8mb4，InnoDB引擎
-- ============================================

CREATE DATABASE IF NOT EXISTS wedding_seat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE wedding_seat;

-- 1. 管理员账号表
CREATE TABLE admin_user (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username VARCHAR(50) NOT NULL COMMENT '登录用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密后存储，不存明文）',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称/显示名',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1=正常，0=禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='管理员账号表';

-- 2. 婚礼实例表（含场地布局画布尺寸字段）
CREATE TABLE wedding_event (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    admin_id BIGINT UNSIGNED NOT NULL COMMENT '所属管理员ID，关联 admin_user.id',
    slug VARCHAR(50) NOT NULL COMMENT '访问短标识，用于生成来宾端URL，如 zhang-li-0815',
    groom_name VARCHAR(50) DEFAULT NULL COMMENT '新郎姓名',
    bride_name VARCHAR(50) DEFAULT NULL COMMENT '新娘姓名',
    event_time DATETIME DEFAULT NULL COMMENT '婚礼开始时间',
    location VARCHAR(255) DEFAULT NULL COMMENT '婚礼地点',
    greeting_message TEXT COMMENT '寄语/欢迎语',
    music_url VARCHAR(500) DEFAULT NULL COMMENT '背景音乐文件URL',
    layout_width INT NOT NULL DEFAULT 1000 COMMENT '场地布局画布宽度',
    layout_height INT NOT NULL DEFAULT 800 COMMENT '场地布局画布高度',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=筹备中，1=已发布',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_slug (slug),
    KEY idx_admin_id (admin_id),
    CONSTRAINT fk_event_admin FOREIGN KEY (admin_id) REFERENCES admin_user (id)
) ENGINE=InnoDB COMMENT='婚礼实例表';

-- 3. 轮播照片表
CREATE TABLE photo (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    event_id BIGINT UNSIGNED NOT NULL COMMENT '所属婚礼ID',
    url VARCHAR(500) NOT NULL COMMENT '图片访问URL',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    KEY idx_event_id (event_id, sort_order),
    CONSTRAINT fk_photo_event FOREIGN KEY (event_id) REFERENCES wedding_event (id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='婚礼轮播照片表';

-- 4. 桌位表（含场地布局坐标字段）
CREATE TABLE table_info (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    event_id BIGINT UNSIGNED NOT NULL COMMENT '所属婚礼ID',
    table_no VARCHAR(20) NOT NULL COMMENT '桌号',
    seat_count INT NOT NULL DEFAULT 10 COMMENT '本桌总座位数',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注，用于选座推荐匹配',
    pos_x INT DEFAULT NULL COMMENT '桌子在画布上的X坐标',
    pos_y INT DEFAULT NULL COMMENT '桌子在画布上的Y坐标',
    rotation INT NOT NULL DEFAULT 0 COMMENT '旋转角度',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=启用，0=停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_event_id (event_id),
    UNIQUE KEY uk_event_table_no (event_id, table_no),
    CONSTRAINT fk_table_event FOREIGN KEY (event_id) REFERENCES wedding_event (id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='桌位表';

-- 5. 座位表
CREATE TABLE seat (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    table_id BIGINT UNSIGNED NOT NULL COMMENT '所属桌位ID',
    seat_no INT NOT NULL COMMENT '座位号',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=空闲，1=已占用',
    guest_id BIGINT UNSIGNED DEFAULT NULL COMMENT '占用此座位的来宾ID（权威字段）',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_table_id (table_id),
    KEY idx_guest_id (guest_id),
    UNIQUE KEY uk_table_seat_no (table_id, seat_no),
    CONSTRAINT fk_seat_table FOREIGN KEY (table_id) REFERENCES table_info (id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='座位表';

-- 6. 来宾登记表（注意：不含seat_id字段，一对多关系完全靠seat.guest_id维护，一个来宾最多占3个座位由应用层校验）
CREATE TABLE guest (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    event_id BIGINT UNSIGNED NOT NULL COMMENT '所属婚礼ID',
    name VARCHAR(50) NOT NULL COMMENT '来宾姓名',
    phone VARCHAR(20) NOT NULL COMMENT '来宾手机号',
    category VARCHAR(50) DEFAULT NULL COMMENT '来源类别，用于座位推荐匹配',
    register_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
    KEY idx_event_id (event_id),
    UNIQUE KEY uk_event_phone (event_id, phone) COMMENT '同一场婚礼内手机号不可重复登记',
    CONSTRAINT fk_guest_event FOREIGN KEY (event_id) REFERENCES wedding_event (id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='来宾登记表';

-- 7. 场地元素表：舞台/屏幕/出入口等非桌子的场地标注
CREATE TABLE venue_element (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    event_id BIGINT UNSIGNED NOT NULL COMMENT '所属婚礼ID',
    type VARCHAR(20) NOT NULL COMMENT '元素类型：stage/screen/entrance/exit/other',
    label VARCHAR(50) DEFAULT NULL COMMENT '展示文字',
    pos_x INT NOT NULL COMMENT 'X坐标',
    pos_y INT NOT NULL COMMENT 'Y坐标',
    width INT NOT NULL DEFAULT 100 COMMENT '宽度',
    height INT NOT NULL DEFAULT 60 COMMENT '高度',
    rotation INT NOT NULL DEFAULT 0 COMMENT '旋转角度',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序/层级',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_event_id (event_id),
    CONSTRAINT fk_venue_element_event FOREIGN KEY (event_id) REFERENCES wedding_event (id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='场地元素表';
