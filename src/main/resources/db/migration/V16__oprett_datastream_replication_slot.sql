DO
$$
    BEGIN
        IF EXISTS(SELECT * FROM pg_roles WHERE rolname = 'bigquery_datastream') THEN
            PERFORM PG_CREATE_LOGICAL_REPLICATION_SLOT('toi_rekrutteringsbistand_stilling_api_replication', 'pgoutput');
        END IF;
    END
$$
