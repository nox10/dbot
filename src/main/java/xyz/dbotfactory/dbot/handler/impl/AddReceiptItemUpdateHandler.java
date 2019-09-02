package xyz.dbotfactory.dbot.handler.impl;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.DBotUserException;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.ReceiptItem;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.ArrayList;

@Component
@Log
public class AddReceiptItemUpdateHandler implements UpdateHandler {

    private final ChatService chatService;
    private final TelegramLongPollingBot bot;

    @Autowired
    public AddReceiptItemUpdateHandler(ChatService chatService, TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return !update.getMessage().isCommand() &&
                chat.getChatState() == ChatState.COLLECTING_ITEMS;
    }

    @Override
    public void handle(Update update, Chat chat) {
        String text = update.getMessage().getText();
        String[] split = text.split("-");
        if (split.length != 3)
            throw new DBotUserException("incorrect receipt item format");

        Receipt receipt = chatService.getActiveReceipt(chat);

        int quantity = Integer.parseInt(split[1].trim());
        for (int i = 0; i < quantity; i++) {
            ReceiptItem receiptItem = ReceiptItem.builder()
                    .price(Double.parseDouble(split[1].trim()))
                    .name(split[2].trim())
                    .telegramUsers(new ArrayList<>())
                    .build();
            receipt.getItems().add(receiptItem);
        }
        chatService.save(chat);
        log.info("item(s) added to receipt" + receipt.getId() + " . Current items:  " + receipt.getItems());
    }
}
