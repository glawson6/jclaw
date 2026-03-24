package io.jclaw.docstore.analysis;

import io.jclaw.docstore.model.AnalysisResult;

/**
 * SPI for analyzing document content. Implementations range from basic text extraction
 * to LLM-powered summarization and entity extraction.
 */
public interface DocStoreAnalyzer {

    AnalysisResult analyze(byte[] content, String mimeType, String filename);

    boolean supports(String mimeType);
}
