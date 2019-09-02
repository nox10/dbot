package xyz.dbotfactory.dbot;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;

import javax.annotation.PostConstruct;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

@Configuration
@EnableConfigurationProperties
public class Config {

    @Bean
    public TelegramBotsApi telegramBotsApi() {
        return new TelegramBotsApi();
    }

    @Bean
    public DefaultBotOptions botOptions(Proxy proxy) {
        DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);

        if (proxy.isEnabled()) {
            botOptions.setProxyHost(proxy.getHost());
            botOptions.setProxyPort(proxy.getPort());
            botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
        }

        return botOptions;
    }


    @Bean
    public PasswordAuthentication passwordAuthentication(Proxy proxy) {
        if (proxy.isEnabled()) {
            return new PasswordAuthentication(proxy.getUser(), proxy.getPass().toCharArray());
        } else {
            return new PasswordAuthentication("no_proxy", "no_proxy".toCharArray());
        }
    }

    @Bean
    public Authenticator authenticator(PasswordAuthentication passwordAuthentication, Proxy proxy) {
        if (proxy.isEnabled()) {
            return new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return passwordAuthentication;
                }
            };
        } else {
            return new Authenticator() {
            };
        }
    }

    @PostConstruct
    public void init() {
        ApiContextInitializer.init();
    }
}
