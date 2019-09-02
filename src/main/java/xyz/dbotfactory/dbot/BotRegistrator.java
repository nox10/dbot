package xyz.dbotfactory.dbot;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.TelegramBotsApi;

import javax.annotation.PostConstruct;
import java.net.Authenticator;

@Service
public class BotRegistrator {

    @Autowired
    private TelegramBotsApi telegramBotsApi;

    @Autowired
    private Authenticator authenticator;

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private Proxy proxy;

    @PostConstruct
    @SneakyThrows
    private void registerBot() {
        if (proxy.isEnabled()) {
            Authenticator.setDefault(authenticator);
        }

        telegramBotsApi.registerBot(telegramBot);
    }
}
