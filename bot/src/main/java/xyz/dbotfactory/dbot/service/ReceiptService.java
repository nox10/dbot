package xyz.dbotfactory.dbot.service;

import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.ReceiptItem;
import xyz.dbotfactory.dbot.model.Share;

import java.math.BigDecimal;

@Service
public interface ReceiptService {

    BigDecimal getTotalReceiptPrice(Receipt receipt);

    BigDecimal getTotalBalance(Receipt receipt);

    BigDecimal shareLeft(ReceiptItem item, long userId);

    String getShareStringForButton(ReceiptItem item, long telegramUserId);

    void save(Receipt receipt);

    boolean allSharesDone(Receipt receipt);
}
