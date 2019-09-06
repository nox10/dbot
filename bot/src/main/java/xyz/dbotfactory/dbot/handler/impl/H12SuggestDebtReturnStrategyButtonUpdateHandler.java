package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.impl.callback.PayOffCallbackInfo;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.DebtReturnTransaction;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.List;

import static xyz.dbotfactory.dbot.BigDecimalUtils.toStr;


@Component
public class H12SuggestDebtReturnStrategyButtonUpdateHandler implements UpdateHandler {

    private final ChatService chatService;
    private final TelegramLongPollingBot bot;
    private final BotMessageHelper messageHelper;

    @Autowired
    public H12SuggestDebtReturnStrategyButtonUpdateHandler(ChatService chatService, TelegramLongPollingBot bot, BotMessageHelper messageHelper) {
        this.chatService = chatService;
        this.bot = bot;
        this.messageHelper = messageHelper;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            if (PayOffCallbackInfo.canHandle(data)) {
                PayOffCallbackInfo callbackInfo = PayOffCallbackInfo.fromCallbackData(data);
                Chat groupChat = chatService.findOrCreateChatByTelegramId(callbackInfo.getTelegramChatId());

                return groupChat.getChatState() == ChatState.NO_ACTIVE_RECEIPT;
            }
        }

        return false;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        String data = update.getCallbackQuery().getData();
        PayOffCallbackInfo callbackInfo = PayOffCallbackInfo.fromCallbackData(data);

        chat = chatService.findOrCreateChatByTelegramId(callbackInfo.getTelegramChatId());

        List<DebtReturnTransaction> returnStrategy = chatService.getReturnStrategy(chat);

        String response = prettyPrintReturnStrategy(returnStrategy);
        if (!response.equals("")) {
            Message sentMessage = messageHelper.sendSimpleMessage(response, callbackInfo.getTelegramChatId(), bot);
            messageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                    chat.getChatMetaInfo(), sentMessage);
            messageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                    chat.getChatMetaInfo(), sentMessage);
        } else {
            AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery()
                    .setCallbackQueryId(update.getCallbackQuery().getId())
                    .setText("Seems like you don't need any return strategy")
                    .setShowAlert(true);
            bot.execute(answerCallbackQuery);
        }

        messageHelper.notifyCallbackProcessed(update.getCallbackQuery().getId(), bot);
        messageHelper.executeExistingTasks(this.getClass().getSimpleName(),
                chat.getChatMetaInfo(), bot, update.getCallbackQuery().getFrom().getId());

        chatService.save(chat);
    }

    @SneakyThrows
    private String prettyPrintReturnStrategy(List<DebtReturnTransaction> returnStrategy) {
        StringBuilder sb = new StringBuilder();
        for (DebtReturnTransaction debtReturnTransaction : returnStrategy) {
            GetChat getFromChat = new GetChat(debtReturnTransaction.getFromId());
            GetChat getToChat = new GetChat(debtReturnTransaction.getToId());
            String fromUsername = bot.execute(getFromChat).getUserName();
            String toUsername = bot.execute(getToChat).getUserName();

            String string = "@" + fromUsername + " -> " + "@" + toUsername + " : "
                    + toStr(debtReturnTransaction.getAmount()) + "\n";
            sb.append(string);
        }
        return sb.toString();
    }
}
