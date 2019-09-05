package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import xyz.dbotfactory.dbot.handler.PayOffHelper;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.impl.callback.DiscardReceiptBalanceCallbackInfo;
import xyz.dbotfactory.dbot.handler.impl.callback.PayOffCallbackInfo;
import xyz.dbotfactory.dbot.helper.PrettyPrintUtils;
import xyz.dbotfactory.dbot.model.BalanceStatus;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.UserBalance;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static xyz.dbotfactory.dbot.BigDecimalUtils.create;
import static xyz.dbotfactory.dbot.BigDecimalUtils.isSmaller;
import static xyz.dbotfactory.dbot.helper.PrettyPrintUtils.getPrettyBalanceStatuses;
import static xyz.dbotfactory.dbot.model.ChatState.COLLECTING_PAYMENTS_INFO;
import static xyz.dbotfactory.dbot.model.ChatState.NO_ACTIVE_RECEIPT;

@Component
@Log
public class H11CollectingPaymentsMessageUpdateHandler implements UpdateHandler {

    private final ChatService chatService;


    private final PayOffHelper payOffHelper;

    private final ReceiptService receiptService;

    private final TelegramLongPollingBot bot;

    @Autowired
    public H11CollectingPaymentsMessageUpdateHandler(ChatService chatService, ReceiptService receiptService, TelegramLongPollingBot bot, PayOffHelper payOffHelper) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.bot = bot;
        this.payOffHelper = payOffHelper;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {

        return update.hasMessage() &&
                !update.getMessage().isCommand() &&
                chat.getChatState() == COLLECTING_PAYMENTS_INFO;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        double payment;
        try {
            payment = Double.parseDouble(update.getMessage().getText());
        } catch (Throwable throwable) {
            // TODO: logging etc
            return;
        }
        Receipt receipt = chatService.getActiveReceipt(chat);
        long telegramUserId = update.getMessage().getFrom().getId();

        UserBalance userBalance = receipt.getUserBalances().stream()
                .filter(userBalance1 -> userBalance1.getTelegramUserId() == telegramUserId)
                .findAny()
                .orElse(UserBalance.builder().telegramUserId(telegramUserId).build());
        userBalance.setBalance(create(payment));
        if (!receipt.getUserBalances().contains(userBalance)) {
            receipt.getUserBalances().add(userBalance);
        }


        BigDecimal totalReceiptPrice = receiptService.getTotalReceiptPrice(receipt);
        BigDecimal totalBalance = receiptService.getTotalBalance(receipt);

        String response;

        InlineKeyboardMarkup howToPayOffMarkup = null;

        if (totalBalance.compareTo(totalReceiptPrice) == 0) {
            response = "<i>All good, receipt input completed.\n" +
                    getPrettyChatBalanceStatuses(chat) + "</i>";
            chat.setChatState(NO_ACTIVE_RECEIPT);
            receipt.setActive(false);

            if (true){//payOffHelper.canSuggestPayOffStrategy(chat)) {
                PayOffCallbackInfo payOffCallbackInfo =
                        new PayOffCallbackInfo(chat.getTelegramChatId());

                DiscardReceiptBalanceCallbackInfo discardReceiptBalanceCallbackInfo =

                        new DiscardReceiptBalanceCallbackInfo(chat.getTelegramChatId(), receipt.getId());

                howToPayOffMarkup = new InlineKeyboardMarkup()
                        .setKeyboard(singletonList(List.of(payOffCallbackInfo.getButton(),
                                discardReceiptBalanceCallbackInfo.getButton())));
            }
        } else if (isSmaller(totalBalance, totalReceiptPrice)) {
            response = "<i>Ok. Anyone else?\n\n" +
                    "Need " + totalReceiptPrice.subtract(totalBalance) + " more.</i>";
        } else { // totalBalance > totalReceiptPrice
            response = "<i>Ups, total sum is greater than receipt total (" + totalBalance + "vs" + totalReceiptPrice
                    + "). Can you pls check and type again?</i>";
            receipt.setUserBalances(new ArrayList<>());
        }
        chatService.save(chat);
        SendMessage message = new SendMessage()
                .setChatId(chat.getTelegramChatId())
                .setText(response)
                .setReplyMarkup(howToPayOffMarkup)
                .setParseMode(ParseMode.HTML);

        bot.execute(message);
    }

    @SneakyThrows
    private String getPrettyChatBalanceStatuses(Chat chat) {
        StringBuilder sb = new StringBuilder();
        sb.append("RECEIPT BALANCES: \n\n");
        List<BalanceStatus> receiptBalanceStatuses = chatService.getCurrentReceiptBalanceStatuses(chatService.getActiveReceipt(chat));
        sb.append(getPrettyBalanceStatuses(receiptBalanceStatuses, bot));
        sb.append("\n\n TOTAL BALANCES: \n\n");
        List<BalanceStatus> totalBalanceStatuses = chatService.getTotalBalanceStatuses(chat);
        sb.append(getPrettyBalanceStatuses(totalBalanceStatuses, bot));
        return sb.toString();
    }


}
