-- ============================================
-- 婚礼选座系统 - 数据库建表脚本
-- 数据库名：wedding_seat
-- 字符集：utf8mb4（支持emoji等特殊字符）
-- 存储引擎：InnoDB（支持事务和行锁，选座并发控制需要）
-- ============================================

CREATE DATABASE IF NOT EXISTS wedding_seat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE wedding_seat;

-- ============================================
-- 1. 管理员账号表
-- ============================================
CREATE TABLE admin_user (
                            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                            username VARCHAR(50) NOT NULL COMMENT '登录用户名',
                            password VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密后存储，不存明文）',
                            phone VARCHAR(20) DEFAULT NULL COMMENT '手机号（预留，可用于找回密码）',
                            nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称/显示名',
                            status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1=正常，0=禁用',
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='管理员账号表';


-- ============================================
-- 2. 婚礼实例表（一个管理员可创建多场婚礼）
-- ============================================
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
                               status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=筹备中（来宾端不可访问），1=已发布（来宾端可访问）',
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               UNIQUE KEY uk_slug (slug),
                               KEY idx_admin_id (admin_id),
                               CONSTRAINT fk_event_admin FOREIGN KEY (admin_id) REFERENCES admin_user (id)
) ENGINE=InnoDB COMMENT='婚礼实例表';


-- ============================================
-- 3. 轮播照片表
-- ============================================
CREATE TABLE photo (
                       id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                       event_id BIGINT UNSIGNED NOT NULL COMMENT '所属婚礼ID，关联 wedding_event.id',
                       url VARCHAR(500) NOT NULL COMMENT '图片访问URL',
                       sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号，数字越小越靠前',
                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
                       KEY idx_event_id (event_id, sort_order),
                       CONSTRAINT fk_photo_event FOREIGN KEY (event_id) REFERENCES wedding_event (id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='婚礼轮播照片表';


-- ============================================
-- 4. 桌位表
-- ============================================
CREATE TABLE table_info (
                            id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                            event_id BIGINT UNSIGNED NOT NULL COMMENT '所属婚礼ID，关联 wedding_event.id',
                            table_no VARCHAR(20) NOT NULL COMMENT '桌号，支持"1""A1""主桌"等自定义格式',
                            seat_count INT NOT NULL DEFAULT 10 COMMENT '本桌总座位数',
                            remark VARCHAR(255) DEFAULT NULL COMMENT '备注，如"新郎大学同学"，用于选座推荐匹配',
                            status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=启用，0=停用（临时不开放此桌）',
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            KEY idx_event_id (event_id),
                            UNIQUE KEY uk_event_table_no (event_id, table_no),
                            CONSTRAINT fk_table_event FOREIGN KEY (event_id) REFERENCES wedding_event (id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='桌位表';


-- ============================================
-- 5. 座位表
-- ============================================
CREATE TABLE seat (
                      id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                      table_id BIGINT UNSIGNED NOT NULL COMMENT '所属桌位ID，关联 table_info.id',
                      seat_no INT NOT NULL COMMENT '座位号（桌内编号，如1-10）',
                      status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=空闲，1=已占用',
                      guest_id BIGINT UNSIGNED DEFAULT NULL COMMENT '占用此座位的来宾ID，关联 guest.id（权威字段，选座并发控制以此行加锁）',
                      version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，选座时用于防止并发冲突',
                      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                      KEY idx_table_id (table_id),
                      KEY idx_guest_id (guest_id),
                      UNIQUE KEY uk_table_seat_no (table_id, seat_no),
                      CONSTRAINT fk_seat_table FOREIGN KEY (table_id) REFERENCES table_info (id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='座位表';


-- ============================================
-- 6. 来宾登记表
-- ============================================
CREATE TABLE guest (
                       id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                       event_id BIGINT UNSIGNED NOT NULL COMMENT '所属婚礼ID，关联 wedding_event.id',
                       name VARCHAR(50) NOT NULL COMMENT '来宾姓名',
                       phone VARCHAR(20) NOT NULL COMMENT '来宾手机号',
                       category VARCHAR(50) DEFAULT NULL COMMENT '来源类别，如"新郎同学""新娘同事"，用于座位推荐匹配table_info.remark',
                       seat_id BIGINT UNSIGNED DEFAULT NULL COMMENT '所选座位ID（冗余反向引用，方便查询，需与seat.guest_id同事务保持一致）',
                       register_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
                       KEY idx_event_id (event_id),
                       KEY idx_seat_id (seat_id),
                       UNIQUE KEY uk_event_phone (event_id, phone) COMMENT '同一场婚礼内手机号不可重复登记',
                       CONSTRAINT fk_guest_event FOREIGN KEY (event_id) REFERENCES wedding_event (id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='来宾登记表';