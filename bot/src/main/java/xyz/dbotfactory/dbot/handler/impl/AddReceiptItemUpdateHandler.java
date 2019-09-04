package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.DBotUserException;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.ReceiptItem;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

@Component
@Log
public class AddReceiptItemUpdateHandler implements UpdateHandler, CommonConsts {

    private static final String DONE_EMOJI = "✔️";
    private static final String DONE_TEXT = DONE_EMOJI + " <i>Done, feel free to send more...</i>";
    private static final String STATUS_EMOJI = "ℹ️";
    private static final String COLLECTING_STATUS_BUTTON_TEXT = STATUS_EMOJI + " Status " + STATUS_EMOJI;

    private final ChatService chatService;
    private final TelegramLongPollingBot bot;

    @Autowired
    public AddReceiptItemUpdateHandler(ChatService chatService, TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return update.hasMessage() &&
                !update.getMessage().isCommand() &&
                chat.getChatState() == ChatState.COLLECTING_ITEMS;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        String text = update.getMessage().getText();
        String[] itemInfo = parseItem(text);

        String amountStr = itemInfo[0];
        String priceForUnit = itemInfo[1];
        String name = itemInfo[2];

        Receipt receipt = chatService.getActiveReceipt(chat);

        int amount = Integer.parseInt(amountStr);
        ReceiptItem receiptItem = ReceiptItem.builder()
                .price(Double.parseDouble(priceForUnit))
                .name(name)
                .shares(new ArrayList<>())
                .amount(amount)
                .build();

        List<ReceiptItem> items = receipt.getItems();

        int index = items.indexOf(receiptItem);
        if (index == -1) {
            items.add(receiptItem);
        } else {
            ReceiptItem existingItem = items.get(index);
            existingItem.setAmount(existingItem.getAmount() + receiptItem.getAmount());
        }

        chatService.save(chat);

        InlineKeyboardButton collectingStatusButton = new InlineKeyboardButton()
                .setText(COLLECTING_STATUS_BUTTON_TEXT)
                .setCallbackData(COLLECTING_STATUS_CALLBACK_DATA);
        InlineKeyboardMarkup collectingStatusMarkup = new InlineKeyboardMarkup()
                .setKeyboard(singletonList(singletonList(collectingStatusButton)));

        SendMessage message = new SendMessage()
                .setChatId(chat.getTelegramChatId())
                .setText(DONE_TEXT)
                .setReplyToMessageId(update.getMessage().getMessageId())
                .setParseMode(ParseMode.HTML)
                .setReplyMarkup(collectingStatusMarkup);
        bot.execute(message);

        log.info("item(s) added to receipt" + receipt.getId() + " . Current items:  " + receipt.getItems());
    }

    private String[] parseItem(String item) {
        String[] split = item.split(" ");
        if (split.length < 3)
            throw new DBotUserException("incorrect receipt item format");
        String amount = split[0];
        String priceForUnit = split[1];
        String name = item.substring(amount.length() + priceForUnit.length() + 2).toUpperCase();

        return new String[]{amount, priceForUnit, name};
    }
}
