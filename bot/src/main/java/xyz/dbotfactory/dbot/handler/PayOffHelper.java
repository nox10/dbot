package xyz.dbotfactory.dbot.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.BigDecimalUtils;
import xyz.dbotfactory.dbot.model.BalanceStatus;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.List;

import static xyz.dbotfactory.dbot.handler.CommonConsts.*;

@Component
public class PayOffHelper {
    @Autowired
    ChatService chatService;

    public InlineKeyboardButton getPayOffButton(long telegramChatId){
        return new InlineKeyboardButton()
                .setText(SUGGEST_DEBT_RETURN_STATEGY_MESSAGE)
                .setCallbackData(SUGGEST_DEBT_RETURN_STATEGY + DELIMITER + telegramChatId);
    }
    public InlineKeyboardButton getDiscardBalancesButton(long telegramChatId, int receiptId){
        return new InlineKeyboardButton()
                .setText(DISCARD_RECEIPT_BALANCE_BUTTON_TEXT)
                .setCallbackData(DISCARD_RECEIPT_BALANCE_BUTTON + DELIMITER + telegramChatId + DELIMITER + receiptId);
    }

    public boolean canSuggestPayOffStrategy(Chat chat) {
        List<BalanceStatus> totalBalanceStatuses = chatService.getTotalBalanceStatuses(chat);
        return totalBalanceStatuses.stream()
                .anyMatch(balance -> !balance.getAmount().equals(BigDecimalUtils.create(0)));
    }
}
