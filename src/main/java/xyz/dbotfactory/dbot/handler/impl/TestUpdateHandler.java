package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;

@Component
public class TestUpdateHandler implements UpdateHandler {

    private final TelegramLongPollingBot telegramBot;

    @Autowired
    public TestUpdateHandler(TelegramLongPollingBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return false;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            SendMessage message = new SendMessage()
                    .setChatId(update.getMessage().getChatId())
                    .setText(update.getMessage().getText());

            telegramBot.execute(message);
        }
    }
}
