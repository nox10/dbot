package xyz.dbotfactory.dbot.handler.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.impl.callback.PayOffChatCallbackInfo;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.ArrayList;

@Component
public class PayOffChatUpdateHandler implements UpdateHandler {

    private final ChatService chatService;

    private final TelegramLongPollingBot bot;

    @Autowired
    BotMessageHelper messageHelper;

    @Autowired
    public PayOffChatUpdateHandler(ChatService chatService, TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            if (PayOffChatCallbackInfo.canHandle(data)) {
                PayOffChatCallbackInfo callbackInfo = PayOffChatCallbackInfo.fromCallbackData(data);
                Chat groupChat = chatService.findOrCreateChatByTelegramId(callbackInfo.getTelegramChatId());

                return groupChat.getChatState() == ChatState.NO_ACTIVE_RECEIPT;
            }
        }

        return false;
    }

    @Override
    public void handle(Update update, Chat chat) {
        String data = update.getCallbackQuery().getData();
        PayOffChatCallbackInfo callbackInfo = PayOffChatCallbackInfo.fromCallbackData(data);
        Chat groupChat = chatService.findOrCreateChatByTelegramId(callbackInfo.getTelegramChatId());

        groupChat.setReceipts(new ArrayList<>());

        chatService.save(groupChat);
        messageHelper.notifyCallbackProcessed(update.getCallbackQuery().getId(), bot);
    }
}
