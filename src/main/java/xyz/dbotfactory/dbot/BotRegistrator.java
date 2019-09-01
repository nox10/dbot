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

    @PostConstruct
    @SneakyThrows
    private void registerBot() {
        // Create the Authenticator that will return auth's parameters for proxy authentication
        Authenticator.setDefault(authenticator);

        // Create the TelegramBotsApi object to register your bots
        telegramBotsApi.registerBot(telegramBot);
    }
}
