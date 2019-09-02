package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.service.ChatService;

@Component
@Log
public class CollectingFinishedUpdateHandler implements UpdateHandler, CommonConsts {

    private ChatService chatService;
    private TelegramLongPollingBot bot;

    public CollectingFinishedUpdateHandler(ChatService chatService, TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return update.hasCallbackQuery() &&
                update.getCallbackQuery().getData().equals(COLLECTING_FINISHED_CALLBACK_DATA);
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        // TODO
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery()
                .setCallbackQueryId(callbackQuery.getId());
        bot.execute(answerCallbackQuery);
    }
}
