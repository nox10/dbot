package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.BigDecimalHelper;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.*;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Double.parseDouble;
import static java.util.Collections.singletonList;

@Component
@Log
public class H9CustomShareTextMessageUpdateHandler implements UpdateHandler, CommonConsts {

    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final TelegramLongPollingBot bot;

    @Autowired
    public H9CustomShareTextMessageUpdateHandler(ChatService chatService, ReceiptService receiptService,
                                                 TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (chat.getChatState() == ChatState.SETTING_CUSTOM_SHARE &&
                update.hasMessage() &&
                update.getMessage().hasText() &&
                update.getMessage().getChat().isUserChat()) {

            String[] ids = chat.getChatMetaInfo().getMetaData()
                    .substring(SETING_CUSTOM_SHARE_METADATA.length()).split(DELIMITER);
            int itemId = Integer.parseInt(ids[0]);
            int receiptId = Integer.parseInt(ids[1]);
            long tgGroupChatId = Integer.parseInt(ids[2]);

            Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);

            return chatService.getActiveReceipt(groupChat).getId() == receiptId;
        }

        return false;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        String[] ids = chat.getChatMetaInfo().getMetaData()
                .substring(SETING_CUSTOM_SHARE_METADATA.length()).split(DELIMITER);
        int itemId = Integer.parseInt(ids[0]);
        int receiptId = Integer.parseInt(ids[1]);
        long tgGroupChatId = Integer.parseInt(ids[2]);
        int editMessageId = Integer.parseInt(ids[3]);
        Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);
        long userId = chat.getTelegramChatId();

        String text = update.getMessage().getText();
        BigDecimal customShareAmount = getCustomShareFromString(text);

        Receipt receipt = chatService.getActiveReceipt(groupChat);
        ReceiptItem item =
                receipt.getItems().stream().filter(anItem -> anItem.getId() == itemId).findFirst().get();

        boolean shareIsValid =
                BigDecimalHelper.isSmallerOrEqual(customShareAmount, receiptService.shareLeft(item, userId))
                        && BigDecimalHelper.isGreaterOrEqual(customShareAmount, BigDecimal.ZERO);

        if (shareIsValid) {
            Share share;
            if (item.getShares().stream().anyMatch(aShare -> aShare.getTelegramUserId() == userId)) {
                share = item.getShares().stream()
                        .filter(aShare -> aShare.getTelegramUserId() == userId)
                        .findFirst().get();
                share.setShare(customShareAmount);
            } else {
                share = Share.builder().share(customShareAmount)
                        .telegramUserId(userId).build();
                item.getShares().add(share);
            }
        }

        List<List<InlineKeyboardButton>> itemButtons = receipt.getItems().stream()
                .map(anItem -> singletonList(new InlineKeyboardButton()
                        .setText(anItem.getName() + receiptService.getShareStringForButton(anItem, userId))
                        .setCallbackData(ITEM_BUTTON_CALLBACK_DATA_PREFIX + anItem.getId() + DELIMITER +
                                receiptId + DELIMITER + tgGroupChatId)
                )).collect(Collectors.toList());

        InlineKeyboardButton finishedButton = new InlineKeyboardButton()
                .setText(FINISHED_SETTING_SHARES_BUTTON_TEXT)
                .setCallbackData(FINISHED_SETTING_SHARES_CALLBACK_DATA + tgGroupChatId
                        + DELIMITER + receiptId);

        itemButtons.add(singletonList(finishedButton));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup().setKeyboard(itemButtons);

        EditMessageText editMessageText = new EditMessageText()
                .setMessageId(editMessageId)
                .setChatId(userId)
                .setReplyMarkup(markup)
                .setParseMode(ParseMode.HTML)
                .setText(ITEMS_MESSAGE_TEXT);

        DeleteMessage deleteMessage = new DeleteMessage()
                .setMessageId(update.getMessage().getMessageId())
                .setChatId(update.getMessage().getChatId());

        bot.execute(editMessageText);
        bot.execute(deleteMessage);

        if (receiptService.allSharesDone(receipt)) {
            groupChat.setChatState(ChatState.COLLECTING_PAYMENTS_INFO);
            log.info("Chat " + groupChat.getId() + " is now in " + groupChat.getChatState() + " state");
            SendMessage sendMessage = new SendMessage()
                    .setChatId(tgGroupChatId)
                    .setParseMode(ParseMode.HTML)
                    .setText(DONE_MESSAGE_TEXT + receiptService.getTotalReceiptPrice(receipt));
            bot.execute(sendMessage);

            String[] pmUserIds = groupChat.getChatMetaInfo().getPmUserIds().split(DELIMITER);
            for (String pmUserId : pmUserIds) {
                SendMessage sendMessage2 = new SendMessage()
                        .setChatId(pmUserId)
                        .setParseMode(ParseMode.HTML)
                        .setText(GO_TO_GROUP_TEXT);
                bot.execute(sendMessage2);
                groupChat.getChatMetaInfo().setPmUserIds("");
            }
        }

        chatService.save(groupChat);
        chat.setChatState(ChatState.DETECTING_OWNERS);
    }

    private BigDecimal getCustomShareFromString(String text) {
        if (text.contains("/")) {
            String[] fraction = text.split("/");
            if (isNotProperDecimal(fraction[0]) || isNotProperDecimal(fraction[1])) {
                return BigDecimalHelper.create(-1);
            } else {
                return BigDecimalHelper.create(parseDouble(fraction[0]) / parseDouble(fraction[1]));
            }
        } else {
            if (isNotProperDecimal(text)) {
                return BigDecimalHelper.create(-1);
            } else {
                return new BigDecimal(text);
            }
        }
    }

    private boolean isNotProperDecimal(String decimal) {
        try {
            Double.parseDouble(decimal);
        } catch (Throwable e) {
            return true;
        }

        return false;
    }
}
