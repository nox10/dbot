package xyz.dbotfactory.dbot.handler.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.impl.callback.CountMeInCallbackInfo;
import xyz.dbotfactory.dbot.handler.impl.callback.DiscardReceiptBalanceCallbackInfo;
import xyz.dbotfactory.dbot.handler.impl.callback.PayOffCallbackInfo;
import xyz.dbotfactory.dbot.model.BalanceStatus;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.UserBalance;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static xyz.dbotfactory.dbot.BigDecimalUtils.create;
import static xyz.dbotfactory.dbot.BigDecimalUtils.divide;
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
        if (CountMeInCallbackInfo.canHandle(update)) {
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

        if (countInNewUser(activeReceipt, id)) {
            activeReceipt.getUserBalances().add(UserBalance.builder().telegramUserId(id).build());
            BigDecimal share = divide(totalBalance,create(activeReceipt.getUserBalances().size()));
            for (UserBalance userBalance : activeReceipt.getUserBalances()) {
                userBalance.setBalance(share);
            }
        }

        List<BalanceStatus> collect = activeReceipt.getUserBalances()
                .stream()
                .map(x -> new BalanceStatus(x.getTelegramUserId(), x.getBalance()))
                .collect(toList());
        String message = "<b>Payments:</b>\n\n" + getPrettyBalanceStatuses(collect, bot);
        DiscardReceiptBalanceCallbackInfo discardReceiptBalanceCallbackInfo =
                new DiscardReceiptBalanceCallbackInfo(chat.getTelegramChatId(), activeReceipt.getId());
        InlineKeyboardButton button = discardReceiptBalanceCallbackInfo.getButton();
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup().setKeyboard(singletonList(singletonList(button)));
        Message sentMessage = messageHelper.sendMessageWithSingleInlineMarkup(chat.getTelegramChatId(), markup , bot, message);

        messageHelper.notifyCallbackProcessed(update.getCallbackQuery().getId(), bot);

        messageHelper.executeExistingTasks(this.getClass().getSimpleName(),
                chat.getChatMetaInfo(), bot, update.getCallbackQuery().getFrom().getId());
        messageHelper.addNewTask(this.getClass().getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        messageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        messageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);

        chatService.save(chat);
    }

    private boolean countInNewUser(Receipt receipt, int id) {
        return receipt.getUserBalances().stream().noneMatch(x -> x.getTelegramUserId() == id);
    }
}
