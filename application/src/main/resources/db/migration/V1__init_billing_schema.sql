CREATE TABLE IF NOT EXISTS invoice (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL UNIQUE,
    customer_document VARCHAR(14) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    external_payment_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_invoice_work_order_id ON invoice (work_order_id);
CREATE INDEX IF NOT EXISTS idx_invoice_status ON invoice (status);
CREATE INDEX IF NOT EXISTS idx_invoice_external_payment_id ON invoice (external_payment_id);
