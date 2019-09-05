package xyz.dbotfactory.dbot.handler.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.ButtonFactory;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;

import static xyz.dbotfactory.dbot.handler.CommonConsts.DELIMITER;
import static xyz.dbotfactory.dbot.handler.CommonConsts.DISCARD_ACTIVE_RECEIPT_CALLBACK_DATA;
import static xyz.dbotfactory.dbot.handler.impl.H1NewReceiptCommandUpdateHandler.COMMAND_NAME;
import static xyz.dbotfactory.dbot.model.ChatState.NO_ACTIVE_RECEIPT;

@Component
public class NewReceiptDiscardExistingUpdateHandler implements UpdateHandler {

    private static final String BUTTON_LABEL = "Discard";
    private static final String MESSAGE = "Active receipt found. Discard and create a new one?";

    private final ButtonFactory buttonFactory;

    private final BotMessageHelper botMessageHelper;

    private final TelegramLongPollingBot bot;

    @Autowired
    public NewReceiptDiscardExistingUpdateHandler(ButtonFactory buttonFactory, BotMessageHelper botMessageHelper, TelegramLongPollingBot bot) {
        this.buttonFactory = buttonFactory;
        this.botMessageHelper = botMessageHelper;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return update.hasMessage() &&
                update.getMessage().isCommand() &&
                update.getMessage().getText().contains(COMMAND_NAME)
                && chat.getChatState() != NO_ACTIVE_RECEIPT
                && !update.getMessage().getChat().isUserChat();
    }

    @Override
    public void handle(Update update, Chat chat) {
        String callbackData = DISCARD_ACTIVE_RECEIPT_CALLBACK_DATA + DELIMITER + chat.getTelegramChatId();
        InlineKeyboardMarkup singleButton = buttonFactory.getSingleButton(BUTTON_LABEL, callbackData);

        botMessageHelper.sendMessageWithSingleInlineMarkup(chat.getTelegramChatId(), singleButton, bot, MESSAGE );
    }
}
