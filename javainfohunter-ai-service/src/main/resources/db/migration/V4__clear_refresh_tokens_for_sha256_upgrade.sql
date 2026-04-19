-- Clear all refresh tokens after upgrading hash algorithm from 32-bit to SHA-256
-- Users will need to re-login to obtain new SHA-256 hashed tokens
DELETE FROM refresh_tokens;
