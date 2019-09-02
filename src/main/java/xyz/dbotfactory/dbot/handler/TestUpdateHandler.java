package xyz.dbotfactory.dbot.handler;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.TelegramBot;

@Component
public class TestUpdateHandler implements UpdateHandler {

    private final TelegramBot telegramBot;

    @Autowired
    public TestUpdateHandler(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @Override
    public boolean canHandle(Update update) {
        return true;
    }

    @Override
    @SneakyThrows
    public void handle(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            SendMessage message = new SendMessage()
                    .setChatId(update.getMessage().getChatId())
                    .setText(update.getMessage().getText());

            telegramBot.execute(message);
        }
    }
}
