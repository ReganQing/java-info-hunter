package com.ron.javainfohunter.processor.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for token-aware content preprocessing.
 *
 * <p>Provides conservative token estimation, truncation, and chunking
 * for Chinese-heavy content where 1 CJK character ≈ 2 tokens and
 * 1 ASCII character ≈ 0.25 tokens under qwen-max tokenization.</p>
 *
 * <p><b>Token Estimation Strategy:</b></p>
 * <ul>
 *   <li>CJK characters (Chinese/Japanese/Korean): ~2 tokens each</li>
 *   <li>Other characters (English/punctuation/numbers): ~0.25 tokens each</li>
 * </ul>
 *
 * <p>This is a conservative over-estimation to ensure we stay within
 * DashScope qwen-max's 30,720 token input limit.</p>
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
public final class ContentPreprocessor {

    private ContentPreprocessor() {
        // Utility class
    }

    /**
     * Conservative token count estimation for mixed Chinese/English text.
     *
     * <p>CJK characters count as ~2 tokens each (conservative estimate for qwen-max).
     * All other characters count as ~0.25 tokens each (4 chars ≈ 1 token).</p>
     *
     * @param text the text to estimate tokens for
     * @return estimated token count (always >= 0)
     */
    public static int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        double tokens = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCJK(c)) {
                tokens += 2.0;
            } else {
                tokens += 0.25;
            }
        }
        return (int) Math.ceil(tokens);
    }

    /**
     * Truncate text to fit within a token budget, preserving sentence boundaries.
     *
     * <p>Tries to break at sentence-ending punctuation (. ! ? 。！？)
     * to avoid cutting mid-sentence. Falls back to hard truncation
     * if no sentence boundary is found near the limit.</p>
     *
     * @param text      the text to truncate
     * @param maxTokens the maximum allowed token count
     * @return truncated text, possibly with "..." appended
     */
    public static String truncateToTokenLimit(String text, int maxTokens) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        int estimatedTokens = estimateTokenCount(text);
        if (estimatedTokens <= maxTokens) {
            return text;
        }

        // Binary search for the character position that fits within maxTokens
        int left = 0;
        int right = text.length();
        int bestPos = text.length();

        while (left <= right) {
            int mid = (left + right) / 2;
            int tokensAtMid = estimateTokenCount(text.substring(0, mid));
            if (tokensAtMid <= maxTokens) {
                bestPos = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        // Try to find a sentence boundary near bestPos (look back up to 200 chars)
        int boundaryPos = findSentenceBoundary(text, bestPos);
        if (boundaryPos > 0) {
            bestPos = boundaryPos;
        }

        return text.substring(0, bestPos) + "...";
    }

    /**
     * Split content into chunks for map-reduce processing.
     *
     * <p>Each chunk will have at most {@code chunkTokenLimit} estimated tokens.
     * Chunks are split at sentence boundaries when possible.</p>
     *
     * @param text           the text to split
     * @param chunkTokenLimit maximum tokens per chunk
     * @return list of text chunks
     */
    public static List<String> splitIntoChunks(String text, int chunkTokenLimit) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        int totalTokens = estimateTokenCount(text);
        if (totalTokens <= chunkTokenLimit) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int length = text.length();

        while (start < length) {
            // Find end position for this chunk using binary search
            int left = start + 1;
            int right = Math.min(start + (length - start), length);
            int bestEnd = right;

            while (left <= right) {
                int mid = (left + right) / 2;
                int tokens = estimateTokenCount(text.substring(start, mid));
                if (tokens <= chunkTokenLimit) {
                    bestEnd = mid;
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }

            // Try to break at sentence boundary
            int boundaryPos = findSentenceBoundary(text.substring(start, Math.min(bestEnd, length)), bestEnd - start);
            if (boundaryPos > 0 && boundaryPos < bestEnd - start) {
                bestEnd = start + boundaryPos;
            }

            // Ensure we make progress
            if (bestEnd <= start) {
                bestEnd = Math.min(start + 100, length);
            }

            String chunk = text.substring(start, bestEnd).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start = bestEnd;
        }

        return chunks;
    }

    /**
     * Check if a character is a CJK (Chinese/Japanese/Korean) ideograph.
     */
    private static boolean isCJK(char c) {
        // CJK Unified Ideographs
        if (c >= '\u4E00' && c <= '\u9FFF') return true;
        // CJK Unified Ideographs Extension A
        if (c >= '\u3400' && c <= '\u4DBF') return true;
        // CJK Unified Ideographs Extension B (handled via surrogate pairs in String, not char)
        // Cannot represent in char - these are supplementary plane characters
        // CJK Compatibility Ideographs
        if (c >= '\uF900' && c <= '\uFAFF') return true;
        // Hiragana & Katakana (Japanese)
        if (c >= '\u3040' && c <= '\u309F') return true;
        if (c >= '\u30A0' && c <= '\u30FF') return true;
        // Hangul Syllables (Korean)
        if (c >= '\uAC00' && c <= '\uD7AF') return true;
        // Fullwidth forms
        if (c >= '\uFF01' && c <= '\uFF60') return true;
        // CJK punctuation
        if (c >= '\u3000' && c <= '\u303F') return true;
        return false;
    }

    /**
     * Find the last sentence boundary at or before the given position.
     *
     * <p>Looks for sentence-ending characters: . ! ? 。！？</p>
     *
     * @param text the full text
     * @param pos  the maximum position to consider
     * @return the position after the last sentence boundary, or 0 if none found
     */
    private static int findSentenceBoundary(String text, int pos) {
        int searchStart = Math.max(0, pos - 200);
        int lastBoundary = -1;

        for (int i = searchStart; i < pos; i++) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '。' || c == '！' || c == '？') {
                lastBoundary = i + 1;
            }
        }

        return lastBoundary > 0 ? lastBoundary : 0;
    }
}
