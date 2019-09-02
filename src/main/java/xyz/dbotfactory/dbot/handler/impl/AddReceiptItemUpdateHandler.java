package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.DBotUserException;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.ReceiptItem;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.ArrayList;

@Component
@Log
public class AddReceiptItemUpdateHandler implements UpdateHandler, CommonConsts {

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

    private String[] parseItem(String item) {
        String[] split = item.split(" ");
        if (split.length < 3)
            throw new DBotUserException("incorrect receipt item format");
        String amount = split[0];
        String priceForOne = split[1];
        String name = item.substring(amount.length() + priceForOne.length() + 2);

        return new String[]{amount, priceForOne, name};
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        String text = update.getMessage().getText();
        String[] itemInfo = parseItem(text);

        String amount = itemInfo[0];
        String priceForOne = itemInfo[1];
        String name = itemInfo[2];

        Receipt receipt = chatService.getActiveReceipt(chat);

        int quantity = Integer.parseInt(amount);
        for (int i = 0; i < quantity; i++) {
            ReceiptItem receiptItem = ReceiptItem.builder()
                    .price(Double.parseDouble(priceForOne))
                    .name(name)
                    .telegramUsers(new ArrayList<>())
                    .build();
            receipt.getItems().add(receiptItem);
        }
        chatService.save(chat);

        SendMessage message = new SendMessage()
                .setChatId(chat.getTelegramChatId())
                .setText(DONE_EMOJI + "⠀");
        bot.execute(message);

        log.info("item(s) added to receipt" + receipt.getId() + " . Current items:  " + receipt.getItems());
    }
}
