package org.example.modular.core.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerClientConfiguration {

  @Bean
  public DockerClient dockerClient(DockerProperties properties) {
    if (properties.getHost() == null || properties.getHost().isBlank()) {
      throw new IllegalStateException("Docker host is not configured");
    }

    DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerHost(properties.getHost())
        .withDockerTlsVerify(properties.isTlsVerify())
        .withDockerCertPath(properties.getCertPath())
        .build();

    DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
        .dockerHost(config.getDockerHost())
        .sslConfig(config.getSSLConfig())
        .build();

    return DockerClientImpl.getInstance(config, httpClient);
  }
}
