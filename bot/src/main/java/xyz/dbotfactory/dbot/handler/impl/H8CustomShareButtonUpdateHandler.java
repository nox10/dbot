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
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.ReceiptItem;
import xyz.dbotfactory.dbot.model.meta.ChatMetaInfo;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import static java.util.Collections.singletonList;

@Component
@Log
public class H8CustomShareButtonUpdateHandler implements UpdateHandler, CommonConsts {

    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final TelegramLongPollingBot bot;
    private final BotMessageHelper botMessageHelper;

    @Autowired
    public H8CustomShareButtonUpdateHandler(ChatService chatService, ReceiptService receiptService,
                                            TelegramLongPollingBot bot, BotMessageHelper botMessageHelper) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.bot = bot;
        this.botMessageHelper = botMessageHelper;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            if (data.startsWith(CUSTOM_SHARE_CALLBACK_DATA)) {
                String[] ids = data.substring(CUSTOM_SHARE_CALLBACK_DATA.length()).split(DELIMITER);
                int itemId = Integer.parseInt(ids[0]);
                int receiptId = Integer.parseInt(ids[1]);
                long tgGroupChatId = Long.parseLong(ids[2]);

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
        long tgGroupChatId = Long.parseLong(ids[2]);
        Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);
        int userId = update.getCallbackQuery().getFrom().getId();

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
                .setText("Enter share amount between 0 and " +
                        receiptService.shareLeft(item, userId) + " or press cancel.\n\n" +
                        "ℹ️ You can also use fractions like <code>1/3</code>")
                .setReplyMarkup(cancelKeyboardMarkup)
                .setChatId((long) userId)
                .setMessageId(message.getMessageId())
                .setParseMode(ParseMode.HTML);
        chat.setChatState(ChatState.SETTING_CUSTOM_SHARE);
        chat.setChatMetaInfo(ChatMetaInfo.builder().metaData(SETING_CUSTOM_SHARE_METADATA + itemId + DELIMITER +
                receiptId + DELIMITER + tgGroupChatId + DELIMITER + message.getMessageId()).build());
        bot.execute(editMessageText);

        botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(),
                groupChat.getChatMetaInfo(), bot, userId);

        chatService.save(chat);
    }
}
