-- P2-19：V30 创建的 rx_scheduler_lock 是死表（采集 Leader 选举用 MySQL GET_LOCK，代码无任何读写），删除
DROP TABLE IF EXISTS rx_scheduler_lock;

-- P2-1：JWT 吊销名单（登出时按 jti 登记，有效期内拒绝认证；过期记录由 TokenBlacklistService 定时清理）
CREATE TABLE IF NOT EXISTS rx_token_blacklist (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    jti          VARCHAR(64)  NOT NULL COMMENT 'JWT jti（UUID，吊销唯一标识）',
    username     VARCHAR(64)  NOT NULL COMMENT '所属用户',
    expire_time  DATETIME     NOT NULL COMMENT 'token 到期时间（超过即可物理删除）',
    created_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '吊销时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_token_blacklist_jti (jti),
    KEY idx_token_blacklist_expire (expire_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='JWT 吊销名单（登出令牌）';
