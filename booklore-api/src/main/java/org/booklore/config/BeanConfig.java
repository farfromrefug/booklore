package org.booklore.config;

import jakarta.annotation.PostConstruct;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class BeanConfig {

    @Autowired
    private WebSocketMessageBrokerStats webSocketMessageBrokerStats;

    @Bean
    public RestTemplate restTemplate(HttpClient httpClient) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(15));
        return new RestTemplate(factory);
    }

    @PostConstruct
    public void init() {
        webSocketMessageBrokerStats.setLoggingPeriod(30L * 24 * 60 * 60 * 1000); // 30 days
    }


}
