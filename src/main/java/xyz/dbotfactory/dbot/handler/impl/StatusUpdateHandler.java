package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.ReceiptItem;
import xyz.dbotfactory.dbot.service.ChatService;

@Component
public class StatusUpdateHandler implements UpdateHandler {

    private static final String COMMAND_NAME = "/status";

    private final ChatService chatService;
    private final TelegramLongPollingBot bot;

    @Autowired
    public StatusUpdateHandler(ChatService chatService, TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return update.getMessage().isCommand() &&
                update.getMessage().getText().equals(COMMAND_NAME) &&
                chat.getChatState() == ChatState.COLLECTING_ITEMS;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        Receipt activeReceipt = chatService.getActiveReceipt(chat);
        StringBuilder stringBuilder = new StringBuilder();

        for (ReceiptItem item : activeReceipt.getItems()) {
            stringBuilder.append(item.getName() + " : " + item.getPrice() + "\n");
        }
        String result = stringBuilder.toString();

        SendMessage message = new SendMessage()
                .setChatId(update.getMessage().getChatId())
                .setText(result);

        bot.execute(message);
    }
}