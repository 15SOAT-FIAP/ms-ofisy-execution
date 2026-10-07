CREATE TABLE service_order_executions (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    service_catalog_id UUID        NOT NULL,
    service_order_id   UUID        NOT NULL,
    status             VARCHAR(50) NOT NULL,
    created_at         TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP   NOT NULL DEFAULT now(),
    started_at         TIMESTAMP,
    finished_at        TIMESTAMP,
    CONSTRAINT ck_service_order_executions_status
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT uq_service_order_executions_order_catalog
        UNIQUE (service_order_id, service_catalog_id)
);

CREATE INDEX idx_service_order_executions_service_catalog_id ON service_order_executions (service_catalog_id);
CREATE INDEX idx_service_order_executions_status             ON service_order_executions (status);
CREATE INDEX idx_service_order_executions_created_at         ON service_order_executions (created_at);