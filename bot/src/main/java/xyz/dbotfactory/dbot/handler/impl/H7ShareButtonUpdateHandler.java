package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.handler.*;
import xyz.dbotfactory.dbot.model.*;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static xyz.dbotfactory.dbot.BigDecimalUtils.create;

@Component
@Log
public class H7ShareButtonUpdateHandler implements UpdateHandler, CommonConsts {

    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final TelegramLongPollingBot bot;
    private final SharePickerHelper sharePickerHelper;
    private final BotMessageHelper botMessageHelper;

    @Autowired
    public H7ShareButtonUpdateHandler(ChatService chatService, ReceiptService receiptService,
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
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            if (data.startsWith(SHARE_BUTTON_CALLBACK_DATA)) {
                ShareButtonCallbackInfo callbackInfo = ShareButtonCallbackInfo.parseCallbackString(data);

                Chat groupChat = chatService.findOrCreateChatByTelegramId(callbackInfo.getTgGroupChatId());

                return groupChat.getChatState() == ChatState.DETECTING_OWNERS &&
                        chatService.getActiveReceipt(groupChat).getId() == callbackInfo.getReceiptId();
            }
        }

        return false;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery()
                .setCallbackQueryId(update.getCallbackQuery().getId());
        String data = update.getCallbackQuery().getData();
        ShareButtonCallbackInfo callbackInfo = ShareButtonCallbackInfo.parseCallbackString(data);

        int userId = update.getCallbackQuery().getFrom().getId();

        Chat groupChat = chatService.findOrCreateChatByTelegramId(callbackInfo.getTgGroupChatId());
        Receipt receipt = chatService.getActiveReceipt(groupChat);
        List<ReceiptItem> items = receipt.getItems();

        ReceiptItem item = items.stream().filter(anItem -> anItem.getId() == callbackInfo.getItemId()).findFirst().get();

        BigDecimal shareAmount;
        if (callbackInfo.getShareAmount().equals(SHARE_LEFT_BUTTON_CALLBACK_DATA)) {
            shareAmount = receiptService.shareLeft(item, userId);
        } else {
            shareAmount = create(Double.parseDouble(callbackInfo.getShareAmount()));
        }

        Share share;
        if (item.getShares().stream().anyMatch(aShare -> aShare.getTelegramUserId() == userId)) {
            share = item.getShares().stream()
                    .filter(aShare -> aShare.getTelegramUserId() == userId)
                    .findFirst().get();
            share.setShare(shareAmount);
        } else {
            share = Share.builder().share(shareAmount)
                    .telegramUserId(userId).build();
            item.getShares().add(share);
        }

        List<List<InlineKeyboardButton>> itemButtons = receipt.getItems().stream()
                .map(anItem -> singletonList(new InlineKeyboardButton()
                        .setText(anItem.getName() + receiptService.getShareStringForButton(anItem, userId))
                        .setCallbackData(ITEM_BUTTON_CALLBACK_DATA_PREFIX + anItem.getId() + DELIMITER +
                                callbackInfo.getReceiptId() + DELIMITER + callbackInfo.getTgGroupChatId())
                )).collect(Collectors.toList());

        InlineKeyboardButton finishedButton = new InlineKeyboardButton()
                .setText(FINISHED_SETTING_SHARES_BUTTON_TEXT)
                .setCallbackData(FINISHED_SETTING_SHARES_CALLBACK_DATA + callbackInfo.getTgGroupChatId()
                        + DELIMITER + callbackInfo.getReceiptId());

        itemButtons.add(singletonList(finishedButton));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup().setKeyboard(itemButtons);

        Message message = update.getCallbackQuery().getMessage();
        EditMessageText editMessageReplyMarkup = new EditMessageText()
                .setMessageId(message.getMessageId())
                .setChatId((long) userId)
                .setReplyMarkup(markup)
                .setParseMode(ParseMode.HTML)
                .setText(ITEMS_MESSAGE_TEXT);

        bot.execute(editMessageReplyMarkup);
        bot.execute(answerCallbackQuery);

        if (receiptService.allSharesDone(receipt)) {
            botMessageHelper.executeExistingTasks(SHARES_DONE_TASK_NAME, groupChat.getChatMetaInfo(), bot, userId);
            groupChat.setChatState(ChatState.COLLECTING_PAYMENTS_INFO);
            log.info("Chat " + groupChat.getId() + " is now in " + groupChat.getChatState() + " state");
            SendMessage sendMessage = new SendMessage()
                    .setChatId(callbackInfo.getTgGroupChatId())
                    .setParseMode(ParseMode.HTML)
                    .setText(DONE_MESSAGE_TEXT + receiptService.getTotalReceiptPrice(receipt));
            Message sentMessage = bot.execute(sendMessage);
            addCleanupTasks(groupChat, sentMessage);

            List<Message> sentMessages = sharePickerHelper.sendTotalPriceForEachUser(groupChat, receipt, bot);
            for (Message messagee : sentMessages) {
                botMessageHelper.addNewTask(H5RedirectToPmButtonUpdateHandler.class.getSimpleName(),
                        groupChat.getChatMetaInfo(), messagee);
                botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                        groupChat.getChatMetaInfo(), messagee);
                botMessageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                        groupChat.getChatMetaInfo(), messagee);
            }
        }

        botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(), groupChat.getChatMetaInfo(), bot, userId);

        chatService.save(groupChat);
    }

    private void addCleanupTasks(Chat groupChat, Message sentMessage) {
        botMessageHelper.addNewTask(RECEIPT_BALANCES_BUILT, groupChat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                groupChat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                groupChat.getChatMetaInfo(), sentMessage);
    }
}
