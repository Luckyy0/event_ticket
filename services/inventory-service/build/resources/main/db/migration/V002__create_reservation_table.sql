CREATE TABLE reservations (
    id             UUID PRIMARY KEY,
    ticket_type_id UUID        NOT NULL,
    show_id        UUID        NOT NULL,
    user_id        UUID        NOT NULL,
    request_id     UUID        NOT NULL,
    quantity       INT         NOT NULL CHECK (quantity > 0),
    status         VARCHAR(20) NOT NULL DEFAULT 'HELD',
    expires_at     TIMESTAMPTZ NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at   TIMESTAMPTZ,
    released_at    TIMESTAMPTZ,
    version        INT         NOT NULL DEFAULT 0,

    CONSTRAINT uq_reservation_request_id UNIQUE (request_id),
    CONSTRAINT ck_reservation_status
        CHECK (status IN ('PENDING','HELD','CONFIRMED','RELEASED','EXPIRED','CANCELLED'))
);

CREATE INDEX idx_reservation_user_id ON reservations (user_id);
CREATE INDEX idx_reservation_show_ticket ON reservations (show_id, ticket_type_id);
CREATE INDEX idx_reservation_status_expires ON reservations (status, expires_at)
    WHERE status = 'HELD';
CREATE INDEX idx_reservation_request_id ON reservations (request_id);
