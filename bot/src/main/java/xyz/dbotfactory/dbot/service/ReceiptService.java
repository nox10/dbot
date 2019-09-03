package xyz.dbotfactory.dbot.service;

import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.model.Receipt;

@Service
public interface ReceiptService {

    double getTotalReceiptPrice(Receipt receipt);

    double getTotalBalance(Receipt receipt);
}
