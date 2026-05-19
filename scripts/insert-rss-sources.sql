-- Insert common RSS sources
-- Chinese encoding fix: use file-based import

INSERT INTO rss_sources (name, url, category, description, is_active, crawl_interval_seconds, max_retries, retry_backoff_seconds, failed_crawls, total_articles, language, timezone, tags, created_at, updated_at) VALUES
-- Chinese Tech
('36Kr', 'https://36kr.com/feed', 'Technology', '36Kr - Startup and Innovation', true, 1800, 3, 60, 0, 0, 'zh', 'Asia/Shanghai', ARRAY['tech','startup','invest'], now(), now()),
('SSPai', 'https://sspai.com/feed', 'Technology', 'SSPai - Productivity and Digital Life', true, 3600, 3, 60, 0, 0, 'zh', 'Asia/Shanghai', ARRAY['productivity','tools','digital'], now(), now()),

-- International Tech
('Hacker News', 'https://hnrss.org/frontpage', 'Technology', 'Hacker News Top Stories', true, 1800, 3, 60, 0, 0, 'en', 'UTC', ARRAY['tech','startup','programming'], now(), now()),
('TechCrunch', 'https://techcrunch.com/feed/', 'Technology', 'TechCrunch - Startup and Technology News', true, 3600, 3, 60, 0, 0, 'en', 'UTC', ARRAY['tech','startup','venture'], now(), now()),
('The Verge', 'https://www.theverge.com/rss/index.xml', 'Technology', 'The Verge - Technology, Science, Art', true, 3600, 3, 60, 0, 0, 'en', 'UTC', ARRAY['tech','gadgets','science'], now(), now()),
('Ars Technica', 'https://feeds.arstechnica.com/arstechnica/index', 'Technology', 'Ars Technica - Technology News and Analysis', true, 3600, 3, 60, 0, 0, 'en', 'UTC', ARRAY['tech','science','review'], now(), now()),

-- AI / ML
('MIT Tech Review', 'https://www.technologyreview.com/feed/', 'AI', 'MIT Technology Review', true, 7200, 3, 60, 0, 0, 'en', 'UTC', ARRAY['AI','ML','research'], now(), now()),
('OpenAI Blog', 'https://openai.com/blog/rss.xml', 'AI', 'OpenAI Official Blog', true, 7200, 3, 60, 0, 0, 'en', 'UTC', ARRAY['AI','LLM','research'], now(), now()),
('Google AI Blog', 'https://blog.google/technology/ai/rss/', 'AI', 'Google AI Blog', true, 7200, 3, 60, 0, 0, 'en', 'UTC', ARRAY['AI','ML','google'], now(), now()),

-- Programming
('Dev.to', 'https://dev.to/feed', 'Programming', 'Dev.to Developer Community', true, 3600, 3, 60, 0, 0, 'en', 'UTC', ARRAY['programming','web','tutorial'], now(), now()),
('Baeldung', 'https://feeds.feedburner.com/Baeldung', 'Programming', 'Baeldung - Java, Spring and Web Development', true, 7200, 3, 60, 0, 0, 'en', 'UTC', ARRAY['java','spring','backend'], now(), now()),
('Martin Fowler', 'https://martinfowler.com/feed.atom', 'Programming', 'Martin Fowler - Software Architecture', true, 86400, 3, 60, 0, 0, 'en', 'UTC', ARRAY['architecture','design','patterns'], now(), now()),

-- Business
('BBC Business', 'https://feeds.bbci.co.uk/news/business/rss.xml', 'Business', 'BBC Business News', true, 3600, 3, 60, 0, 0, 'en', 'UTC', ARRAY['business','finance','economy'], now(), now()),
('CNBC Tech', 'https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=10001147', 'Business', 'CNBC Technology News', true, 3600, 3, 60, 0, 0, 'en', 'UTC', ARRAY['business','tech','market'], now(), now());
