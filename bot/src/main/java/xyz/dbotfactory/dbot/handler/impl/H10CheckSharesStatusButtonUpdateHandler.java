package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.*;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.math.BigDecimal;

import static xyz.dbotfactory.dbot.BigDecimalUtils.create;
import static xyz.dbotfactory.dbot.BigDecimalUtils.toStr;

@Component
public class H10CheckSharesStatusButtonUpdateHandler implements UpdateHandler, CommonConsts {


    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final BotMessageHelper botMessageHelper;

    private final TelegramLongPollingBot bot;

    @Autowired
    public H10CheckSharesStatusButtonUpdateHandler(ChatService chatService, ReceiptService receiptService,
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
            if (data.startsWith(CHECK_STATUS_CALLBACK_DATA)) {
                String[] ids = data.substring(CHECK_STATUS_CALLBACK_DATA.length()).split(DELIMITER);
                int receiptId = Integer.parseInt(ids[1]);
                long tgGroupChatId = Long.parseLong(ids[0]);

                Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);

                return chatService.getActiveReceipt(groupChat).getId() == receiptId &&
                        groupChat.getChatState() == ChatState.COLLECTING_ITEMS;
            }
        }
        return false;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        String[] ids = update.getCallbackQuery()
                .getData().substring(CHECK_STATUS_CALLBACK_DATA.length()).split(DELIMITER);

        int receiptId = Integer.parseInt(ids[1]);
        long tgGroupChatId = Long.parseLong(ids[0]);
        Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);

        Receipt receipt = chatService.getActiveReceipt(groupChat);
        String response;
        if (receipt.getItems().size() != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("❗️ <i>These items are still not picked:</i> \n");
            sb.append("\n");
            sb.append("<pre>");
            for (ReceiptItem item : receipt.getItems()) {
                BigDecimal pickedShare = item.getShares()
                        .stream()
                        .map(Share::getShare)
                        .reduce(BigDecimal::add)
                        .orElse(create(0));
                if (item.getAmount().compareTo(pickedShare) != 0) {

                    BigDecimal unpickedShare = item.getAmount().subtract(pickedShare);
                    sb.append(item.getName()).append(" x ").append(toStr(unpickedShare))
                            .append("\n");
                }
            }

            sb.append("</pre>\n");
            response = sb.toString();
        } else {
            response = "<i>All items are picked!</i>";
        }

        SendMessage message = new SendMessage()
                .setChatId(chat.getTelegramChatId())
                .setText(response)
                .setParseMode(ParseMode.HTML);
        Message sentMessage = bot.execute(message);

//        addCleanupTasks(groupChat, sentMessage);

        Thread.sleep(2000);
        DeleteMessage deleteMessage = new DeleteMessage()
                .setChatId(sentMessage.getChatId())
                .setMessageId(sentMessage.getMessageId());
        bot.execute(deleteMessage);

        botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(),
                groupChat.getChatMetaInfo(), bot, update.getCallbackQuery().getFrom().getId());

        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery()
                .setCallbackQueryId(update.getCallbackQuery().getId());
        bot.execute(answerCallbackQuery);

        chatService.save(groupChat);
    }

    private void addCleanupTasks(Chat groupChat, Message sentMessage) {
        botMessageHelper.addNewTask(SHARES_DONE_TASK_NAME, groupChat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                groupChat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                groupChat.getChatMetaInfo(), sentMessage);
    }
}
