package xyz.dbotfactory.dbot.service;

import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.ReceiptItem;
import xyz.dbotfactory.dbot.model.Share;

@Service
public interface ReceiptService {

    double getTotalReceiptPrice(Receipt receipt);

    double getTotalBalance(Receipt receipt);

    double shareLeft(ReceiptItem item, long userId);

    String getShareStringForButton(ReceiptItem item, long telegramUserId);

    void save(Receipt receipt);

    boolean allSharesDone(Receipt receipt);
}
