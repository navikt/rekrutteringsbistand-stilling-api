ALTER TABLE DIREKTEMELDT_STILLING
    ADD VERSJON INTEGER NOT NULL DEFAULT 1,
    ADD PUBLISERT TIMESTAMP WITH TIME ZONE,
    ADD UTLOPSDATO TIMESTAMP WITH TIME ZONE,
    ADD PUBLISERT_AV_ADMIN TEXT,
    ADD ADMIN_STATUS TEXT,
    ALTER COLUMN OPPRETTET_AV TYPE TEXT,
    ALTER COLUMN SIST_ENDRET_AV TYPE TEXT;
