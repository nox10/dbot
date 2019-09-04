package xyz.dbotfactory.dbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.ReceiptItem;
import xyz.dbotfactory.dbot.model.Share;
import xyz.dbotfactory.dbot.model.UserBalance;
import xyz.dbotfactory.dbot.repo.ReceiptRepository;

import javax.transaction.Transactional;

@Service
@Transactional
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;

    @Autowired
    public ReceiptServiceImpl(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

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
                                .anyMatch(share -> share.getTelegramUserId() == userId)) {
            return item.getAmount();
        } else {
            double otherSharesSum = item.getShares().stream()
                    .filter(share -> share.getTelegramUserId() != userId)
                    .map(Share::getShare).reduce(Double::sum).get();
            return item.getAmount() - otherSharesSum;
        }
    }

    @Override
    public String getShareStringForButton(ReceiptItem item, long telegramUserId) {
        double shareAmount = item.getShares().stream()
                .filter(share -> share.getTelegramUserId() == telegramUserId)
                .findFirst().orElse(Share.builder().share(0.0).build()).getShare();
        if (shareAmount == 0.0) {
            return "";
        } else {
            return " — " + shareAmount;
        }
    }

    @Override
    public void save(Receipt receipt) {
        receiptRepository.save(receipt);
    }

    @Override
    public boolean allSharesDone(Receipt receipt) {
        return receipt.getItems().stream()
                .map(item -> item.getShares().stream()
                        .map(Share::getShare)
                        .reduce(Double::sum).get() == item.getAmount())
                .reduce((expr1, expr2) -> expr1 & expr2).get();

    }
}
