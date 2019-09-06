package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.PayOffHelper;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.impl.callback.DiscardReceiptBalanceCallbackInfo;
import xyz.dbotfactory.dbot.handler.impl.callback.PayOffCallbackInfo;
import xyz.dbotfactory.dbot.handler.impl.callback.PayOffChatCallbackInfo;
import xyz.dbotfactory.dbot.model.BalanceStatus;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.UserBalance;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static xyz.dbotfactory.dbot.BigDecimalUtils.*;
import static xyz.dbotfactory.dbot.helper.PrettyPrintUtils.getPrettyBalanceStatuses;
import static xyz.dbotfactory.dbot.model.ChatState.COLLECTING_PAYMENTS_INFO;
import static xyz.dbotfactory.dbot.model.ChatState.NO_ACTIVE_RECEIPT;

@Component
@Log
public class H11CollectingPaymentsMessageUpdateHandler implements UpdateHandler, CommonConsts {

    private final ChatService chatService;

    private final PayOffHelper payOffHelper;

    private final ReceiptService receiptService;

    private final TelegramLongPollingBot bot;

    private final BotMessageHelper botMessageHelper;

    @Autowired
    public H11CollectingPaymentsMessageUpdateHandler(ChatService chatService, ReceiptService receiptService,
                                                     TelegramLongPollingBot bot, PayOffHelper payOffHelper,
                                                     BotMessageHelper botMessageHelper) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.bot = bot;
        this.payOffHelper = payOffHelper;
        this.botMessageHelper = botMessageHelper;
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
        int telegramUserId = update.getMessage().getFrom().getId();

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

        List<String> whenToExecuteDeleteTask;

        if (totalBalance.compareTo(totalReceiptPrice) == 0) {
            response = "✔️ All good, receipt input is completed.\n" +
                    getPrettyChatBalanceStatuses(chat);
            whenToExecuteDeleteTask = asList(
                    H12SuggestDebtReturnStrategyButtonUpdateHandler.class.getSimpleName(), // TODO
                    H1NewReceiptCommandUpdateHandler.class.getSimpleName());
            chat.setChatState(NO_ACTIVE_RECEIPT);
            receipt.setActive(false);

            botMessageHelper.executeExistingTasks(RECEIPT_BALANCES_BUILT, chat.getChatMetaInfo(), bot,
                    update.getMessage().getFrom().getId());

            if (true) {//payOffHelper.canSuggestPayOffStrategy(chat)) {
                PayOffCallbackInfo payOffCallbackInfo =
                        new PayOffCallbackInfo(chat.getTelegramChatId());

                DiscardReceiptBalanceCallbackInfo discardReceiptBalanceCallbackInfo =
                        new DiscardReceiptBalanceCallbackInfo(chat.getTelegramChatId(), receipt.getId());

                PayOffChatCallbackInfo payOffChatCallbackInfo = new PayOffChatCallbackInfo(chat.getTelegramChatId());

                howToPayOffMarkup = new InlineKeyboardMarkup()
                        .setKeyboard(List.of(List.of(payOffCallbackInfo.getButton()),
                                List.of(discardReceiptBalanceCallbackInfo.getButton()),
                                List.of(payOffChatCallbackInfo.getButton())));


            }
        } else if (isSmaller(totalBalance, totalReceiptPrice)) {
            whenToExecuteDeleteTask = singletonList(this.getClass().getSimpleName());
            response = "✔️ Ok. Anyone else?\n\n" +
                    "Need " + toStr(totalReceiptPrice.subtract(totalBalance)) + " more.";
        } else { // totalBalance > totalReceiptPrice
            response = "❗️ Ups, total sum is greater than receipt total (" + totalBalance + "vs" + totalReceiptPrice
                    + "). Can you pls check and type again?";
            whenToExecuteDeleteTask = singletonList(RECEIPT_BALANCES_BUILT);
            receipt.setUserBalances(new ArrayList<>());
        }

        SendMessage message = new SendMessage()
                .setChatId(chat.getTelegramChatId())
                .setText(response)
                .setReplyMarkup(howToPayOffMarkup)
                .setParseMode(ParseMode.HTML);

        Message sentMessage = bot.execute(message);

        botMessageHelper.deleteMessage(bot, update.getMessage());
        botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(),
                chat.getChatMetaInfo(), bot, telegramUserId);
        for (String event : whenToExecuteDeleteTask) {
            addCleanupTasks(chat, sentMessage, event);
        }

        chatService.save(chat);
    }

    private void addCleanupTasks(Chat chat, Message sentMessage, String event) {
        botMessageHelper.addNewTask(event, chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
    }

    @SneakyThrows
    private String getPrettyChatBalanceStatuses(Chat chat) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n<code>" + RECEIPT_LINE + "</code>");
        sb.append("\n<b>RECEIPT BALANCES</b>");
        sb.append("\n<code>" + RECEIPT_LINE_SMALL + "\n</code>");
        List<BalanceStatus> receiptBalanceStatuses = chatService.getCurrentReceiptBalanceStatuses(chatService.getActiveReceipt(chat));
        sb.append(getPrettyBalanceStatuses(receiptBalanceStatuses, bot));
        sb.append("\n<code>" + RECEIPT_LINE + "</code>");
        sb.append("\n<b>TOTAL BALANCES</b>");
        sb.append("\n<code>" + RECEIPT_LINE + "\n</code>");
        List<BalanceStatus> totalBalanceStatuses = chatService.getTotalBalanceStatuses(chat);
        sb.append(getPrettyBalanceStatuses(totalBalanceStatuses, bot));
        return sb.toString();
    }
}
