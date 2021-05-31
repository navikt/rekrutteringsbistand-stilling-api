CREATE TABLE inkluderingsmuligheter (
    id BIGSERIAL PRIMARY KEY,
    stillingsid VARCHAR(36) NOT NULL,
    tilretteleggingmuligheter TEXT,
    virkemidler TEXT,
    prioriterte_maalgrupper TEXT,
    statlig_inkluderingsdugnad BOOLEAN,
    rad_opprettet TIMESTAMP(6) DEFAULT current_timestamp
)
