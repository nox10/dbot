package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.*;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;
import xyz.dbotfactory.dbot.service.TelegramUserService;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Component
@Log
public class ShareButtonUpdateHandler implements UpdateHandler, CommonConsts {

    private static final String DONE_MESSAGE_TEXT =
            "<i>Now each of you can send me how much you have already paid, right in this chat</i>";
    private static final String GO_TO_GROUP_TEXT =
            "<i>Go back to group chat</i>";

    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final TelegramLongPollingBot bot;
    private final TelegramUserService telegramUserService;

    @Autowired
    public ShareButtonUpdateHandler(ChatService chatService, ReceiptService receiptService,
                                    TelegramUserService telegramUserService,
                                    TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.telegramUserService = telegramUserService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            if (data.startsWith(SHARE_BUTTON_CALLBACK_DATA)) {
                String[] dataArray = data.substring(SHARE_BUTTON_CALLBACK_DATA.length()).split(DELIMITER);
                String shareAmount = dataArray[0];
                int itemId = Integer.parseInt(dataArray[1]);
                int receiptId = Integer.parseInt(dataArray[2]);
                long tgGroupChatId = Integer.parseInt(dataArray[3]);

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
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery()
                .setCallbackQueryId(update.getCallbackQuery().getId());
        String[] dataArray = update.getCallbackQuery()
                .getData().substring(SHARE_BUTTON_CALLBACK_DATA.length()).split(DELIMITER);
        String shareAmount = dataArray[0];
        int itemId = Integer.parseInt(dataArray[1]);
        int receiptId = Integer.parseInt(dataArray[2]);
        long tgGroupChatId = Integer.parseInt(dataArray[3]);
        long userId = update.getCallbackQuery().getMessage().getChatId();

        Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);
        Receipt receipt = chatService.getActiveReceipt(groupChat);
        List<ReceiptItem> items = receipt.getItems();

        ReceiptItem item = items.stream().filter(anItem -> anItem.getId() == itemId).findFirst().get();

        double shareAmountDouble;
        if (shareAmount.equals(SHARE_LEFT_BUTTON_CALLBACK_DATA)) {
            shareAmountDouble = receiptService.shareLeft(item, userId);
        } else {
            shareAmountDouble = Double.parseDouble(shareAmount);
        }

        Share share;
        if (item.getShares().stream().anyMatch(aShare -> aShare.getTelegramUser().getTelegramId() == userId)) {
            share = item.getShares().stream()
                    .filter(aShare -> aShare.getTelegramUser().getTelegramId() == userId)
                    .findFirst().get();
            share.setShare(shareAmountDouble);
        } else {
            share = Share.builder().share(shareAmountDouble)
                    .telegramUser(telegramUserService.getTelegramUserByTgId(userId)).build();
            item.getShares().add(share);
        }

        List<List<InlineKeyboardButton>> itemButtons = receipt.getItems().stream()
                .map(anItem -> singletonList(new InlineKeyboardButton()
                        .setText(anItem.getName() + receiptService.getShareStringForButton(anItem, userId))
                        .setCallbackData(ITEM_BUTTON_CALLBACK_DATA_PREFIX + anItem.getId() + DELIMITER +
                                receiptId + DELIMITER + tgGroupChatId)
                )).collect(Collectors.toList());

        InlineKeyboardButton finishedButton = new InlineKeyboardButton()
                .setText(FINISHED_SETTING_SHARES_BUTTON_TEXT)
                .setCallbackData(FINISHED_SETTING_SHARES_CALLBACK_DATA + tgGroupChatId + DELIMITER + receiptId);

        itemButtons.add(singletonList(finishedButton));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup()
                .setKeyboard(itemButtons);

        Message message = update.getCallbackQuery().getMessage();
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup()
                .setMessageId(message.getMessageId())
                .setChatId(userId)
                .setReplyMarkup(markup);

        bot.execute(editMessageReplyMarkup);
        bot.execute(answerCallbackQuery);

        if (allSharesDone(receipt)) {
            groupChat.setChatState(ChatState.COLLECTING_PAYMENTS_INFO);
            log.info("Chat " + groupChat.getId() + " is now in " + groupChat.getChatState() + " state");
            SendMessage sendMessage = new SendMessage()
                    .setChatId(tgGroupChatId)
                    .setParseMode(ParseMode.HTML)
                    .setText(DONE_MESSAGE_TEXT);
            SendMessage sendMessage2 = new SendMessage()
                    .setChatId(userId)
                    .setParseMode(ParseMode.HTML)
                    .setText(GO_TO_GROUP_TEXT);
            bot.execute(sendMessage);
            bot.execute(sendMessage2);
        }

        chatService.save(groupChat);
    }

    private boolean allSharesDone(Receipt receipt) {
        return receipt.getItems().stream()
                .map(item -> item.getShares().stream()
                        .map(Share::getShare)
                        .reduce(Double::sum).get() == item.getAmount())
                .reduce((expr1, expr2) -> expr1 & expr2).get();
    }
}
