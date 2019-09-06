package xyz.dbotfactory.dbot.handler.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.impl.callback.CountMeInCallbackInfo;
import xyz.dbotfactory.dbot.handler.impl.callback.PayOffCallbackInfo;
import xyz.dbotfactory.dbot.handler.impl.callback.ShareEqualCallbackInfo;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.service.ChatService;

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
        if (ShareEqualCallbackInfo.canHandle(update)) {
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

        Message sentMessage = messageHelper.sendMessageWithSingleInlineMarkup(
                callbackInfo.getTelegramChatId(),
                markup,
                bot,
                "Click if you're in! 😎");

        messageHelper.executeExistingTasks(this.getClass().getSimpleName(),
                chat.getChatMetaInfo(), bot, update.getCallbackQuery().getFrom().getId());
        messageHelper.addNewTask(CountMeInCallbackInfo.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        messageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        messageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);

        messageHelper.notifyCallbackProcessed(update.getCallbackQuery().getId(), bot);

        chatService.save(chat);
    }
}
