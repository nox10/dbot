package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.ShareButtonCallbackInfo;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.DebtReturnTransaction;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.List;

import static xyz.dbotfactory.dbot.handler.CommonConsts.DELIMITER;
import static xyz.dbotfactory.dbot.handler.CommonConsts.SUGGEST_DEBT_RETURN_STATEGY;

@Component
public class H12SuggestDebtReturnStrategyButtonUpdateHandler implements UpdateHandler {
    @Autowired
    ChatService chatService;

    @Autowired
    TelegramLongPollingBot bot;

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            if (data.startsWith(SUGGEST_DEBT_RETURN_STATEGY)) {

                Chat groupChat = chatService.findOrCreateChatByTelegramId(getChatIdFromCallbackData(data));

                return groupChat.getChatState() == ChatState.NO_ACTIVE_RECEIPT ;

            }
        }

        return false;
    }

    private long getChatIdFromCallbackData(String data) {
        return Long.parseLong(data.split(DELIMITER)[1]);
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        long chatId = getChatIdFromCallbackData(update.getCallbackQuery().getData());
        chat = chatService.findOrCreateChatByTelegramId(chatId);

        List<DebtReturnTransaction> returnStrategy = chatService.getReturnStrategy(chat);

        String response = prettyPrintReturnStrategy(returnStrategy);
        if(!response.equals("")){
            SendMessage sendMessage = new SendMessage(chatId, response);
            bot.execute(sendMessage);
        }
    }

    @SneakyThrows
    private String prettyPrintReturnStrategy(List<DebtReturnTransaction> returnStrategy) {
        StringBuilder sb = new StringBuilder();
        for (DebtReturnTransaction debtReturnTransaction : returnStrategy) {
            GetChat getFromChat = new GetChat(debtReturnTransaction.getFromId());
            GetChat getToChat = new GetChat(debtReturnTransaction.getToId());
            String fromUsername = bot.execute(getFromChat).getUserName();
            String toUsername = bot.execute(getToChat).getUserName();

            String string = "@" + fromUsername + " -> " + "@" + toUsername + " : " + debtReturnTransaction.getAmount() + "\n";
            sb.append(string);
         }
        return sb.toString();
    }
}
