package xyz.dbotfactory.dbot.proxy_settings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("proxy")
@Data
public class Proxy {
    private boolean enabled;
    private String user;
    private String pass;
    private String host;
    private Integer port;
}
