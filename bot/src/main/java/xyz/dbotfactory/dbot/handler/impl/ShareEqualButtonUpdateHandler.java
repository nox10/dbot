package xyz.dbotfactory.dbot.handler.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.impl.callback.CountMeInCallbackInfo;
import xyz.dbotfactory.dbot.handler.impl.callback.PayOffCallbackInfo;
import xyz.dbotfactory.dbot.handler.impl.callback.ShareEqualCallbackInfo;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.Collections;

import static java.util.Collections.singletonList;
import static xyz.dbotfactory.dbot.model.ChatState.DETECTING_OWNERS;

@Component
public class ShareEqualButtonUpdateHandler implements UpdateHandler {

    private final ChatService chatService;

    private final TelegramLongPollingBot bot;

    private final BotMessageHelper messageHelper;

    @Autowired
    public ShareEqualButtonUpdateHandler(ChatService chatService, TelegramLongPollingBot bot, BotMessageHelper messageHelper) {
        this.chatService = chatService;
        this.bot = bot;
        this.messageHelper = messageHelper;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if(ShareEqualCallbackInfo.canHandle(update)){
            PayOffCallbackInfo callbackInfo = ShareEqualCallbackInfo.fromCallbackData(update.getCallbackQuery().getData());
            chat = chatService.findOrCreateChatByTelegramId(callbackInfo.getTelegramChatId());
            return chat.getChatState() == DETECTING_OWNERS;
        }
        return false;
    }

    @Override
    public void handle(Update update, Chat chat) {
        PayOffCallbackInfo callbackInfo = ShareEqualCallbackInfo.fromCallbackData(update.getCallbackQuery().getData());

        CountMeInCallbackInfo countMeInCallbackInfo = new CountMeInCallbackInfo(callbackInfo.getTelegramChatId());
        InlineKeyboardButton button = countMeInCallbackInfo.getButton();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup().setKeyboard(singletonList(singletonList(button)));

        messageHelper.sendMessageWithSingleInlineMarkup(
                callbackInfo.getTelegramChatId(),
                markup,
                bot,
                "Click if you're in!");

        messageHelper.notifyCallbackProcessed(update.getCallbackQuery().getId(), bot);
    }

}
