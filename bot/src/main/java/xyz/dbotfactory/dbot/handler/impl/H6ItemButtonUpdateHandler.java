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
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static xyz.dbotfactory.dbot.BigDecimalUtils.isGreater;
import static xyz.dbotfactory.dbot.BigDecimalUtils.shareToStr;

@Component
@Log
public class H6ItemButtonUpdateHandler implements UpdateHandler, CommonConsts {

    private static final String CUSTOM_SHARE_BUTTON_TEXT = "Custom 📊";
    private static final String SHARES_MESSAGE_TEXT = "Set your share 🍕";

    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final TelegramLongPollingBot bot;
    private final BotMessageHelper botMessageHelper;

    @Autowired
    public H6ItemButtonUpdateHandler(ChatService chatService, ReceiptService receiptService,
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
            if (data.startsWith(ITEM_BUTTON_CALLBACK_DATA_PREFIX)) {
                String[] ids = data.substring(ITEM_BUTTON_CALLBACK_DATA_PREFIX.length()).split(DELIMITER);
                int itemId = Integer.parseInt(ids[0]);
                int receiptId = Integer.parseInt(ids[1]);
                long tgGroupChatId = Long.parseLong(ids[2]);

                Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);

                return groupChat.getChatState() == ChatState.COLLECTING_ITEMS &&
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
        long tgGroupChatId = Long.parseLong(ids[2]);
        Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);
        long userId = update.getCallbackQuery().getFrom().getId();

        Receipt receipt = chatService.getActiveReceipt(groupChat);
        ReceiptItem item =
                receipt.getItems().stream().filter(anItem -> anItem.getId() == itemId).findFirst().get();

        List<List<InlineKeyboardButton>> shareButtons = new ArrayList<>();

        BigDecimal shareLeft = receiptService.shareLeft(item, userId);
        if (isGreater(shareLeft, 0)) {
            InlineKeyboardButton shareLeftButton = InlineKeyboardButton.builder()
                    .text(shareToStr(shareLeft))
                    .callbackData(SHARE_BUTTON_CALLBACK_DATA + SHARE_LEFT_BUTTON_CALLBACK_DATA +
                            DELIMITER + itemId + DELIMITER + receiptId + DELIMITER + tgGroupChatId)
                    .build();
            shareButtons.add(singletonList(shareLeftButton));
        }

        if (isGreater(shareLeft, 1)) {
            InlineKeyboardButton shareLeftButton = InlineKeyboardButton.builder()
                    .text("1")
                    .callbackData(SHARE_BUTTON_CALLBACK_DATA + "1" + DELIMITER +
                            itemId + DELIMITER + receiptId + DELIMITER + tgGroupChatId)
                    .build();
            shareButtons.add(singletonList(shareLeftButton));
        }

        if (isGreater(shareLeft, 0.5)) {
            InlineKeyboardButton shareLeftButton = InlineKeyboardButton.builder()
                    .text("0.5")
                    .callbackData(SHARE_BUTTON_CALLBACK_DATA + "0.5" + DELIMITER +
                            itemId + DELIMITER + receiptId + DELIMITER + tgGroupChatId)
                    .build();
            shareButtons.add(singletonList(shareLeftButton));
        }

        InlineKeyboardButton shareLeftButton = InlineKeyboardButton.builder()
                .text(CUSTOM_SHARE_BUTTON_TEXT)
                .callbackData(CUSTOM_SHARE_CALLBACK_DATA + itemId + DELIMITER +
                        receiptId + DELIMITER + tgGroupChatId)
                .build();
        shareButtons.add(singletonList(shareLeftButton));

        InlineKeyboardMarkup shareButtonsKeyboardMarkup = InlineKeyboardMarkup.builder()
                .keyboard(shareButtons)
                .build();

        Message message = update.getCallbackQuery().getMessage();
        EditMessageText editMessageText = EditMessageText.builder()
                .messageId(message.getMessageId())
                .chatId(Long.toString(userId))
                .replyMarkup(shareButtonsKeyboardMarkup)
                .text(SHARES_MESSAGE_TEXT)
                .parseMode(ParseMode.HTML)
                .build();

        bot.execute(editMessageText);

        if (chat.getChatState() == ChatState.SETTING_CUSTOM_SHARE) {
            chat.setChatState(ChatState.NO_ACTIVE_RECEIPT);
        }

        botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(), groupChat.getChatMetaInfo(), bot, userId);

        chatService.save(chat);
    }
}
