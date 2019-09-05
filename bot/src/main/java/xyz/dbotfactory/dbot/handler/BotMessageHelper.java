package xyz.dbotfactory.dbot.handler;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class BotMessageHelper {

    @SneakyThrows
    public void sendSimpleMessageToChat(String message, Long chatId, TelegramLongPollingBot bot){
        SendMessage sendMessage = new SendMessage(chatId, message);
        bot.execute(sendMessage);
    }
}
