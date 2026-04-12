-- Scope mes_box/mes_package uniqueness by batch_num + work_id to avoid cross-work collisions.

ALTER TABLE mes_box
    DROP INDEX uk_box_code;

ALTER TABLE mes_box
    ADD CONSTRAINT uk_batch_work_box UNIQUE (batch_num, work_id, box_code);

ALTER TABLE mes_package
    DROP INDEX uk_box_package;

ALTER TABLE mes_package
    ADD CONSTRAINT uk_batch_work_box_package UNIQUE (batch_num, work_id, box_code, package_no);
