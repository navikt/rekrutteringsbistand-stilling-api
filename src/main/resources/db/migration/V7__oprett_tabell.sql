CREATE TABLE intern_stilling (
    id SERIAL CONSTRAINT PK_SILLING_INTERN PRIMARY KEY,
    stillingsid UUID NOT NULL UNIQUE,
    innhold JSONB NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp,
    opprettet_av VARCHAR(255) NOT NULL,
    sist_endret TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp,
    sist_endret_av VARCHAR(255) NOT NULL
)
