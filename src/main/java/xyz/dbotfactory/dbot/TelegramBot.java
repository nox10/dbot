package xyz.dbotfactory.dbot;

import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.UpdateHandlerAggregator;
import xyz.dbotfactory.dbot.model.Chat;

@Component
@ConfigurationProperties("bot")
@Setter
public class TelegramBot extends TelegramLongPollingBot {
    private String token;
    private String username;

    @Autowired
    UpdateHandlerAggregator updateHandlerAggregator;

    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        Chat chat = new Chat();
        UpdateHandler commandHandler = updateHandlerAggregator.getCommandHandler(update,chat);
        commandHandler.handle(update, chat);
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }
}
