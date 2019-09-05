package xyz.dbotfactory.dbot.service;

import xyz.dbotfactory.dbot.model.BalanceStatus;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.DebtReturnTransaction;
import xyz.dbotfactory.dbot.model.Receipt;

import java.util.List;

public interface ChatService {
    Chat findOrCreateChatByTelegramId(long chatId);

    Receipt getActiveReceipt(Chat chat);

    List<BalanceStatus> getTotalBalanceStatuses(Chat chat);
    List<BalanceStatus> getCurrentReceiptBalanceStatuses(Receipt receipt);
    List<DebtReturnTransaction> getReturnStrategy(Chat chat);

    void save(Chat chat);

    void removeActiveReceipt(Chat chat);
}
