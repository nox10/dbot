package xyz.dbotfactory.dbot.handler.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.BigDecimalUtils;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.impl.callback.CountMeInCallbackInfo;
import xyz.dbotfactory.dbot.handler.impl.callback.PayOffCallbackInfo;
import xyz.dbotfactory.dbot.model.BalanceStatus;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.UserBalance;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static xyz.dbotfactory.dbot.BigDecimalUtils.create;
import static xyz.dbotfactory.dbot.helper.PrettyPrintUtils.getPrettyBalanceStatuses;
import static xyz.dbotfactory.dbot.model.ChatState.DETECTING_OWNERS;

@Component
public class CountMeInUpdateHandler implements UpdateHandler {

    private final ChatService chatService;

    private final ReceiptService receiptService;

    private final BotMessageHelper messageHelper;

    private final TelegramLongPollingBot bot;

    @Autowired
    public CountMeInUpdateHandler(ChatService chatService, ReceiptService receiptService, BotMessageHelper messageHelper, TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.messageHelper = messageHelper;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if(CountMeInCallbackInfo.canHandle(update)){
            PayOffCallbackInfo callbackInfo = CountMeInCallbackInfo.fromCallbackData(update.getCallbackQuery().getData());
            chat = chatService.findOrCreateChatByTelegramId(callbackInfo.getTelegramChatId());
            return chat.getChatState() == DETECTING_OWNERS;
        }
        return false;
    }

    @Override
    public void handle(Update update, Chat chat) {
        PayOffCallbackInfo callbackInfo = CountMeInCallbackInfo.fromCallbackData(update.getCallbackQuery().getData());
        chat = chatService.findOrCreateChatByTelegramId(callbackInfo.getTelegramChatId());

        Receipt activeReceipt = chatService.getActiveReceipt(chat);

        BigDecimal totalBalance = receiptService.getTotalReceiptPrice(activeReceipt);
        int id = update.getCallbackQuery().getFrom().getId();

        if(countInNewUser(activeReceipt, id)){
            activeReceipt.getUserBalances().add(UserBalance.builder().telegramUserId(id).build());
            BigDecimal share = totalBalance.divide(create(activeReceipt.getUserBalances().size()));
            for (UserBalance userBalance : activeReceipt.getUserBalances()) {
                userBalance.setBalance(share);
            }
        }

        chatService.save(chat);
        List<BalanceStatus> collect = activeReceipt.getUserBalances()
                .stream()
                .map(x -> new BalanceStatus(x.getTelegramUserId(), x.getBalance()))
                .collect(toList());
        String message = "Payments:\n" +getPrettyBalanceStatuses(collect, bot);
        messageHelper.sendSimpleMessage(message, chat.getTelegramChatId(), bot);
        messageHelper.notifyCallbackProcessed(update.getCallbackQuery().getId(), bot);
    }

    private boolean countInNewUser(Receipt receipt, int id) {
        return receipt.getUserBalances().stream().noneMatch(x -> x.getTelegramUserId() == id);
    }
}
