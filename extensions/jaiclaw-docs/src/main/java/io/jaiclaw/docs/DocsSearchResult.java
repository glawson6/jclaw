package io.jaiclaw.docs;

/**
 * A search result from the docs repository.
 *
 * @param uri     the document URI
 * @param name    human-readable name
 * @param snippet a text snippet showing the match context
 * @param score   relevance score (higher is better)
 */
public record DocsSearchResult(
        String uri,
        String name,
        String snippet,
        double score
) {}
