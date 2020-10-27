package xyz.dbotfactory.dbot.handler.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;

import static java.util.Collections.singletonList;

@Component
public class H0StartCommandUpdateHandler implements UpdateHandler {

    private final BotMessageHelper messageHelper;
    private final TelegramLongPollingBot bot;

    @Autowired
    public H0StartCommandUpdateHandler(BotMessageHelper messageHelper, TelegramLongPollingBot bot) {
        this.messageHelper = messageHelper;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return update.hasMessage() && update.getMessage().hasText()
                && (update.getMessage().getText().equals("/start")
                || update.getMessage().getText().equals("/new_receipt")
                || update.getMessage().getText().equals("/discard")
        )
                && chat.getChatState() == ChatState.NO_ACTIVE_RECEIPT
                && update.getMessage().getChat().isUserChat();
    }

    @Override
    public void handle(Update update, Chat chat) {
        InlineKeyboardButton button = new InlineKeyboardButton()
                .setUrl("https://telegram.me/" + bot.getBotUsername() + "?startgroup=initial_receipt")
                .setText("🌀 Add to a group 🌀");

        messageHelper.sendMessageWithSingleInlineMarkup(update.getMessage().getChatId(),
                new InlineKeyboardMarkup().setKeyboard(singletonList(singletonList(button))), bot,
                "Hello!\n\nℹ️ This bot works perfectly in groups.");
    }
}
