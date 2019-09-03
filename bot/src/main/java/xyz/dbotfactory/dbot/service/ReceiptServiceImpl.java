package xyz.dbotfactory.dbot.service;

import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.UserBalance;

@Service
public class ReceiptServiceImpl implements ReceiptService {


    @Override
    public double getTotalReceiptPrice(Receipt receipt) {
        return receipt.getItems().stream().mapToDouble(x -> x.getAmount() * x.getPrice()).sum();
    }

    @Override
    public double getTotalBalance(Receipt receipt) {
        return receipt.getUserBalances().stream().mapToDouble(UserBalance::getBalance).sum();
    }
}
