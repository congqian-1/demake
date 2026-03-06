-- Make work orders unique by batch_num + work_id while still allowing the same work_id across batches.

ALTER TABLE mes_work_order
    DROP INDEX uk_work_id;

ALTER TABLE mes_work_order
    ADD CONSTRAINT uk_batch_work UNIQUE (batch_num, work_id);

ALTER TABLE mes_work_order
    ADD INDEX idx_work_id (work_id);
