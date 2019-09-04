package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.*;
import xyz.dbotfactory.dbot.service.ChatService;

import java.text.DecimalFormat;

import static xyz.dbotfactory.dbot.handler.CommonConsts.DELIMITER;
import static xyz.dbotfactory.dbot.handler.CommonConsts.FINISHED_SETTING_SHARES_CALLBACK_DATA;

@Component
public class H8SharesStatusButtonUpdateHandler implements UpdateHandler {

    private static DecimalFormat df2 = new DecimalFormat("#.##");

    private final ChatService chatService;

    private final TelegramLongPollingBot bot;

    @Autowired
    public H8SharesStatusButtonUpdateHandler(ChatService chatService, TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.bot = bot;
    }


    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            if (data.startsWith(FINISHED_SETTING_SHARES_CALLBACK_DATA)) {
                String[] ids = data.substring(FINISHED_SETTING_SHARES_CALLBACK_DATA.length()).split(DELIMITER);
                int receiptId = Integer.parseInt(ids[1]);
                long tgGroupChatId = Integer.parseInt(ids[0]);

                Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);

                return chatService.getActiveReceipt(groupChat).getId() == receiptId &&
                        groupChat.getChatState() == ChatState.DETECTING_OWNERS;
            }
        }
        return false;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        String[] ids = update.getCallbackQuery()
                .getData().substring(FINISHED_SETTING_SHARES_CALLBACK_DATA.length()).split(DELIMITER);


        int receiptId = Integer.parseInt(ids[1]);
        long tgGroupChatId = Integer.parseInt(ids[0]);
        Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);

        Receipt receipt = chatService.getActiveReceipt(groupChat);
        String response;
        if (receipt.getItems().size() != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("<b>These items are still not picked:</b> \n");
            for (ReceiptItem item : receipt.getItems()) {

                double pickedShare = item.getShares().stream().mapToDouble(Share::getShare).sum();
                if (item.getAmount() - pickedShare != 0) {

                    double unpickedShare = item.getAmount() - pickedShare;
                    sb.append("<pre>").append(item.getName()).append(" x ").append(df2.format(unpickedShare))
                            .append("</pre>");
                }
            }
            response = sb.toString();
        } else
            response = "<i>All items are picked!</i>";

        SendMessage message = new SendMessage()
                .setChatId(chat.getTelegramChatId())
                .setText(response)
                .setParseMode(ParseMode.HTML);
        bot.execute(message);

        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery()
                .setCallbackQueryId(update.getCallbackQuery().getId());
        bot.execute(answerCallbackQuery);
    }
}
