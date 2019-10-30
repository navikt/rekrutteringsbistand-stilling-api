CREATE TABLE REKRUTTERINGSBISTAND (
    id SERIAL PRIMARY KEY,
    rekruttering_uuid VARCHAR(36) UNIQUE,
    stilling_uuid VARCHAR(36) UNIQUE,
    overfoert_til VARCHAR(7)
);
