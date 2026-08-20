-- Runs once, on first container init (mounted into /docker-entrypoint-initdb.d).
-- Required for the vector_documents table introduced in M7.
CREATE EXTENSION IF NOT EXISTS vector;
