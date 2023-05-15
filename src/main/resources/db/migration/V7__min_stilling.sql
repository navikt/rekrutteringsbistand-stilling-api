CREATE TABLE min_stilling (
    id BIGSERIAL PRIMARY KEY,
    stillingsid UUID NOT NULL,
    tittel TEXT NOT NULL,
    sist_endret TIMESTAMP NOT NULL,
    annonsenr BIGSERIAL NOT NULL,
    arbeidsgiver_navn TEXT NOT NULL,
    utløpsdato TIMESTAMP NOT NULL,
    status TEXT NOT NULL,
    eier_nav_ident TEXT NOT NULL
);

CREATE INDEX eier_nav_ident_idx
ON min_stilling (eier_nav_ident, stillingsid);
