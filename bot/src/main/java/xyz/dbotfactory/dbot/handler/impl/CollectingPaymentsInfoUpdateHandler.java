package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.TelegramUser;
import xyz.dbotfactory.dbot.model.UserBalance;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;
import xyz.dbotfactory.dbot.service.TelegramUserService;

import java.util.ArrayList;

import static xyz.dbotfactory.dbot.model.ChatState.COLLECTING_PAYMENTS_INFO;
import static xyz.dbotfactory.dbot.model.ChatState.NO_ACTIVE_RECEIPT;

@Component
@Log
public class CollectingPaymentsInfoUpdateHandler implements UpdateHandler {

    private final ChatService chatService;

    private final TelegramUserService telegramUserService;

    private final ReceiptService receiptService;

    private final TelegramLongPollingBot bot;

    @Autowired
    public CollectingPaymentsInfoUpdateHandler(ChatService chatService, TelegramUserService telegramUserService, ReceiptService receiptService, TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.telegramUserService = telegramUserService;
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
        TelegramUser user = telegramUserService.getTelegramUserByTgId(update.getMessage().getFrom().getId());

        UserBalance userBalance = UserBalance
                .builder()
                .user(user)
                .balance(payment)
                .build();
        receipt.getUserBalances().add(userBalance);

        double totalReceiptPrice = receiptService.getTotalReceiptPrice(receipt);
        double totalBalance = receiptService.getTotalBalance(receipt);

        String response;

        if (totalBalance == totalReceiptPrice) {
            response = "All good, receipt input completed";
            chat.setChatState(NO_ACTIVE_RECEIPT);
            receipt.setActive(false);
        } else if (totalBalance < totalReceiptPrice) {
            response = "";
            //TODO: ask nikita
        } else {// totalBalance > totalReceiptPrice
            response = "<i>Ups, total sum is greater than receipt total (" + totalBalance + "vs" + totalReceiptPrice
                    + "). Can you pls check and type again?</i>";
            receipt.setUserBalances(new ArrayList<>());
        }
        chatService.save(chat);
        SendMessage message = new SendMessage()
                .setChatId(chat.getTelegramChatId())
                .setText(response)
                .setParseMode(ParseMode.HTML);

        bot.execute(message);
    }
}
