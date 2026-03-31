package io.jaiclaw.config;

import java.util.List;
import java.util.Map;

public record ModelsProperties(
        Map<String, ModelProviderConfig> providers
) {
    public static final ModelsProperties DEFAULT = new ModelsProperties(Map.of());

    public record ModelProviderConfig(
            String baseUrl,
            String apiKey,
            String api,
            List<ModelDef> models,
            List<String> wizardModels,
            String fallbackModel,
            String displayName
    ) {
        public ModelProviderConfig {
            if (wizardModels == null) wizardModels = List.of();
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String baseUrl;
            private String apiKey;
            private String api;
            private List<ModelDef> models;
            private List<String> wizardModels;
            private String fallbackModel;
            private String displayName;

            public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
            public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
            public Builder api(String api) { this.api = api; return this; }
            public Builder models(List<ModelDef> models) { this.models = models; return this; }
            public Builder wizardModels(List<String> wizardModels) { this.wizardModels = wizardModels; return this; }
            public Builder fallbackModel(String fallbackModel) { this.fallbackModel = fallbackModel; return this; }
            public Builder displayName(String displayName) { this.displayName = displayName; return this; }

            public ModelProviderConfig build() {
                return new ModelProviderConfig(baseUrl, apiKey, api, models, wizardModels, fallbackModel, displayName);
            }
        }
    }

    public record ModelDef(
            String id,
            String name,
            int contextWindow
    ) {
    }
}
