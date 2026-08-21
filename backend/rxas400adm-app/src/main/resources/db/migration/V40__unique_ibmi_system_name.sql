-- ============================================================================
-- P3：为 rx_ibmi_system.name 加唯一约束（多服务器管理按名称唯一标识）
--
-- 策略（先查重、再建索引）：
--   1) 存量重名：保留 id 最小的记录，其余追加 "_<id>" 后缀（最多 100 字符），
--      循环至无重名（后缀可能再次撞上既有名称的极端场景兜底，上限 10 轮防死循环）；
--   2) 建唯一索引 uk_ibmi_system_name；
--   3) 入口校验由 IbmiSystemService.create/update 负责（友好报错），此处仅兜底存量。
-- ============================================================================

DELIMITER $$
CREATE PROCEDURE dedupe_ibmi_system_name()
BEGIN
    DECLARE dup_cnt INT DEFAULT 1;
    DECLARE loop_cnt INT DEFAULT 0;
    WHILE dup_cnt > 0 AND loop_cnt < 10 DO
        SET loop_cnt = loop_cnt + 1;
        -- 临时表规避 "can't specify target table for update in FROM clause"
        DROP TEMPORARY TABLE IF EXISTS tmp_ibmi_name_dup;
        CREATE TEMPORARY TABLE tmp_ibmi_name_dup AS
            SELECT name, MIN(id) AS keep_id
            FROM rx_ibmi_system
            GROUP BY name
            HAVING COUNT(*) > 1;
        SELECT COUNT(*) INTO dup_cnt FROM tmp_ibmi_name_dup;
        UPDATE rx_ibmi_system s
        JOIN tmp_ibmi_name_dup d ON s.name = d.name AND s.id <> d.keep_id
        SET s.name = LEFT(CONCAT(s.name, '_', s.id), 100);
    END WHILE;
    DROP TEMPORARY TABLE IF EXISTS tmp_ibmi_name_dup;
END$$
DELIMITER ;

CALL dedupe_ibmi_system_name();
DROP PROCEDURE dedupe_ibmi_system_name;

-- 若 10 轮后仍重名（极端数据），此处会失败并中止迁移，由管理员人工清理
CREATE UNIQUE INDEX uk_ibmi_system_name ON rx_ibmi_system (name);
