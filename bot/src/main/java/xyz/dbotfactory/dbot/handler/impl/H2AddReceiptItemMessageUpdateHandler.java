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
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static xyz.dbotfactory.dbot.BigDecimalUtils.create;

@Component
@Log
public class H2AddReceiptItemMessageUpdateHandler implements UpdateHandler, CommonConsts {


    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final TelegramLongPollingBot bot;

    @Autowired
    public H2AddReceiptItemMessageUpdateHandler(ChatService chatService, ReceiptService receiptService,
                                                TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return update.hasMessage() &&
                !update.getMessage().isCommand() && update.getMessage().hasText() &&
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
        BigDecimal price = create(Double.parseDouble(priceForUnit));

        BigDecimal amount = create(amountStr);

        if(amount.signum() <= 0 || price.signum() < 0)
            return;

        ReceiptItem receiptItem = ReceiptItem.builder()
                .price(price)
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
            existingItem.setAmount(existingItem.getAmount().add(receiptItem.getAmount()));
        }

        String formattedReceipt = receiptService.buildBeautifulReceiptString(receipt);

        InlineKeyboardButton collectingFinishedButton = new InlineKeyboardButton()
                .setText(COLLECTING_FINISHED_BUTTON_TEXT)
                .setCallbackData(COLLECTING_FINISHED_CALLBACK_DATA);
        InlineKeyboardMarkup collectingFinishedMarkup = new InlineKeyboardMarkup()
                .setKeyboard(singletonList(singletonList(collectingFinishedButton)));
        SendMessage message = new SendMessage()
                .setChatId(update.getMessage().getChatId())
                .setText(YOUR_RECEIPT_TEXT + formattedReceipt + "\n" + DONE_TEXT)
                .setReplyMarkup(collectingFinishedMarkup)
                .setParseMode(ParseMode.HTML);

        bot.execute(message);
        log.info("item(s) added to receipt" + receipt.getId() + " . Current items:  " + receipt.getItems());
        chatService.save(chat);
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
