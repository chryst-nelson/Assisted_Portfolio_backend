-- Runs once, on first container init (mounted into /docker-entrypoint-initdb.d),
-- as the Postgres superuser — CREATE EXTENSION needs privileges the app's own
-- DB user deliberately doesn't have.
CREATE EXTENSION IF NOT EXISTS vector;

-- Required by Spring AI's PgVectorStore auto-schema-init (M7): hstore and
-- uuid-ossp back its default vector_store table (uuid_generate_v4() for the
-- primary key). If PgVectorStore tried to create these itself using the
-- app's own connection, it would fail with a permissions error — hence
-- creating them here instead, same reasoning as the vector extension above.
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
