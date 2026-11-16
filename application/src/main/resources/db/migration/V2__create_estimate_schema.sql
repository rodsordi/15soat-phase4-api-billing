CREATE TABLE IF NOT EXISTS estimate (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL UNIQUE,
    customer_document VARCHAR(14) NOT NULL,
    customer_email VARCHAR(255),
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_estimate_work_order_id ON estimate (work_order_id);
CREATE INDEX IF NOT EXISTS idx_estimate_status ON estimate (status);
CREATE INDEX IF NOT EXISTS idx_estimate_customer_document ON estimate (customer_document);

CREATE TABLE IF NOT EXISTS estimate_item (
    id UUID PRIMARY KEY,
    estimate_id UUID NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    item_code_or_sku VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    quantity INTEGER NOT NULL DEFAULT 1,
    subtotal NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_estimate_item_estimate FOREIGN KEY (estimate_id) REFERENCES estimate (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_estimate_item_estimate_id ON estimate_item (estimate_id);
