-- liquibase formatted sql

-- changeset codex:1
CREATE TABLE IF NOT EXISTS file_info (
  id varchar(128) NOT NULL,
  object_key varchar(128) DEFAULT NULL COMMENT '资源名称',
  original_name varchar(128) NOT NULL COMMENT '资源原始名称',
  display_name varchar(128) NOT NULL COMMENT '资源别名',
  suffix varchar(20) DEFAULT NULL COMMENT '后缀名',
  size bigint DEFAULT NULL COMMENT '大小',
  mime_type varchar(128) DEFAULT NULL COMMENT '存储标准MIME类型',
  is_dir tinyint(1) NOT NULL COMMENT '是否目录',
  parent_id varchar(128) DEFAULT NULL COMMENT '父节点ID',
  user_id varchar(128) NOT NULL COMMENT '用户id',
  content_md5 text COMMENT '用于秒传和文件校验',
  storage_platform_setting_id varchar(128) DEFAULT NULL COMMENT '存储平台标识符',
  upload_time datetime NOT NULL COMMENT '上传时间',
  update_time datetime DEFAULT NULL COMMENT '修改时间',
  last_access_time datetime DEFAULT NULL COMMENT '最后访问时间',
  is_deleted tinyint(1) DEFAULT NULL COMMENT '软删除标记',
  deleted_time datetime DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (id),
  KEY idx_recycle_query (user_id, storage_platform_setting_id, is_deleted, parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件资源表';

-- changeset codex:2
CREATE TABLE IF NOT EXISTS storage_platform (
  id int NOT NULL AUTO_INCREMENT COMMENT '存储平台',
  name varchar(255) NOT NULL COMMENT '存储平台名称',
  identifier varchar(128) NOT NULL COMMENT '存储平台标识符',
  config_scheme json NOT NULL COMMENT '存储平台配置描述schema',
  icon varchar(128) DEFAULT NULL COMMENT '存储平台图标',
  link varchar(255) DEFAULT NULL COMMENT '存储平台链接',
  is_default tinyint NOT NULL DEFAULT 1 COMMENT '是否默认存储平台 0-否 1-是',
  `desc` varchar(255) DEFAULT NULL COMMENT '存储平台描述',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储平台';

-- changeset codex:3
CREATE TABLE IF NOT EXISTS storage_settings (
  id varchar(128) NOT NULL COMMENT 'id',
  platform_identifier varchar(128) NOT NULL COMMENT '存储平台标识符',
  config_data json NOT NULL COMMENT '存储配置',
  enabled tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用 0：否 1：是',
  user_id varchar(128) NOT NULL COMMENT '所属用户',
  created_at datetime DEFAULT NULL COMMENT '创建时间',
  updated_at datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(255) DEFAULT NULL COMMENT '备注',
  deleted tinyint(1) DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (id),
  KEY idx_storage_settings_user (user_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储平台配置';

-- changeset codex:4
CREATE TABLE IF NOT EXISTS file_shares (
  id varchar(128) NOT NULL COMMENT '分享ID',
  user_id varchar(128) NOT NULL COMMENT '分享人ID',
  share_name varchar(255) NOT NULL COMMENT '分享名称',
  share_code varchar(6) DEFAULT NULL COMMENT '提取码',
  expire_time datetime DEFAULT NULL COMMENT '过期时间',
  scope varchar(255) NOT NULL COMMENT '权限范围',
  view_count int DEFAULT 0 COMMENT '查看次数',
  max_view_count int DEFAULT NULL COMMENT '最大查看次数',
  download_count int DEFAULT 0 COMMENT '下载次数',
  max_download_count int DEFAULT NULL COMMENT '最大下载次数',
  created_at datetime NOT NULL,
  updated_at datetime NOT NULL,
  PRIMARY KEY (id),
  KEY idx_file_shares_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分享表';

-- changeset codex:5
CREATE TABLE IF NOT EXISTS file_share_items (
  share_id varchar(128) NOT NULL COMMENT '分享ID',
  file_id varchar(128) NOT NULL COMMENT '文件ID',
  created_at datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (share_id, file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享文件关联表';

-- changeset codex:6
CREATE TABLE IF NOT EXISTS file_transfer_task (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  task_id varchar(64) NOT NULL COMMENT '任务ID',
  upload_id varchar(255) DEFAULT NULL COMMENT '上传唯一ID',
  parent_id varchar(128) DEFAULT NULL COMMENT '父ID',
  user_id varchar(128) NOT NULL COMMENT '用户ID',
  storage_platform_setting_id varchar(255) DEFAULT NULL COMMENT '存储平台配置ID',
  object_key varchar(255) DEFAULT NULL COMMENT '对象key',
  file_id varchar(128) DEFAULT NULL COMMENT '文件ID',
  file_name varchar(255) NOT NULL COMMENT '文件名',
  file_size bigint NOT NULL COMMENT '文件大小',
  file_md5 varchar(64) DEFAULT NULL COMMENT '文件MD5',
  suffix varchar(50) DEFAULT NULL COMMENT '后缀',
  mime_type varchar(255) NOT NULL COMMENT 'MIME类型',
  total_chunks int NOT NULL COMMENT '总分片数',
  task_type varchar(32) DEFAULT NULL COMMENT '任务类型',
  uploaded_chunks int DEFAULT 0 COMMENT '已上传分片数',
  chunk_size bigint DEFAULT 5242880 COMMENT '分片大小',
  uploaded_size bigint DEFAULT 0 COMMENT '已上传大小',
  status varchar(20) NOT NULL DEFAULT 'uploading' COMMENT '状态',
  error_msg varchar(500) DEFAULT NULL COMMENT '错误信息',
  start_time datetime NOT NULL COMMENT '开始时间',
  complete_time datetime DEFAULT NULL COMMENT '完成时间',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_file_transfer_task_task_id (task_id),
  KEY idx_file_transfer_task_user (user_id, created_at),
  KEY idx_file_transfer_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传输任务表';

-- changeset codex:12
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'file_transfer_task' AND column_name = 'overwrite_existing'
ALTER TABLE file_transfer_task
  ADD COLUMN overwrite_existing tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否覆盖同目录下同名文件' AFTER uploaded_size;

-- changeset codex:7
CREATE TABLE IF NOT EXISTS file_user_favorites (
  user_id varchar(128) NOT NULL COMMENT '用户ID',
  file_id varchar(128) NOT NULL COMMENT '文件ID',
  created_at datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (user_id, file_id),
  KEY idx_file_user_favorites_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件收藏表';

-- changeset codex:8
CREATE TABLE IF NOT EXISTS file_share_access_records (
  id varchar(128) NOT NULL COMMENT '主键ID',
  share_id varchar(128) NOT NULL COMMENT '分享ID',
  access_ip varchar(64) DEFAULT NULL COMMENT '访问IP',
  access_address varchar(255) DEFAULT NULL COMMENT '访问地址',
  browser varchar(128) DEFAULT NULL COMMENT '浏览器',
  os varchar(128) DEFAULT NULL COMMENT '操作系统',
  access_time datetime NOT NULL COMMENT '访问时间',
  PRIMARY KEY (id),
  KEY idx_file_share_access_share (share_id, access_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分享访问记录表';

-- changeset codex:9
CREATE TABLE IF NOT EXISTS subscription_plan (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  plan_code varchar(50) NOT NULL COMMENT '套餐代码',
  plan_name varchar(100) NOT NULL COMMENT '套餐名称',
  description text DEFAULT NULL COMMENT '套餐描述',
  storage_quota_gb int NOT NULL COMMENT '存储配额(GB)',
  max_files int NOT NULL COMMENT '最大文件数',
  max_file_size bigint NOT NULL COMMENT '单个文件最大大小(字节)',
  bandwidth_quota bigint NOT NULL COMMENT '每月带宽配额(字节)',
  price double(8,2) NOT NULL COMMENT '价格/月',
  is_active tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用0否1是',
  is_default tinyint(1) NOT NULL COMMENT '是否为默认套餐 0否1是',
  sort_order int NOT NULL COMMENT '排序',
  created_at datetime NOT NULL COMMENT '创建时间',
  updated_at datetime NOT NULL COMMENT '更新时间',
  del_flag tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除 0否1是',
  PRIMARY KEY (id),
  KEY idx_subscription_plan_active (is_active, is_default, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐表';

-- changeset codex:10
CREATE TABLE IF NOT EXISTS user_subscription (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id varchar(128) NOT NULL COMMENT '租户id',
  plan_id bigint NOT NULL COMMENT '套餐id',
  status tinyint(1) NOT NULL DEFAULT 0 COMMENT '订阅状态 0-生效中，1-已过期',
  subscription_date datetime NOT NULL COMMENT '订阅日期',
  expire_date datetime NOT NULL COMMENT '到期日期',
  PRIMARY KEY (id),
  KEY idx_user_subscription_plan (plan_id, status),
  KEY idx_user_subscription_user (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅表';

-- changeset codex:11
CREATE TABLE IF NOT EXISTS user_quota_usage (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id varchar(128) NOT NULL COMMENT '用户ID',
  storage_used int NOT NULL COMMENT '已使用存储(GB)',
  files_count int NOT NULL COMMENT '文件数量',
  bandwidth_used_month bigint NOT NULL COMMENT '带宽使用情况(按月统计)',
  bandwidth_reset_date date DEFAULT NULL COMMENT '带宽重置日期',
  last_calculated_at datetime NOT NULL COMMENT '最后统计时间',
  updated_at datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user_quota_usage_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户配额使用情况表';
