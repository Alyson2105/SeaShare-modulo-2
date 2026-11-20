-- CU-13: FAILED tambien es un resultado definitivo del contrato interno.
-- Reversion: solo despues de tratar las filas FAILED, restaurar el CHECK de V3.
ALTER TABLE payment_attempt DROP CONSTRAINT ck_payment_result;
ALTER TABLE payment_attempt ADD CONSTRAINT ck_payment_result CHECK (result IN (
    'IN_PROCESS', 'APPROVED', 'REJECTED', 'CANCELLED', 'EXPIRED', 'UNKNOWN', 'FAILED'));
