CREATE TABLE SKJULESTATUS (
    db_id bigserial primary key,
    stilling_referanse varchar(36) unique not null,
    grunnlag_for_skjuling timestamp with time zone,
    utført_markere_for_skjuling timestamp with time zone,
    utført_slette_elasticsearch timestamp with time zone,
    utført_skjule_kandidatliste timestamp with time zone
)
