package org.eSante.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxDBConfig {
    @Value("${influxdb.url:http://influxdb:8086}")
    private String url;
    @Value("${influxdb.token:my-super-secret-auth-token}")
    private String token;
    @Value("${influxdb.org:eSanteIdb}")
    private String org;
    @Value("${influxdb.bucket:mesure_data}")
    private String bucket;

    @Bean
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
    }
}

