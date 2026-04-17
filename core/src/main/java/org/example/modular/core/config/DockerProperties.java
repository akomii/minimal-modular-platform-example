package org.example.modular.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "docker")
public class DockerProperties {

  private String host;
  private boolean tlsVerify;
  private String certPath;
}
