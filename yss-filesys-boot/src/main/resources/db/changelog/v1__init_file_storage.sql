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
CREATE TABLE IF NOT EXISTS sys_login_log (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  user_id varchar(100) DEFAULT NULL COMMENT '用户编号',
  username varchar(50) NOT NULL DEFAULT '' COMMENT '用户账号',
  login_ip varchar(50) NOT NULL COMMENT '登录IP',
  login_address varchar(255) DEFAULT NULL COMMENT '登录地址',
  browser varchar(255) DEFAULT NULL COMMENT '浏览器类型',
  os varchar(512) NOT NULL COMMENT '操作系统',
  status tinyint NOT NULL COMMENT '登录状态（0成功 1失败）',
  msg varchar(255) NOT NULL COMMENT '提示消息',
  login_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (id),
  KEY idx_sys_login_log_user (user_id, login_time),
  KEY idx_sys_login_log_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统访问记录';
