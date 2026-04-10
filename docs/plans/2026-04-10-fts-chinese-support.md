# FTS Chinese Language Support Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix the full-text search system to properly support Chinese text, including updating the database index, rewriting JPQL queries to use PostgreSQL native FTS, and adding language-aware search configuration.

**Architecture:** Use PostgreSQL `zhparser` extension for Chinese text segmentation. Create a language-aware FTS function that selects the correct text search configuration based on content language. Replace JPQL LIKE queries with native SQL using `tsvector`/`tsquery` operators to leverage the GIN index.

**Tech Stack:** PostgreSQL zhparser extension, Spring Data JPA native queries, Flyway migrations

**Branch:** `feature/fts-chinese-support`

---

## Phase A: Database Migration

### Task 1: Create migration for Chinese FTS support

**Files:**
- Create: `javainfohunter-ai-service/src/main/resources/db/migration/V3__add_chinese_fts_support.sql`

**Step 1:** Write migration:

```sql
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
        v_config := EXISTS(SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese_zh')
            THEN 'chinese_zh'
            ELSE 'simple';
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
        v_config := EXISTS(SELECT 1 FROM pg_ts_config WHERE cfgname = 'chinese_zh')
            THEN 'chinese_zh'
            ELSE 'simple';
    ELSE
        v_config := 'english';
    END IF;

    -- Parse query
    EXECUTE format('SELECT plainto_tsquery(%L, %L)', v_config, p_query) INTO v_tsquery;

    RETURN QUERY
    SELECT
        n.id,
        n.title,
        n.summary,
        n.category,
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
```

**Step 2:** Commit
```bash
git commit -m "feat: add Chinese FTS support with language-aware tsvector and search function"
```

---

## Phase B: Backend - Rewrite Search Queries

### Task 2: Add native query for FTS search in NewsRepository

**Files:**
- Modify: `javainfohunter-ai-service/src/main/java/com/ron/javainfohunter/repository/NewsRepository.java`

**Step 1:** Add native query method using the search_news function:

```java
/**
 * Full-text search using PostgreSQL tsvector/tsquery with Chinese support.
 * Uses the search_news database function which handles language-aware search.
 *
 * @param query Search term
 * @param language Content language filter (null for all)
 * @param pageable Pagination
 * @return Page of matching news IDs with scores
 */
@Query(value = "SELECT n.* FROM search_news(:query, :language, :limit, :offset) AS n",
       nativeQuery = true)
List<Object[]> searchNewsNative(
    @Param("query") String query,
    @Param("language") String language,
    @Param("limit") int limit,
    @Param("offset") int offset
);

/**
 * Count results for full-text search
 */
@Query(value = "SELECT COUNT(*) FROM news n WHERE n.is_published = true AND n.fts_vector @@ plainto_tsquery('simple', :query)",
       nativeQuery = true)
long countSearchResults(@Param("query") String query);
```

**Step 2:** Commit
```bash
git commit -m "feat: add native FTS queries using PostgreSQL tsvector in NewsRepository"
```

### Task 3: Create NewsSearchResult projection and update service

**Files:**
- Create: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/dto/response/NewsSearchResponse.java`
- Modify: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/service/impl/NewsServiceImpl.java`
- Modify: `javainfohunter-api/src/main/java/com/ron/javainfohunter/api/controller/NewsController.java`

**Step 1:** Add `searchNews` method to NewsServiceImpl that calls the native FTS query:

```java
@Override
public Page<NewsResponse> searchNews(String query, String language, Pageable pageable) {
    log.debug("Searching news with FTS query: {}, language: {}", query, language);

    int limit = pageable.getPageSize();
    int offset = (int) pageable.getOffset();

    List<Object[]> results = newsRepository.searchNewsNative(query, language, limit, offset);
    long total = newsRepository.countSearchResults(query);

    List<NewsResponse> content = results.stream()
        .map(row -> {
            Long id = ((Number) row[0]).longValue();
            return newsRepository.findByIdWithDetails(id)
                .map(this::toResponse)
                .orElse(null);
        })
        .filter(Objects::nonNull)
        .toList();

    return new PageImpl<>(content, pageable, total);
}
```

**Step 2:** Update NewsController search endpoint to accept language parameter:

```java
@GetMapping("/search")
@Operation(summary = "Search news", description = "Full-text search with Chinese support")
public ResponseEntity<ApiResponse<Page<NewsResponse>>> searchNews(
        @RequestParam String query,
        @RequestParam(required = false) String language,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") @Max(100) int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<NewsResponse> results = newsService.searchNews(query, language, pageable);
    return ResponseEntity.ok(ApiResponse.success(results));
}
```

**Step 3:** Commit
```bash
git commit -m "feat: update search endpoint with language parameter and native FTS"
```

---

## Phase C: Frontend - Update Search

### Task 4: Update frontend search to support language

**Files:**
- Modify: `src/shared/api/endpoints.ts` (frontend)
- Modify: `src/shared/hooks/useNews.ts`

**Step 1:** Update newsApi.search to include language parameter:
```typescript
search: (query: string, language?: string, page = 0, size = 20) =>
  apiClient.get('/news/search', {
    params: { query, language, page, size },
  }),
```

**Step 2:** Commit
```bash
git commit -m "feat: add language parameter to frontend search API"
```

---

## Phase D: Documentation & Verification

### Task 5: Update documentation

**Files:**
- Modify: `docs/wiki/backend-database.md`
- Modify: `docs/wiki/backend-api-reference.md`

**Step 1:** Update FTS documentation to reflect Chinese support, new tsvector column, and search_news function.
**Step 2:** Update API reference with language parameter on search endpoint.

**Step 3:** Commit
```bash
git commit -m "docs: update FTS documentation for Chinese language support"
```

### Task 6: Build verification

**Step 1:** Run backend build: `mvnw.cmd clean package`
**Step 2:** Run frontend build: `npm run build`
**Step 3:** Run backend tests: `mvnw.cmd test -pl javainfohunter-ai-service`
**Step 4:** Merge branch to main

---

## Important Notes

1. **zhparser availability**: The migration uses a `DO $$` block to check if zhparser is available. If not, it falls back to `'simple'` configuration which still provides basic Chinese tokenization (character-level matching).

2. **To install zhparser on the PostgreSQL server:**
   ```bash
   # Install via pgxn or compile from source
   pgxn install zhparser
   # Or on Ubuntu/Debian:
   apt-get install postgresql-16-zhparser
   ```

3. **Backward compatibility**: The old `fullTextSearch` JPQL method still works (uses LIKE). The new native FTS method is used by the search endpoint going forward. Both can coexist.

4. **Performance**: The GIN index on `fts_vector` column will be used by the native queries. The `LIKE '%term%'` queries in the old JPQL methods will NOT use the GIN index and will continue to do sequential scans. Over time, the old methods should be deprecated in favor of the native FTS.
