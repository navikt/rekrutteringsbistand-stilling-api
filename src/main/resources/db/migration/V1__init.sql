CREATE TABLE REKRUTTERINGSBISTAND (
    id SERIAL PRIMARY KEY,
    rekruttering_uuid VARCHAR(36) UNIQUE,
    stilling_uuid VARCHAR(36) UNIQUE,
    eier_ident VARCHAR(25),
    eier_navn VARCHAR(255)
);
