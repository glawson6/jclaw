package io.jaiclaw.docs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository of documentation loaded from {@code classpath:docs/**&#47;*.md}.
 * Provides keyword-based full-text search and URI-based lookup.
 *
 * <p>URI scheme: {@code docs://{relative-path-without-extension}}
 * (e.g. {@code docs://architecture}, {@code docs://features/browser}).
 */
public class DocsRepository {

    private static final Logger log = LoggerFactory.getLogger(DocsRepository.class);
    private static final String CLASSPATH_PATTERN = "classpath:docs/**/*.md";
    private static final String URI_PREFIX = "docs://";

    private final Map<String, DocsEntry> entries = new ConcurrentHashMap<>();

    public DocsRepository() {
        loadFromClasspath();
    }

    /** For testing — construct with explicit entries. */
    public DocsRepository(List<DocsEntry> entries) {
        entries.forEach(e -> this.entries.put(e.uri(), e));
    }

    private void loadFromClasspath() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(CLASSPATH_PATTERN);

            for (Resource resource : resources) {
                try {
                    String path = extractRelativePath(resource);
                    if (path == null) continue;

                    String content;
                    try (InputStream is = resource.getInputStream()) {
                        content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }

                    String uri = URI_PREFIX + path;
                    String name = humanReadableName(path);
                    List<String> tags = extractTags(path);

                    DocsEntry entry = new DocsEntry(uri, name, "text/markdown", content, tags);
                    entries.put(uri, entry);
                    log.debug("Loaded doc: {} ({} chars)", uri, content.length());
                } catch (IOException e) {
                    log.warn("Failed to load doc resource: {}", resource.getFilename(), e);
                }
            }

            log.info("Loaded {} documentation entries from classpath", entries.size());
        } catch (IOException e) {
            log.error("Failed to scan classpath for docs", e);
        }
    }

    private String extractRelativePath(Resource resource) throws IOException {
        String url = resource.getURL().toString();
        int docsIdx = url.indexOf("/docs/");
        if (docsIdx < 0) return null;
        String relative = url.substring(docsIdx + "/docs/".length());
        // Strip .md extension
        if (relative.endsWith(".md")) {
            relative = relative.substring(0, relative.length() - 3);
        }
        return relative;
    }

    private String humanReadableName(String path) {
        String name = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        return name.replace('-', ' ').replace('_', ' ');
    }

    private List<String> extractTags(String path) {
        return Arrays.stream(path.split("[/\\-_]"))
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .toList();
    }

    /** Get all entries. */
    public List<DocsEntry> findAll() {
        return List.copyOf(entries.values());
    }

    /** Find a single entry by URI. */
    public Optional<DocsEntry> findByUri(String uri) {
        return Optional.ofNullable(entries.get(uri));
    }

    /**
     * Keyword search across document content, names, and tags.
     * Returns results sorted by relevance score (descending).
     */
    public List<DocsSearchResult> search(String query, int maxResults) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String[] terms = query.toLowerCase().split("\\s+");

        return entries.values().stream()
                .map(entry -> scoreEntry(entry, terms))
                .filter(r -> r.score() > 0)
                .sorted(Comparator.comparingDouble(DocsSearchResult::score).reversed())
                .limit(maxResults)
                .toList();
    }

    private DocsSearchResult scoreEntry(DocsEntry entry, String[] terms) {
        String contentLower = entry.content().toLowerCase();
        String nameLower = entry.name().toLowerCase();
        double score = 0;
        String bestSnippet = "";

        for (String term : terms) {
            // Name matches are weighted highest
            if (nameLower.contains(term)) {
                score += 10.0;
            }

            // Tag matches
            if (entry.tags().stream().anyMatch(t -> t.contains(term))) {
                score += 5.0;
            }

            // Content matches — count occurrences
            int idx = 0;
            int count = 0;
            while ((idx = contentLower.indexOf(term, idx)) >= 0) {
                count++;
                if (bestSnippet.isEmpty()) {
                    bestSnippet = extractSnippet(entry.content(), idx, term.length());
                }
                idx += term.length();
            }
            score += count;
        }

        if (bestSnippet.isEmpty() && !entry.content().isEmpty()) {
            bestSnippet = entry.content().substring(0, Math.min(150, entry.content().length())) + "...";
        }

        return new DocsSearchResult(entry.uri(), entry.name(), bestSnippet, score);
    }

    private String extractSnippet(String content, int matchIdx, int termLength) {
        int start = Math.max(0, matchIdx - 60);
        int end = Math.min(content.length(), matchIdx + termLength + 90);
        String snippet = content.substring(start, end).replaceAll("\\s+", " ").trim();
        if (start > 0) snippet = "..." + snippet;
        if (end < content.length()) snippet = snippet + "...";
        return snippet;
    }

    public int size() {
        return entries.size();
    }
}
