CREATE TABLE SKJULESTATUS (
    db_id bigserial primary key,
    stillingsid varchar(36) unique not null,
    stilling_stanset_tidspunkt timestamp with time zone,
    utført_markert_for_skjuling timestamp with time zone,
    utført_slette_elasticsearch timestamp with time zone,
    utført_skjule_kandidatliste timestamp with time zone,
)