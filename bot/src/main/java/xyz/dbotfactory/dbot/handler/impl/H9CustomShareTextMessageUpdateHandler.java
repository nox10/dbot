package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.BigDecimalUtils;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.SharePickerHelper;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.*;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Double.parseDouble;
import static java.util.Collections.singletonList;
import static xyz.dbotfactory.dbot.BigDecimalUtils.toStr;

@Component
@Log
public class H9CustomShareTextMessageUpdateHandler implements UpdateHandler, CommonConsts {

    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final TelegramLongPollingBot bot;
    private final BotMessageHelper botMessageHelper;

    private final SharePickerHelper sharePickerHelper;

    @Autowired
    public H9CustomShareTextMessageUpdateHandler(ChatService chatService, ReceiptService receiptService,
                                                 TelegramLongPollingBot bot, SharePickerHelper sharePickerHelper,
                                                 BotMessageHelper botMessageHelper) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.bot = bot;
        this.sharePickerHelper = sharePickerHelper;
        this.botMessageHelper = botMessageHelper;
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
            long tgGroupChatId = Long.parseLong(ids[2]);

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
        long tgGroupChatId = Long.parseLong(ids[2]);
        int editMessageId = Integer.parseInt(ids[3]);
        Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);
        int userId = update.getMessage().getFrom().getId();

        String text = update.getMessage().getText();
        BigDecimal customShareAmount = getCustomShareFromString(text);

        Receipt receipt = chatService.getActiveReceipt(groupChat);
        ReceiptItem item =
                receipt.getItems().stream().filter(anItem -> anItem.getId() == itemId).findFirst().get();

        boolean shareIsValid =
                BigDecimalUtils.isSmallerOrEqual(customShareAmount, receiptService.shareLeft(item, userId))
                        && BigDecimalUtils.isGreaterOrEqual(customShareAmount, BigDecimal.ZERO);

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
                        .setText(receiptService.getShareStringForButton(anItem, userId))
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
                .setChatId((long) userId)
                .setReplyMarkup(markup)
                .setParseMode(ParseMode.HTML)
                .setText(ITEMS_MESSAGE_TEXT);

        bot.execute(editMessageText);

        if (receiptService.allSharesDone(receipt)) {
            botMessageHelper.executeExistingTasks(SHARES_DONE_TASK_NAME, groupChat.getChatMetaInfo(), bot,
                    update.getMessage().getFrom().getId());
            groupChat.setChatState(ChatState.COLLECTING_PAYMENTS_INFO);
            log.info("Chat " + groupChat.getId() + " is now in " + groupChat.getChatState() + " state");
            SendMessage sendMessage = new SendMessage()
                    .setChatId(tgGroupChatId)
                    .setParseMode(ParseMode.HTML)
                    .setText(DONE_MESSAGE_TEXT + "<code>" + toStr(receiptService.getTotalReceiptPrice(receipt)) + "</code>");
            Message sentMessage = bot.execute(sendMessage);

            askCleanupTasks(groupChat, sentMessage);

            List<Message> sentMessages = sharePickerHelper.sendTotalPriceForEachUser(groupChat, receipt, bot);
            for (Message message : sentMessages) {
                botMessageHelper.addNewTask(H5RedirectToPmButtonUpdateHandler.class.getSimpleName(),
                        groupChat.getChatMetaInfo(), message);
                botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                        groupChat.getChatMetaInfo(), message);
                botMessageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                        groupChat.getChatMetaInfo(), message);
            }
        }

        chat.setChatState(ChatState.DETECTING_OWNERS);

        botMessageHelper.deleteMessage(bot, update.getMessage());
        botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(),
                groupChat.getChatMetaInfo(), bot, userId);

        chatService.save(groupChat);
        chatService.save(chat);
    }

    private void askCleanupTasks(Chat groupChat, Message sentMessage) {
        botMessageHelper.addNewTask(RECEIPT_BALANCES_BUILT, groupChat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                groupChat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                groupChat.getChatMetaInfo(), sentMessage);
    }

    private BigDecimal getCustomShareFromString(String text) {
        if (text.contains("/")) {
            String[] fraction = text.split("/");
            if (isNotProperDecimal(fraction[0]) || isNotProperDecimal(fraction[1])) {
                return BigDecimalUtils.create(-1);
            } else {
                return BigDecimalUtils.create(parseDouble(fraction[0]) / parseDouble(fraction[1]));
            }
        } else {
            if (isNotProperDecimal(text)) {
                return BigDecimalUtils.create(-1);
            } else {
                return BigDecimalUtils.create(text);
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
