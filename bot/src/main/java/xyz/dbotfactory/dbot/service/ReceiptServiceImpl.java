package xyz.dbotfactory.dbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.BigDecimalUtils;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.helper.PrettyPrintUtils;
import xyz.dbotfactory.dbot.model.*;
import xyz.dbotfactory.dbot.repo.ReceiptRepository;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;

import static xyz.dbotfactory.dbot.BigDecimalUtils.*;

@Service
@Transactional
public class ReceiptServiceImpl implements ReceiptService, CommonConsts {
    private final ReceiptRepository receiptRepository;

    private final RecognService recognService;

    @Autowired
    public ReceiptServiceImpl(ReceiptRepository receiptRepository, RecognService recognService) {
        this.receiptRepository = receiptRepository;
        this.recognService = recognService;
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
        if (BigDecimalUtils.equals(shareAmount, 0.0)) {
            return "⚪ " + item.getName();
        } else {
            return "🔘 " + item.getName() + " — " + shareToStr(shareAmount);
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
                        .orElse(create(0)).compareTo(item.getAmount()) == 0)
                .reduce(Boolean::logicalAnd).orElseThrow(() -> new IllegalStateException("Empty receipt"));
    }

    @Override
    public String buildBeautifulReceiptString(Receipt receipt) {
        List<ReceiptItem> items = receipt.getItems();

        StringBuilder sb = new StringBuilder();
        sb.append("<pre>");
        sb.append(RECEIPT_LINE + '\n');
        int maxNameLength = items.stream().mapToInt(x -> x.getName().length()).max().orElse(0);
        int maxPriceLength = items.stream().mapToInt(x -> x.getPrice().toString().length()).max().orElse(0);
        for (ReceiptItem item : items) {
            String itemName = PrettyPrintUtils.padRight(item.getName(), maxNameLength);
            String price = PrettyPrintUtils.padRight(toStr(item.getPrice()), maxPriceLength);
            sb
                    .append(itemName).append(" : ")
                    .append(price).append(" x ")
                    .append(toStr(item.getAmount())).append("\n");
        }
        sb.append(RECEIPT_LINE + '\n');
        sb.append("TOTAL: ");
        sb.append(toStr(getTotalReceiptPrice(receipt)));
        sb.append("\n");
        sb.append(RECEIPT_LINE_SMALL + '\n');
        sb.append("</pre>");
        return sb.toString();
    }

    @Override
    public OCRResult parseReceipt(String imageUrl) {
        return this.recognService.parseReceipt(imageUrl);
    }
}
