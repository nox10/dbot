package xyz.dbotfactory.dbot.handler.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.ButtonFactory;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.service.ChatService;

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
    private final ChatService chatService;

    @Autowired
    public NewReceiptDiscardExistingUpdateHandler(ButtonFactory buttonFactory, BotMessageHelper botMessageHelper,
                                                  TelegramLongPollingBot bot, ChatService chatService) {
        this.buttonFactory = buttonFactory;
        this.botMessageHelper = botMessageHelper;
        this.bot = bot;
        this.chatService = chatService;
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
        botMessageHelper.deleteMessage(bot, update.getMessage());

        String callbackData = DISCARD_ACTIVE_RECEIPT_CALLBACK_DATA + DELIMITER + chat.getTelegramChatId();
        InlineKeyboardMarkup singleButton = buttonFactory.getSingleButton(BUTTON_LABEL, callbackData);

        Message sentMessage = botMessageHelper.sendMessageWithSingleInlineMarkup(chat.getTelegramChatId(),
                singleButton, bot, MESSAGE);
        botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(),
                chat.getChatMetaInfo(), bot, update.getMessage().getFrom().getId());
        botMessageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);

        chatService.save(chat);
    }
}
