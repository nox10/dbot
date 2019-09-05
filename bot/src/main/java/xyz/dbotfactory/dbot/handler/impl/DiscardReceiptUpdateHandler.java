package xyz.dbotfactory.dbot.handler.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.ButtonFactory;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.service.ChatService;

import static xyz.dbotfactory.dbot.model.ChatState.NO_ACTIVE_RECEIPT;


@Component
public class DiscardReceiptUpdateHandler implements UpdateHandler {

    private static String COMMAND_NAME = "/discard";
    private static String MESSAGE = "Chat was discarded";

    private final ButtonFactory buttonFactory;

    private final ChatService chatService;

    private final BotMessageHelper messageHelper;

    private final TelegramLongPollingBot bot;

    @Autowired
    public DiscardReceiptUpdateHandler(ButtonFactory buttonFactory, ChatService chatService, BotMessageHelper messageHelper, TelegramLongPollingBot bot) {
        this.buttonFactory = buttonFactory;
        this.chatService = chatService;
        this.messageHelper = messageHelper;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return  update.hasMessage() &&
                update.getMessage().isCommand() &&
                update.getMessage().getText().contains(COMMAND_NAME)
                && chat.getChatState() != NO_ACTIVE_RECEIPT
                && !update.getMessage().getChat().isUserChat();
    }

    @Override
    public void handle(Update update, Chat chat) {
        chatService.removeActiveReceipt(chat);
        chat.setChatState(NO_ACTIVE_RECEIPT);
        chatService.save(chat);
        messageHelper.sendSimpleMessageToChat(MESSAGE, chat.getTelegramChatId(), bot);
    }
}
