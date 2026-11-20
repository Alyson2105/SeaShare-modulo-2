-- Trazabilidad de consultas al Modulo 1: estado crudo devuelto por M1 (CU-10 FR-009)
ALTER TABLE external_query_log
    ADD COLUMN returned_status VARCHAR(30) NULL;
