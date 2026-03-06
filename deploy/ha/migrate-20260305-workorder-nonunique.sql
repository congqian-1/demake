-- Allow the same work_id to appear under different batch/file combinations.

ALTER TABLE mes_work_order
    DROP INDEX uk_work_id;

ALTER TABLE mes_work_order
    ADD INDEX idx_work_id (work_id);
