package io.jclaw.invoice;

import io.jclaw.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that registers invoice and timesheet tools
 * into the JClaw tool registry when present on the classpath.
 */
@AutoConfiguration
@AutoConfigureAfter(name = "io.jclaw.autoconfigure.JClawAutoConfiguration")
@ConditionalOnBean(ToolRegistry.class)
public class InvoiceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(InvoiceAutoConfiguration.class);

    @Bean
    public InvoiceToolsRegistrar invoiceToolsRegistrar(
            ToolRegistry toolRegistry,
            @Value("${jclaw.invoice.data-dir:./invoice-data}") String dataDir,
            @Value("${jclaw.invoice.output-dir:./invoices}") String outputDir,
            @Value("${jclaw.invoice.default-company:TapTech Holdings, Inc}") String defaultCompany,
            @Value("${jclaw.invoice.default-rate:100.00}") double defaultRate) {

        InvoiceConfig config = new InvoiceConfig(dataDir, outputDir, defaultCompany, defaultRate);
        log.info("Registering invoice tools (data-dir={}, output-dir={})", dataDir, outputDir);
        InvoiceTools.registerAll(toolRegistry, config);
        return new InvoiceToolsRegistrar();
    }

    /**
     * Marker bean to indicate invoice tools have been registered.
     */
    public static class InvoiceToolsRegistrar {}
}
