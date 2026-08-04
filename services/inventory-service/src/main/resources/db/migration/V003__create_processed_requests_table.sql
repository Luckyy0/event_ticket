CREATE TABLE processed_requests (
    request_id     UUID PRIMARY KEY,
    reservation_id UUID,
    processed_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    result_status  VARCHAR(20) NOT NULL
);
