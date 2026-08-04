CREATE TABLE inventories (
    id                 UUID PRIMARY KEY,
    show_id            UUID        NOT NULL,
    ticket_type_id     UUID        NOT NULL,
    total_quantity     INT         NOT NULL CHECK (total_quantity >= 0),
    available_quantity INT         NOT NULL CHECK (available_quantity >= 0),
    reserved_quantity  INT         NOT NULL CHECK (reserved_quantity >= 0),
    sold_quantity      INT         NOT NULL CHECK (sold_quantity >= 0),
    version            INT         NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_inventory_show_ticket UNIQUE (show_id, ticket_type_id),
    CONSTRAINT ck_inventory_quantity_sum
        CHECK (total_quantity = available_quantity + reserved_quantity + sold_quantity)
);

CREATE INDEX idx_inventory_show_id ON inventories (show_id);
CREATE INDEX idx_inventory_ticket_type ON inventories (ticket_type_id);
