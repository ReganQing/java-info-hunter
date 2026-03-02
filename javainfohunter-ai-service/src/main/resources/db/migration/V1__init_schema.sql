-- ============================================================================
-- JavaInfoHunter Database Schema
-- ============================================================================
-- Database: PostgreSQL 16+
-- Migration: V1 - Initial Schema
-- Description: RSS subscription, content collection, and AI agent execution tracking
-- ============================================================================

-- ============================================================================
-- Extension: pgvector for vector similarity search
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================================
-- Table: rss_sources (RSS 订阅源表)
-- ============================================================================
-- Purpose: Store RSS subscription source information
-- ============================================================================
CREATE TABLE rss_sources (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,

    -- Basic Information
    name VARCHAR(255) NOT NULL,
    url VARCHAR(2048) NOT NULL UNIQUE,
    description TEXT,

    -- Categorization
    category VARCHAR(100),
    tags TEXT[], -- Array of tags for flexible categorization

    -- Configuration
    crawl_interval_seconds INTEGER NOT NULL DEFAULT 3600, -- Default: 1 hour
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Retry Configuration
    max_retries INTEGER NOT NULL DEFAULT 3,
    retry_backoff_seconds INTEGER NOT NULL DEFAULT 60,

    -- Metadata
    language VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(50) DEFAULT 'UTC',

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_crawled_at TIMESTAMP WITH TIME ZONE,

    -- Statistics
    total_articles BIGINT NOT NULL DEFAULT 0,
    failed_crawls BIGINT NOT NULL DEFAULT 0,

    -- Constraints
    CONSTRAINT chk_crawl_interval_positive CHECK (crawl_interval_seconds > 0),
    CONSTRAINT chk_max_retries_positive CHECK (max_retries >= 0),
    CONSTRAINT chk_retry_backoff_positive CHECK (retry_backoff_seconds >= 0)
);

-- Indexes for rss_sources
CREATE INDEX idx_rss_sources_active ON rss_sources(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_rss_sources_category ON rss_sources(category);
CREATE INDEX idx_rss_sources_tags ON rss_sources USING GIN(tags);
CREATE INDEX idx_rss_sources_last_crawled ON rss_sources(last_crawled_at);

-- Comments
COMMENT ON TABLE rss_sources IS 'RSS subscription sources for content collection';
COMMENT ON COLUMN rss_sources.url IS 'RSS feed URL (unique constraint prevents duplicates)';
COMMENT ON COLUMN rss_sources.tags IS 'Array of tags for flexible categorization and filtering';
COMMENT ON COLUMN rss_sources.crawl_interval_seconds IS 'Crawling frequency in seconds (minimum 60s recommended)';
COMMENT ON COLUMN rss_sources.is_active IS 'Whether this source is actively being crawled';

-- ============================================================================
-- Table: raw_content (原始内容表)
-- ============================================================================
-- Purpose: Store raw content collected from RSS feeds before AI processing
-- ============================================================================
CREATE TABLE raw_content (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign Key to RSS Source
    rss_source_id BIGINT NOT NULL REFERENCES rss_sources(id) ON DELETE CASCADE,

    -- Content Identification
    guid VARCHAR(255) NOT NULL, -- Unique identifier from RSS feed
    title VARCHAR(500) NOT NULL,
    link VARCHAR(2048),

    -- Raw Content
    raw_content TEXT NOT NULL, -- Original HTML or plain text
    content_hash VARCHAR(64) NOT NULL, -- SHA-256 hash for deduplication

    -- Metadata
    author VARCHAR(255),
    publish_date TIMESTAMP WITH TIME ZONE,
    crawl_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Processing Status
    processing_status VARCHAR(20) NOT NULL DEFAULT 'pending', -- pending, processing, completed, failed
    error_message TEXT,

    -- AI Processing Metadata
    embedding vector(1536), -- OpenAI embedding dimension (adjust if using different model)
    embedding_generated_at TIMESTAMP WITH TIME ZONE,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_processing_status CHECK (processing_status IN ('pending', 'processing', 'completed', 'failed')),
    CONSTRAINT uq_raw_content_guid_source UNIQUE (guid, rss_source_id), -- Prevent duplicate articles from same source
    CONSTRAINT uq_raw_content_hash UNIQUE (content_hash) -- Prevent duplicate content across all sources
);

-- Indexes for raw_content
CREATE INDEX idx_raw_content_rss_source ON raw_content(rss_source_id);
CREATE INDEX idx_raw_content_status ON raw_content(processing_status);
CREATE INDEX idx_raw_content_publish_date ON raw_content(publish_date DESC);
CREATE INDEX idx_raw_content_crawl_date ON raw_content(crawl_date DESC);
CREATE INDEX idx_raw_content_embedding ON raw_content USING ivfflat(embedding vector_cosine_ops) WITH (lists = 100);

-- Comments
COMMENT ON TABLE raw_content IS 'Raw content collected from RSS feeds before AI processing';
COMMENT ON COLUMN raw_content.guid IS 'Unique identifier from RSS feed (unique per source)';
COMMENT ON COLUMN raw_content.content_hash IS 'SHA-256 hash for global deduplication';
COMMENT ON COLUMN raw_content.embedding IS 'Vector embedding for semantic search and similarity';
COMMENT ON COLUMN raw_content.processing_status IS 'Processing status: pending, processing, completed, failed';

-- ============================================================================
-- Table: news (处理后的新闻表)
-- ============================================================================
-- Purpose: Store processed and enriched news articles after AI analysis
-- ============================================================================
CREATE TABLE news (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,

    -- Foreign Key to Raw Content
    raw_content_id BIGINT NOT NULL UNIQUE REFERENCES raw_content(id) ON DELETE CASCADE,

    -- Enriched Content
    title VARCHAR(500) NOT NULL,
    summary TEXT NOT NULL, -- AI-generated summary
    full_content TEXT, -- Cleaned and structured content

    -- AI-Generated Metadata
    topics TEXT[], -- AI-extracted topics
    keywords TEXT[], -- AI-extracted keywords
    sentiment VARCHAR(20), -- positive, neutral, negative
    sentiment_score DECIMAL(3,2), -- -1.0 to 1.0
    importance_score DECIMAL(3,2), -- 0.0 to 1.0 (AI-rated importance)

    -- Categorization
    category VARCHAR(100), -- AI-assigned category
    tags TEXT[], -- AI-assigned tags

    -- Content Analysis
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    reading_time_minutes INTEGER, -- Estimated reading time

    -- SEO and Discovery
    slug VARCHAR(255) UNIQUE,
    featured_image_url VARCHAR(2048),

    -- Engagement Metrics
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count INTEGER NOT NULL DEFAULT 0,
    share_count INTEGER NOT NULL DEFAULT 0,

    -- Moderation
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP WITH TIME ZONE,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_sentiment_values CHECK (sentiment IN ('positive', 'neutral', 'negative')),
    CONSTRAINT chk_sentiment_score_range CHECK (sentiment_score BETWEEN -1.0 AND 1.0),
    CONSTRAINT chk_importance_score_range CHECK (importance_score BETWEEN 0.0 AND 1.0),
    CONSTRAINT chk_reading_time_positive CHECK (reading_time_minutes IS NULL OR reading_time_minutes > 0)
);

-- Indexes for news
CREATE INDEX idx_news_raw_content ON news(raw_content_id);
CREATE INDEX idx_news_published ON news(is_published, published_at DESC);
CREATE INDEX idx_news_category ON news(category);
CREATE INDEX idx_news_tags ON news USING GIN(tags);
CREATE INDEX idx_news_topics ON news USING GIN(topics);
CREATE INDEX idx_news_keywords ON news USING GIN(keywords);
CREATE INDEX idx_news_sentiment ON news(sentiment);
CREATE INDEX idx_news_importance ON news(importance_score DESC);
CREATE INDEX idx_news_slug ON news(slug);
CREATE INDEX idx_news_created_at ON news(created_at DESC);

-- Full-text search index
CREATE INDEX idx_news_fulltext ON news USING GIN(to_tsvector('english', title || ' ' || COALESCE(summary, '') || ' ' || COALESCE(full_content, '')));

-- Comments
COMMENT ON TABLE news IS 'Processed and enriched news articles after AI analysis';
COMMENT ON COLUMN news.raw_content_id IS 'Reference to original raw content (one-to-one)';
COMMENT ON COLUMN news.summary IS 'AI-generated summary of the article';
COMMENT ON COLUMN news.sentiment_score IS 'Sentiment score from -1.0 (negative) to 1.0 (positive)';
COMMENT ON COLUMN news.importance_score IS 'AI-rated importance score from 0.0 to 1.0';
COMMENT ON COLUMN news.reading_time_minutes IS 'Estimated reading time in minutes';

-- ============================================================================
-- Table: agent_executions (Agent 执行记录表)
-- ============================================================================
-- Purpose: Track AI agent executions for debugging, analytics, and optimization
-- ============================================================================
CREATE TABLE agent_executions (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,

    -- Agent Information
    agent_id VARCHAR(100) NOT NULL, -- Registered agent ID (e.g., 'crawler-agent')
    agent_type VARCHAR(50) NOT NULL, -- BaseAgent, ReActAgent, ToolCallAgent
    agent_name VARCHAR(255), -- Human-readable name

    -- Execution Context
    execution_id VARCHAR(100) NOT NULL UNIQUE, -- Unique execution identifier (UUID)
    task_type VARCHAR(100), -- Type of task (crawl, analyze, summarize, etc.)
    coordination_pattern VARCHAR(50), -- chain, parallel, master-worker, standalone

    -- Input/Output
    input_data JSONB, -- Input parameters and context
    output_data JSONB, -- Agent output and results
    error_trace TEXT, -- Error stack trace if failed

    -- Execution Metrics
    status VARCHAR(20) NOT NULL, -- running, completed, failed, cancelled
    start_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP WITH TIME ZONE,
    duration_milliseconds INTEGER,

    -- Performance Metrics
    total_steps INTEGER,
    tokens_used INTEGER,
    estimated_cost_usd DECIMAL(10,4),

    -- Tool Usage (for ToolCallAgent)
    tools_used TEXT[], -- List of tools used during execution
    tool_call_count INTEGER,

    -- References to Related Entities
    raw_content_id BIGINT REFERENCES raw_content(id) ON DELETE SET NULL,
    news_id BIGINT REFERENCES news(id) ON DELETE SET NULL,

    -- Metadata
    parent_execution_id VARCHAR(100), -- For chained or coordinated executions
    correlation_id VARCHAR(100), -- For tracking related executions across agents
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_agent_execution_status CHECK (status IN ('running', 'completed', 'failed', 'cancelled')),
    CONSTRAINT chk_duration_positive CHECK (duration_milliseconds IS NULL OR duration_milliseconds >= 0),
    CONSTRAINT chk_tokens_positive CHECK (tokens_used IS NULL OR tokens_used >= 0),
    CONSTRAINT chk_retry_count_valid CHECK (retry_count >= 0 AND retry_count <= max_retries)
);

-- Indexes for agent_executions
CREATE INDEX idx_agent_executions_agent_id ON agent_executions(agent_id);
CREATE INDEX idx_agent_executions_agent_type ON agent_executions(agent_type);
CREATE INDEX idx_agent_executions_execution_id ON agent_executions(execution_id);
CREATE INDEX idx_agent_executions_status ON agent_executions(status);
CREATE INDEX idx_agent_executions_start_time ON agent_executions(start_time DESC);
CREATE INDEX idx_agent_executions_task_type ON agent_executions(task_type);
CREATE INDEX idx_agent_executions_coordination_pattern ON agent_executions(coordination_pattern);
CREATE INDEX idx_agent_executions_correlation_id ON agent_executions(correlation_id);
CREATE INDEX idx_agent_executions_parent_execution_id ON agent_executions(parent_execution_id);
CREATE INDEX idx_agent_executions_raw_content_id ON agent_executions(raw_content_id);
CREATE INDEX idx_agent_executions_news_id ON agent_executions(news_id);

-- Composite index for common queries
CREATE INDEX idx_agent_executions_agent_time ON agent_executions(agent_id, start_time DESC);
CREATE INDEX idx_agent_executions_status_time ON agent_executions(status, start_time DESC);

-- Comments
COMMENT ON TABLE agent_executions IS 'AI agent execution records for debugging and analytics';
COMMENT ON COLUMN agent_executions.agent_id IS 'Registered agent identifier (e.g., crawler-agent, analysis-agent)';
COMMENT ON COLUMN agent_executions.execution_id IS 'Unique execution identifier (UUID format)';
COMMENT ON COLUMN agent_executions.coordination_pattern IS 'Chain, parallel, master-worker, or standalone execution';
COMMENT ON COLUMN agent_executions.input_data IS 'Input parameters and context (JSONB format)';
COMMENT ON COLUMN agent_executions.output_data IS 'Agent output and results (JSONB format)';
COMMENT ON COLUMN agent_executions.estimated_cost_usd IS 'Estimated LLM API cost in USD';
COMMENT ON COLUMN agent_executions.correlation_id IS 'For tracking related executions across coordinated agents';

-- ============================================================================
-- Functions: Automatic updated_at timestamp
-- ============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to all tables
CREATE TRIGGER update_rss_sources_updated_at
    BEFORE UPDATE ON rss_sources
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_raw_content_updated_at
    BEFORE UPDATE ON raw_content
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_news_updated_at
    BEFORE UPDATE ON news
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_agent_executions_updated_at
    BEFORE UPDATE ON agent_executions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Initial Data: Sample RSS Sources (for development/testing)
-- ============================================================================
INSERT INTO rss_sources (name, url, description, category, tags, crawl_interval_seconds) VALUES
('TechCrunch', 'https://techcrunch.com/feed/', 'Technology news and analysis', 'technology', ARRAY['tech', 'startups', 'venture-capital'], 3600),
('Hacker News', 'https://news.ycombinator.com/rss', 'Hacker News front page', 'technology', ARRAY['hacking', 'programming', 'startups'], 1800),
('Ars Technica', 'https://feeds.arstechnica.com/arstechnica/index', 'Technology news and policy', 'technology', ARRAY['tech', 'science', 'policy'], 3600),
('BBC Technology', 'http://feeds.bbci.co.uk/news/technology/rss.xml', 'BBC Technology news', 'technology', ARRAY['tech', 'world-news'], 3600),
('Reuters Technology', 'https://www.reuters.com/agency/rss/technologyNews', 'Reuters Technology news', 'technology', ARRAY['tech', 'business'], 3600);

-- ============================================================================
-- Views: Common Query Patterns
-- ============================================================================

-- View: Active RSS sources with statistics
CREATE OR REPLACE VIEW active_rss_sources AS
SELECT
    id,
    name,
    url,
    category,
    tags,
    crawl_interval_seconds,
    last_crawled_at,
    total_articles,
    failed_crawls,
    ROUND(100.0 * failed_crawls / NULLIF(total_articles, 0), 2) AS failure_rate,
    created_at
FROM rss_sources
WHERE is_active = TRUE
ORDER BY last_crawled_at ASC NULLS LAST;

COMMENT ON VIEW active_rss_sources IS 'Active RSS sources with crawling statistics';

-- View: Pending content for processing
CREATE OR REPLACE VIEW pending_content AS
SELECT
    rc.id,
    rc.rss_source_id,
    rs.name AS source_name,
    rc.title,
    rc.link,
    rc.publish_date,
    rc.crawl_date,
    rc.processing_status
FROM raw_content rc
JOIN rss_sources rs ON rc.rss_source_id = rs.id
WHERE rc.processing_status = 'pending'
ORDER BY rc.crawl_date ASC;

COMMENT ON VIEW pending_content IS 'Raw content waiting for AI processing';

-- View: Published news with metadata
CREATE OR REPLACE VIEW published_news AS
SELECT
    n.id,
    n.title,
    n.summary,
    n.category,
    n.tags,
    n.topics,
    n.keywords,
    n.sentiment,
    n.importance_score,
    n.view_count,
    n.like_count,
    n.published_at,
    rs.name AS source_name,
    rc.link AS original_link,
    rc.author AS original_author,
    rc.publish_date AS original_publish_date
FROM news n
JOIN raw_content rc ON n.raw_content_id = rc.id
JOIN rss_sources rs ON rc.rss_source_id = rs.id
WHERE n.is_published = TRUE
ORDER BY n.published_at DESC;

COMMENT ON VIEW published_news IS 'Published news articles with full metadata';

-- View: Agent execution statistics
CREATE OR REPLACE VIEW agent_execution_stats AS
SELECT
    agent_id,
    agent_type,
    COUNT(*) AS total_executions,
    COUNT(*) FILTER (WHERE status = 'completed') AS completed_count,
    COUNT(*) FILTER (WHERE status = 'failed') AS failed_count,
    COUNT(*) FILTER (WHERE status = 'running') AS running_count,
    ROUND(AVG(duration_milliseconds) / 1000.0, 2) AS avg_duration_seconds,
    ROUND(SUM(tokens_used), 0) AS total_tokens,
    ROUND(SUM(estimated_cost_usd), 4) AS total_cost_usd,
    ROUND(100.0 * COUNT(*) FILTER (WHERE status = 'failed') / NULLIF(COUNT(*), 0), 2) AS failure_rate
FROM agent_executions
WHERE start_time >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY agent_id, agent_type
ORDER BY total_executions DESC;

COMMENT ON VIEW agent_execution_stats IS 'Agent execution statistics for the last 7 days';

-- ============================================================================
-- End of Migration V1
-- ============================================================================
