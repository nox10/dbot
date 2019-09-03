package xyz.dbotfactory.dbot.handler.impl;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.DBotUserException;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.service.ChatService;

@Component
@Log
public class ItemButtonIsClickedUpdateHandler implements UpdateHandler, CommonConsts {

    private final static String MESSAGE_TEXT = "<i>Tap to items which are yours</i>";

    private final ChatService chatService;
    private final TelegramLongPollingBot bot;

    @Autowired
    public ItemButtonIsClickedUpdateHandler(ChatService chatService, TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            if (data.startsWith(ITEM_BUTTON_CALLBACK_DATA_PREFIX)) {
                String[] ids = data.substring(ITEM_BUTTON_CALLBACK_DATA_PREFIX.length()).split(DELIMITER);
                int itemId = Integer.parseInt(ids[0]);
                int receiptId = Integer.parseInt(ids[1]);
                long tgGroupChatId = Integer.parseInt(ids[2]);

                Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);

                return chatService.getActiveReceipt(groupChat).getId() == receiptId &&
                        groupChat.getChatState() == ChatState.DETECTING_OWNERS;
            }
        }

        return false;
    }

    @Override
    public void handle(Update update, Chat chat) {

    }
}
