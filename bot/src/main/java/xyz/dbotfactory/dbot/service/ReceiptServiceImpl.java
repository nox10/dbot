package xyz.dbotfactory.dbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.BigDecimalUtils;
import xyz.dbotfactory.dbot.helper.PrettyPrintHelper;
import xyz.dbotfactory.dbot.model.*;
import xyz.dbotfactory.dbot.repo.ReceiptRepository;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;

import static xyz.dbotfactory.dbot.BigDecimalUtils.create;

@Service
@Transactional
public class ReceiptServiceImpl implements ReceiptService {
    private static final String RECEIPT_LINE = "=====================\n";

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

    @Override
    public String buildBeautifulReceiptString(Receipt receipt) {
        List<ReceiptItem> items = receipt.getItems();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<pre>");
        stringBuilder.append(RECEIPT_LINE);
        int maxNameLength = items.stream().mapToInt(x -> x.getName().length()).max().orElse(0);
        int maxPriceLength = items.stream().mapToInt(x -> String.valueOf(x.getPrice()).length()).max().orElse(0);
        for (ReceiptItem item : items) {
            String itemName = PrettyPrintHelper.padRight(item.getName(), maxNameLength);
            String price = PrettyPrintHelper.padRight(String.valueOf(item.getPrice()), maxPriceLength);
            stringBuilder
                    .append(itemName).append(" : ")
                    .append(price).append(" x ")
                    .append(item.getAmount()).append("\n");
        }
        stringBuilder.append(RECEIPT_LINE);
        stringBuilder.append("TOTAL: ");
        stringBuilder.append(getTotalReceiptPrice(receipt));
        stringBuilder.append("\n");
        stringBuilder.append(RECEIPT_LINE);
        stringBuilder.append("</pre>");
        return stringBuilder.toString();
    }

    @Override
    public OCRResult parseReceipt(String imageUrl) {
        return this.recognService.parseReceipt(imageUrl);
    }
}
