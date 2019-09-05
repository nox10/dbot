package xyz.dbotfactory.dbot.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.dbotfactory.dbot.BigDecimalUtils;
import xyz.dbotfactory.dbot.model.BalanceStatus;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.List;

@Component
public class PayOffHelper {
    @Autowired
    ChatService chatService;

    public boolean canSuggestPayOffStrategy(Chat chat) {
        List<BalanceStatus> totalBalanceStatuses = chatService.getTotalBalanceStatuses(chat);
        return totalBalanceStatuses.stream()
                .anyMatch(balance -> !balance.getAmount().equals(BigDecimalUtils.create(0)));
    }
}
