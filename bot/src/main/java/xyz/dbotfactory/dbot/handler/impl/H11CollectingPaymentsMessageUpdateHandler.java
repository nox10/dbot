package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.BigDecimalHelper;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
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
import static xyz.dbotfactory.dbot.BigDecimalHelper.create;
import static xyz.dbotfactory.dbot.BigDecimalHelper.isSmaller;
import static xyz.dbotfactory.dbot.handler.CommonConsts.*;
import static xyz.dbotfactory.dbot.model.ChatState.COLLECTING_PAYMENTS_INFO;
import static xyz.dbotfactory.dbot.model.ChatState.NO_ACTIVE_RECEIPT;

@Component
@Log
public class H11CollectingPaymentsMessageUpdateHandler implements UpdateHandler {

    private final ChatService chatService;


    private final ReceiptService receiptService;

    private final TelegramLongPollingBot bot;

    @Autowired
    public H11CollectingPaymentsMessageUpdateHandler(ChatService chatService, ReceiptService receiptService, TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return chat.getChatState() == COLLECTING_PAYMENTS_INFO;
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
            response = "<i>All good, receipt input completed.\n\nCurrent balances: \n" +
                    getPrettyChatBalanceStatuses(chat) + "</i>";
            chat.setChatState(NO_ACTIVE_RECEIPT);
            receipt.setActive(false);

            if (needPayoffButton(chat)) {
                InlineKeyboardButton howToPayOffButton = new InlineKeyboardButton()
                        .setText(SUGGEST_DEBT_RETURN_STATEGY_MESSAGE)
                        .setCallbackData(SUGGEST_DEBT_RETURN_STATEGY + DELIMITER + chat.getTelegramChatId());
                howToPayOffMarkup = new InlineKeyboardMarkup()
                        .setKeyboard(singletonList(singletonList(howToPayOffButton)));
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
        List<BalanceStatus> totalBalanceStatuses = chatService.getTotalBalanceStatuses(chat);
        StringBuilder sb = new StringBuilder();
        for (BalanceStatus balanceStatus : totalBalanceStatuses) {
            GetChat getChat = new GetChat(balanceStatus.getId());
            String userName = bot.execute(getChat).getUserName();
            sb.append("@").append(userName).append(" : ").append(balanceStatus.getAmount()).append("\n");
        }
        return sb.toString();
    }

    private boolean needPayoffButton(Chat chat) {
        List<BalanceStatus> totalBalanceStatuses = chatService.getTotalBalanceStatuses(chat);
        return totalBalanceStatuses.stream()
                .anyMatch(balance -> !balance.getAmount().equals(BigDecimalHelper.create(0)));
    }
}
