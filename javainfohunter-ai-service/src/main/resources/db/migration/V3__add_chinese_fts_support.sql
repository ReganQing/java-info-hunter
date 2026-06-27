-- ============================================================
-- Chinese Full-Text Search Support
-- ============================================================
-- This migration adds Chinese text segmentation support using
-- the zhparser extension and creates language-aware FTS indexes.
-- ============================================================

-- 1. Create zhparser extension (requires superuser or extension installed)
-- If zhparser is not available, fallback to 'simple' configuration
-- which works for Chinese as character-by-character matching

DO $$
BEGIN
    -- Try to create zhparser extension
    BEGIN
        CREATE EXTENSION IF NOT EXISTS zhparser SCHEMA public;
        RAISE NOTICE 'zhparser extension created successfully';

        -- Create Chinese text search configuration
        DROP TEXT SEARCH CONFIGURATION IF EXISTS chinese_zh CASCADE;
        CREATE TEXT SEARCH CONFIGURATION chinese_zh (PARSER = zhparser);
        ALTER TEXT SEARCH CONFIGURATION chinese_zh ADD MAPPING FOR n,v,a,i,e,l WITH simple;

    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'zhparser extension not available, using simple configuration for Chinese';
    END;
END $$;

-- 2. Drop the old English-only FTS index
DROP INDEX IF EXISTS idx_news_fulltext;

-- 3. Add a generated tsvector column that is language-aware
-- Uses 'simple' for Chinese (zh) and 'english' for everything else
-- 'simple' doesn't do stemming but tokenizes CJK characters
ALTER TABLE news ADD COLUMN IF NOT EXISTS fts_vector tsvector;

-- 4. Create a function to generate the tsvector based on language
CREATE OR REPLACE FUNCTION news_tsvector(
    p_title TEXT,
    p_summary TEXT,
    p_full_content TEXT,
    p_language TEXT
) RETURNS tsvector AS $$
DECLARE
    v_text TEXT;
    v_config TEXT;
    v_tsvector tsvector;
BEGIN
    v_text := COALESCE(p_title, '') || ' ' ||
              COALESCE(p_summary, '') || ' ' ||
              COALESCE(p_full_content, '');

    -- Select config based on language
    IF p_language = 'zh' THEN
        -- Use chinese_zh if zhparser is available, otherwise simple
        IF EXISTS(SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese_zh') THEN
            v_config := 'chinese_zh';
        ELSE
            v_config := 'simple';
        END IF;
    ELSE
        v_config := 'english';
    END IF;

    EXECUTE format('SELECT to_tsvector(%L, %L)', v_config, v_text) INTO v_tsvector;
    RETURN v_tsvector;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 5. Update existing rows with tsvector
UPDATE news SET fts_vector = news_tsvector(title, summary, full_content, language)
WHERE fts_vector IS NULL;

-- 6. Create GIN index on the generated tsvector column
CREATE INDEX idx_news_fts_vector ON news USING GIN (fts_vector);

-- 7. Add trigger to auto-update fts_vector on INSERT/UPDATE
CREATE OR REPLACE FUNCTION update_news_fts_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fts_vector = news_tsvector(
        NEW.title, NEW.summary, NEW.full_content, NEW.language
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_news_fts_vector ON news;
CREATE TRIGGER trg_update_news_fts_vector
    BEFORE INSERT OR UPDATE OF title, summary, full_content, language
    ON news
    FOR EACH ROW
    EXECUTE FUNCTION update_news_fts_vector();

-- 8. Create a convenient search function
CREATE OR REPLACE FUNCTION search_news(
    p_query TEXT,
    p_language TEXT DEFAULT NULL,
    p_limit INTEGER DEFAULT 20,
    p_offset INTEGER DEFAULT 0
) RETURNS TABLE(
    id BIGINT,
    title TEXT,
    summary TEXT,
    category VARCHAR,
    published_at TIMESTAMP WITH TIME ZONE,
    rank REAL
) AS $$
DECLARE
    v_config TEXT;
    v_tsquery tsquery;
BEGIN
    -- Determine config
    IF p_language = 'zh' THEN
        IF EXISTS(SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese_zh') THEN
            v_config := 'chinese_zh';
        ELSE
            v_config := 'simple';
        END IF;
    ELSE
        v_config := 'english';
    END IF;

    -- Parse query
    EXECUTE format('SELECT plainto_tsquery(%L, %L)', v_config, p_query) INTO v_tsquery;

    RETURN QUERY
    SELECT
        n.id,
        n.title::TEXT,
        n.summary::TEXT,
        n.category::VARCHAR,
        n.published_at,
        ts_rank(n.fts_vector, v_tsquery) AS rank
    FROM news n
    WHERE n.is_published = true
      AND n.fts_vector @@ v_tsquery
      AND (p_language IS NULL OR n.language = p_language)
    ORDER BY rank DESC, n.published_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- 9. Create a count function for FTS results
CREATE OR REPLACE FUNCTION count_search_news(
    p_query TEXT,
    p_language TEXT DEFAULT NULL
) RETURNS BIGINT AS $$
DECLARE
    v_config TEXT;
    v_tsquery tsquery;
    v_count BIGINT;
BEGIN
    -- Determine config (same logic as search_news)
    IF p_language = 'zh' THEN
        IF EXISTS(SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese_zh') THEN
            v_config := 'chinese_zh';
        ELSE
            v_config := 'simple';
        END IF;
    ELSE
        v_config := 'english';
    END IF;

    -- Parse query
    EXECUTE format('SELECT plainto_tsquery(%L, %L)', v_config, p_query) INTO v_tsquery;

    SELECT COUNT(*) INTO v_count
    FROM news n
    WHERE n.is_published = true
      AND n.fts_vector @@ v_tsquery
      AND (p_language IS NULL OR n.language = p_language);

    RETURN v_count;
END;
$$ LANGUAGE plpgsql;
