package com.converter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties("flvserver")
public class FlvServerConfig {
    private String desKey;
}
