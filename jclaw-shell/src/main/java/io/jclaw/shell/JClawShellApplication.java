package io.jclaw.shell;

import com.embabel.agent.autoconfigure.platform.AgentPlatformAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = AgentPlatformAutoConfiguration.class)
public class JClawShellApplication {

    public static void main(String[] args) {
        SpringApplication.run(JClawShellApplication.class, args);
    }
}
