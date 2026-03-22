package io.jclaw.examples.telegramdocstore;

import io.jclaw.docstore.DocStoreService;
import io.jclaw.docstore.analysis.BasicDocStoreAnalyzer;
import io.jclaw.docstore.analysis.DocStoreAnalyzer;
import io.jclaw.docstore.repository.DocStoreRepository;
import io.jclaw.docstore.repository.JsonFileDocStoreRepository;
import io.jclaw.docstore.search.DocStoreSearchProvider;
import io.jclaw.docstore.search.FullTextDocStoreSearch;
import io.jclaw.docstore.telegram.TelegramDocStorePlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class DocStoreConfig {

    @Bean
    DocStoreRepository docStoreRepository() {
        return new JsonFileDocStoreRepository(
                Path.of(System.getProperty("user.home"), ".jclaw", "docstore"));
    }

    @Bean
    DocStoreSearchProvider docStoreSearchProvider() {
        return new FullTextDocStoreSearch();
    }

    @Bean
    DocStoreAnalyzer docStoreAnalyzer() {
        return new BasicDocStoreAnalyzer();
    }

    @Bean
    DocStoreService docStoreService(DocStoreRepository repository,
                                     DocStoreSearchProvider searchProvider,
                                     DocStoreAnalyzer analyzer) {
        return new DocStoreService(repository, searchProvider, analyzer);
    }

    @Bean
    TelegramDocStorePlugin telegramDocStorePlugin(DocStoreService docStoreService) {
        return new TelegramDocStorePlugin(docStoreService, true, false);
    }
}
