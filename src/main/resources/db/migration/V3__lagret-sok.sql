CREATE TABLE lagret_sok (
    id BIGSERIAL PRIMARY KEY,
    nav_ident TEXT NOT NULL,
    sok TEXT NOT NULL,
    tidspunkt TIMESTAMP NOT NULL
)
