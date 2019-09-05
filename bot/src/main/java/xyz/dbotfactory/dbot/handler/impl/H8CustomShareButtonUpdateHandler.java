package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.*;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import static java.util.Collections.singletonList;

@Component
@Log
public class H8CustomShareButtonUpdateHandler implements UpdateHandler, CommonConsts {

    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final TelegramLongPollingBot bot;

    @Autowired
    public H8CustomShareButtonUpdateHandler(ChatService chatService, ReceiptService receiptService,
                                            TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            if (data.startsWith(CUSTOM_SHARE_CALLBACK_DATA)) {
                String[] ids = data.substring(CUSTOM_SHARE_CALLBACK_DATA.length()).split(DELIMITER);
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
                .substring(CUSTOM_SHARE_CALLBACK_DATA.length()).split(DELIMITER);
        int itemId = Integer.parseInt(ids[0]);
        int receiptId = Integer.parseInt(ids[1]);
        long tgGroupChatId = Integer.parseInt(ids[2]);
        Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);
        long userId = chat.getTelegramChatId();

        Receipt receipt = chatService.getActiveReceipt(groupChat);
        ReceiptItem item =
                receipt.getItems().stream().filter(anItem -> anItem.getId() == itemId).findFirst().get();

        InlineKeyboardButton cancelButton = new InlineKeyboardButton().setText("Cancel")
                .setCallbackData(ITEM_BUTTON_CALLBACK_DATA_PREFIX + itemId + DELIMITER +
                        receiptId + DELIMITER + tgGroupChatId);

        InlineKeyboardMarkup cancelKeyboardMarkup = new InlineKeyboardMarkup()
                .setKeyboard(singletonList(singletonList(cancelButton)));

        Message message = update.getCallbackQuery().getMessage();
        EditMessageText editMessageText = new EditMessageText()
                .setText("<i>Enter share amount between 0 and " +
                        receiptService.shareLeft(item, userId) + " or press cancel.\n\n" +
                        "You can also use fractions like </i><code>1/3</code>")
                .setReplyMarkup(cancelKeyboardMarkup)
                .setChatId(userId)
                .setMessageId(message.getMessageId())
                .setParseMode(ParseMode.HTML);
        chat.setChatState(ChatState.SETTING_CUSTOM_SHARE);
        chat.setChatMetaInfo(ChatMetaInfo.builder().metaData(SETING_CUSTOM_SHARE_METADATA + itemId + DELIMITER +
                receiptId + DELIMITER + tgGroupChatId + DELIMITER + message.getMessageId()).build());
        bot.execute(editMessageText);
        chatService.save(chat);
    }
}
