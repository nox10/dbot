package xyz.dbotfactory.dbot.service;

import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.ReceiptItem;
import xyz.dbotfactory.dbot.model.Share;
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

    @Override
    public double shareLeft(ReceiptItem item, long userId) {
        if (item.getShares().isEmpty() ||
                item.getShares().size() == 1 &&
                        item.getShares().stream()
                                .anyMatch(share -> share.getTelegramUser().getTelegramId() == userId)) {
            return item.getAmount();
        } else {
            double otherSharesSum = item.getShares().stream()
                    .filter(share -> share.getTelegramUser().getTelegramId() != userId)
                    .map(Share::getShare).reduce(Double::sum).get();
            return item.getAmount() - otherSharesSum;
        }
    }

    @Override
    public String getShareStringForButton(ReceiptItem item, long telegramUserId) {
        double shareAmount = item.getShares().stream()
                .filter(share -> share.getTelegramUser().getTelegramId() == telegramUserId)
                .findFirst().orElse(Share.builder().share(0.0).build()).getShare();
        if (shareAmount == 0.0) {
            return "";
        } else {
            return " — " + shareAmount;
        }
    }
}
