-- V89: Rename rx_i18n.updated_at → updated_time to match project convention
-- V81 used updated_at, but project convention (V54/V65+) is updated_time/created_time
ALTER TABLE rx_i18n RENAME COLUMN updated_at TO updated_time;
