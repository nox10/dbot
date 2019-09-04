package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.ReceiptItem;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

@Component
@Log
public class ItemButtonIsClickedUpdateHandler implements UpdateHandler, CommonConsts {

    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final TelegramLongPollingBot bot;

    @Autowired
    public ItemButtonIsClickedUpdateHandler(ChatService chatService, ReceiptService receiptService,
                                            TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            if (data.startsWith(ITEM_BUTTON_CALLBACK_DATA_PREFIX)) {
                String[] ids = data.substring(ITEM_BUTTON_CALLBACK_DATA_PREFIX.length()).split(DELIMITER);
                int itemId = Integer.parseInt(ids[0]);
                int receiptId = Integer.parseInt(ids[1]);
                long tgGroupChatId = Integer.parseInt(ids[2]);

                Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);

                return groupChat.getChatState() == ChatState.DETECTING_OWNERS &&
                        chatService.getActiveReceipt(groupChat).getId() == receiptId;
            }
        }

        return false;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        String[] ids = update.getCallbackQuery().getData()
                .substring(ITEM_BUTTON_CALLBACK_DATA_PREFIX.length()).split(DELIMITER);
        int itemId = Integer.parseInt(ids[0]);
        int receiptId = Integer.parseInt(ids[1]);
        long tgGroupChatId = Integer.parseInt(ids[2]);
        Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);
        long userId = chat.getTelegramChatId();

        Receipt receipt = chatService.getActiveReceipt(groupChat);
        ReceiptItem item =
                receipt.getItems().stream().filter(anItem -> anItem.getId() == itemId).findFirst().get();

        List<List<InlineKeyboardButton>> shareButtons = new ArrayList<>();

        double shareLeft = receiptService.shareLeft(item, userId);
        if (shareLeft > 0) {
            InlineKeyboardButton shareLeftButton = new InlineKeyboardButton()
                    .setText(numberToProperString(shareLeft))
                    .setCallbackData(SHARE_BUTTON_CALLBACK_DATA + SHARE_LEFT_BUTTON_CALLBACK_DATA +
                            DELIMITER + itemId + DELIMITER + receiptId + DELIMITER + tgGroupChatId);
            shareButtons.add(singletonList(shareLeftButton));
        }

        if (shareLeft >= 1) {
            InlineKeyboardButton shareLeftButton = new InlineKeyboardButton().setText("1")
                    .setCallbackData(SHARE_BUTTON_CALLBACK_DATA + "1" + DELIMITER +
                            itemId + DELIMITER + receiptId + DELIMITER + tgGroupChatId);
            shareButtons.add(singletonList(shareLeftButton));
        }

        if (shareLeft >= 0.5) {
            InlineKeyboardButton shareLeftButton = new InlineKeyboardButton().setText("0.5")
                    .setCallbackData(SHARE_BUTTON_CALLBACK_DATA + "0.5" + DELIMITER +
                            itemId + DELIMITER + receiptId + DELIMITER + tgGroupChatId);
            shareButtons.add(singletonList(shareLeftButton));
        }

        InlineKeyboardButton shareLeftButton = new InlineKeyboardButton().setText("Not supported yet")
                .setCallbackData(CUSTOM_SHARE_CALLBACK_DATA + itemId + DELIMITER +
                        receiptId + DELIMITER + tgGroupChatId);
        shareButtons.add(singletonList(shareLeftButton));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup()
                .setKeyboard(shareButtons);

        Message message = update.getCallbackQuery().getMessage();
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup()
                .setMessageId(message.getMessageId())
                .setChatId(userId)
                .setReplyMarkup(inlineKeyboardMarkup);

        bot.execute(editMessageReplyMarkup);
    }

    private String numberToProperString(double number) {
        if (number - (int) number == 0) {
            return Integer.toString((int) number);
        } else {
            return Double.toString(number);
        }
    }
}
