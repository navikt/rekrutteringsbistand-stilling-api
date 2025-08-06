DO
$$
    BEGIN
        IF EXISTS(SELECT * FROM pg_roles WHERE rolname = 'bigquery_datastream') THEN
            ALTER USER "rekrutteringsbistand-stilling-api" WITH REPLICATION;
            CREATE PUBLICATION "ds_publication" FOR ALL TABLES;

            ALTER DEFAULT PRIVILEGES IN SCHEMA PUBLIC GRANT SELECT ON TABLES TO "bigquery_datastream";
            GRANT SELECT ON ALL TABLES IN SCHEMA PUBLIC TO "bigquery_datastream";
            ALTER USER "bigquery_datastream" WITH REPLICATION;
        END IF;
    END
$$;

DO
$$
    BEGIN
        IF EXISTS(SELECT * FROM pg_roles WHERE rolname = 'bigquery_datastream') THEN
            PERFORM PG_CREATE_LOGICAL_REPLICATION_SLOT('ds_replication', 'pgoutput');
        END IF;
    END
$$
