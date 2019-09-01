package xyz.dbotfactory.dbot;

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("proxy")
@Data
public class Proxy {
    private String user;
    private String pass;
    private String host;
    private Integer port;
}
