package xyz.dbotfactory.dbot.handler.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.service.ChatService;

import static xyz.dbotfactory.dbot.handler.CommonConsts.DELIMITER;
import static xyz.dbotfactory.dbot.handler.CommonConsts.DISCARD_ACTIVE_RECEIPT_CALLBACK_DATA;
import static xyz.dbotfactory.dbot.model.ChatState.NO_ACTIVE_RECEIPT;

@Component
public class DiscardButtonUpdateHandler implements UpdateHandler {

    private final H1NewReceiptCommandUpdateHandler newReceiptCommandUpdateHandler;

    private final ChatService chatService;

    private final BotMessageHelper botMessageHelper;

    private final TelegramLongPollingBot bot;

    @Autowired
    public DiscardButtonUpdateHandler(ChatService chatService, H1NewReceiptCommandUpdateHandler newReceiptCommandUpdateHandler, TelegramLongPollingBot bot, BotMessageHelper botMessageHelper) {
        this.chatService = chatService;
        this.newReceiptCommandUpdateHandler = newReceiptCommandUpdateHandler;
        this.botMessageHelper = botMessageHelper;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return chat.getChatState() != NO_ACTIVE_RECEIPT &&
                update.hasCallbackQuery() &&
                update.getCallbackQuery().getData().startsWith(DISCARD_ACTIVE_RECEIPT_CALLBACK_DATA);
    }

    @Override
    public void handle(Update update, Chat chat) {

        CallbackQuery callbackQuery = update.getCallbackQuery();

        long id = Long.parseLong(callbackQuery.getData().split(DELIMITER)[1]);
        chat = chatService.findOrCreateChatByTelegramId(id);
        chatService.removeActiveReceipt(chat);
        chat.setChatState(NO_ACTIVE_RECEIPT);

        botMessageHelper.deleteMessage(bot, update.getCallbackQuery().getMessage());
        botMessageHelper.notifyCallbackProcessed(callbackQuery.getId(), bot);
        newReceiptCommandUpdateHandler.handle(update, chat);
    }
}
