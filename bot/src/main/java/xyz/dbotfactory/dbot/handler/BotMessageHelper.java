package xyz.dbotfactory.dbot.handler;

import lombok.SneakyThrows;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Component
public class BotMessageHelper {

    @SneakyThrows
    public void sendSimpleMessage(String message, Long chatId, TelegramLongPollingBot bot){
        SendMessage sendMessage = new SendMessage(chatId, message).setParseMode(ParseMode.HTML);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void sendMessageWithSingleInlineMarkup(Long chatId,
                                                  InlineKeyboardMarkup markup,
                                                  TelegramLongPollingBot bot,
                                                  @Nullable String message){

        SendMessage sendMessage = new SendMessage()
                .setChatId(chatId)
                .setText(message)
                .setReplyMarkup(markup)
                .setParseMode(ParseMode.HTML);

        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void notifyCallbackProcessed(String callbackId, TelegramLongPollingBot bot){
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery()
                .setCallbackQueryId(callbackId);
        bot.execute(answerCallbackQuery);
    }
}
