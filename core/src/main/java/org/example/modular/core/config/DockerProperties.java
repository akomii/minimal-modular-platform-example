package org.example.modular.core.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Docker daemon connection settings (host plus optional TLS) bound from {@code docker.*} properties, used to build the platform's {@code DockerClient}.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "docker")
@ConditionalOnProperty(name = "modules.runtime", havingValue = "docker", matchIfMissing = true)
public class DockerProperties {

  private String host;
  private boolean tlsVerify;
  private String certPath;
}
