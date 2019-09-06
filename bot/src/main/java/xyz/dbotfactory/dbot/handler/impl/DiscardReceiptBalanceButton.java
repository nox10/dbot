package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.impl.callback.DiscardReceiptBalanceCallbackInfo;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.service.ChatService;

@Component
public class DiscardReceiptBalanceButton implements UpdateHandler {

    private final ChatService chatService;

    private final BotMessageHelper messageHelper;

    private final TelegramLongPollingBot bot;

    @Autowired
    public DiscardReceiptBalanceButton(ChatService chatService, BotMessageHelper messageHelper, TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.messageHelper = messageHelper;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (update.hasCallbackQuery() &&
                DiscardReceiptBalanceCallbackInfo.canHandle(update.getCallbackQuery().getData())) {

            DiscardReceiptBalanceCallbackInfo callbackInfo =
                    DiscardReceiptBalanceCallbackInfo.fromCallbackData(update.getCallbackQuery().getData());

            chat = chatService.findOrCreateChatByTelegramId(callbackInfo.getTelegramChatId());
            return chat.getChatState() == ChatState.NO_ACTIVE_RECEIPT;
        }
        return false;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        DiscardReceiptBalanceCallbackInfo callbackInfo =
                DiscardReceiptBalanceCallbackInfo.fromCallbackData(update.getCallbackQuery().getData());

        chat = chatService.findOrCreateChatByTelegramId(callbackInfo.getTelegramChatId());

        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery()
                .setCallbackQueryId(update.getCallbackQuery().getId())
                .setText("Current receipt was PERERASCHETED")
                .setShowAlert(true);
        bot.execute(answerCallbackQuery);

        chatService.removeReceipt(chat, callbackInfo.getReceiptId());
        chatService.save(chat);
        messageHelper.notifyCallbackProcessed(update.getCallbackQuery().getId(), bot);
    }
}
