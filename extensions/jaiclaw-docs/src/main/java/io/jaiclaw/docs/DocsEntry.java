package io.jaiclaw.docs;

import java.util.List;

/**
 * A single documentation entry loaded from the classpath.
 *
 * @param uri      stable URI (e.g. {@code docs://architecture})
 * @param name     human-readable name
 * @param mimeType MIME type
 * @param content  full text content
 * @param tags     searchable tags derived from the path segments
 */
public record DocsEntry(
        String uri,
        String name,
        String mimeType,
        String content,
        List<String> tags
) {}
