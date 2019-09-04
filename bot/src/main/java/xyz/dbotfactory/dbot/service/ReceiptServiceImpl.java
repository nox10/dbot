package xyz.dbotfactory.dbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.BigDecimalHelper;
import xyz.dbotfactory.dbot.model.*;
import xyz.dbotfactory.dbot.repo.ReceiptRepository;

import javax.transaction.Transactional;
import java.math.BigDecimal;

import static xyz.dbotfactory.dbot.BigDecimalHelper.create;

@Service
@Transactional
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;

    @Autowired
    public ReceiptServiceImpl(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    @Override
    public BigDecimal getTotalReceiptPrice(Receipt receipt) {
        return receipt.getItems().stream().map(x -> x.getAmount().multiply(x.getPrice()))
                .reduce(BigDecimal::add)
                .orElse(create(0));
    }

    @Override
    public BigDecimal getTotalBalance(Receipt receipt) {
        return receipt.getUserBalances()
                .stream()
                .map(UserBalance::getBalance)
                .reduce(BigDecimal::add)
                .orElse(create(0));
    }

    @Override
    public BigDecimal shareLeft(ReceiptItem item, long userId) {
        if (item.getShares().isEmpty() ||
                (item.getShares().size() == 1 &&
                        item.getShares().get(0).getTelegramUserId() == userId)) {
            return item.getAmount();
        } else {
            BigDecimal otherSharesSum = item.getShares().stream()
                    .filter(share -> share.getTelegramUserId() != userId)
                    .map(Share::getShare)
                    .reduce(BigDecimal::add)
                    .orElse(create(0));
            return item.getAmount().subtract(otherSharesSum);
        }
    }

    @Override
    public String getShareStringForButton(ReceiptItem item, long telegramUserId) {
        BigDecimal shareAmount = item.getShares().stream()
                .filter(share -> share.getTelegramUserId() == telegramUserId)
                .findFirst().orElse(Share.builder().share(create(0)).build()).getShare();
        if (BigDecimalHelper.equals(shareAmount,0.0)) {
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
                        .reduce(BigDecimal::add)
                        .orElse(create(0)).equals(item.getAmount()))
                .reduce(Boolean::logicalAnd).orElseThrow(() -> new IllegalStateException("Empty receipt"));
    }
}
