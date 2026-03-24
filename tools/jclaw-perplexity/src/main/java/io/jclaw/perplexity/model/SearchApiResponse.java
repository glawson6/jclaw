package io.jclaw.perplexity.model;

import java.util.List;

public record SearchApiResponse(List<SearchResult> results, int totalResults) {}
