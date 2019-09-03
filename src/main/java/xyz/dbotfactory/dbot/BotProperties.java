package xyz.dbotfactory.dbot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("bot")
@Data
public class BotProperties {
    private String token;
    private String username;
}
