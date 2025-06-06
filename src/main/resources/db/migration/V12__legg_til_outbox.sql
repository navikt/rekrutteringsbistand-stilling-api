CREATE TABLE STILLING_OUTBOX (
    id BIGSERIAL PRIMARY KEY,
    stillingsid UUID NOT NULL,
    event_name TEXT NOT NULL,
    opprettet TIMESTAMP DEFAULT current_timestamp NOT NULL,
    prosessert TIMESTAMP
);

CREATE INDEX IF NOT EXISTS STILLING_OUTBOX_PROSESSERT_IDX ON STILLING_OUTBOX(prosessert);
CREATE INDEX IF NOT EXISTS STILLING_OUTBOX_OPPRETTET_IDX ON STILLING_OUTBOX(opprettet);
