package io.jclaw.docstore.search;

import io.jclaw.docstore.model.DocStoreEntry;

import java.util.List;

/**
 * SPI for searching the DocStore index. Implementations may use
 * full-text search, vector/semantic search, or a hybrid approach.
 */
public interface DocStoreSearchProvider {

    List<DocStoreSearchResult> search(String query, DocStoreSearchOptions options);

    void index(DocStoreEntry entry);

    void remove(String entryId);
}
