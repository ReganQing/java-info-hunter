-- Initialize PostgreSQL database for JavaInfoHunter
-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create database user if not exists (for development)
-- DO NOT use this in production!
-- CREATE USER IF NOT EXISTS javainfohunter WITH PASSWORD 'javainfohunter';
-- GRANT ALL PRIVILEGES ON DATABASE javainfohunter TO javainfohunter;

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'Database initialized successfully';
    RAISE NOTICE 'pgvector extension version: %', pg_extension.extversion FROM pg_extension WHERE pg_extension.extname = 'vector';
END $$;
