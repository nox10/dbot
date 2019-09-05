package xyz.dbotfactory.dbot.handler;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.Receipt;

import java.math.BigDecimal;

import static xyz.dbotfactory.dbot.BigDecimalHelper.create;
import static xyz.dbotfactory.dbot.handler.CommonConsts.DELIMITER;
import static xyz.dbotfactory.dbot.handler.CommonConsts.GO_TO_GROUP_TEXT;

@Service
public class SharePickerHelper {

    private final BotMessageHelper botMessageHelper;

    @Autowired
    public SharePickerHelper(BotMessageHelper botMessageHelper) {
        this.botMessageHelper = botMessageHelper;
    }

    @SneakyThrows
    public void sendTotalPriceForEachUser(Chat groupChat, Receipt receipt, TelegramLongPollingBot bot) {

        String[] pmUserIds = groupChat.getChatMetaInfo().getPmUserIds().split(DELIMITER);
        for (String pmUserId : pmUserIds) {
            long chatId = Long.parseLong(pmUserId);
            BigDecimal totalPrice = getTotalPriceForUser(receipt, chatId);
            String text = "Your total price is " + totalPrice + " \n" + GO_TO_GROUP_TEXT;
            botMessageHelper.sendSimpleMessageToChat(text, chatId, bot);
        }

        groupChat.getChatMetaInfo().setPmUserIds("");
    }

    private BigDecimal getTotalPriceForUser(Receipt receipt, long pmUserId) {
        return receipt.getItems()
                .stream()
                .flatMap(item -> item.getShares()
                        .stream()
                        .filter(share -> share.getTelegramUserId() == pmUserId)
                        .map(share -> share.getShare().multiply(item.getPrice()))
                )
                .reduce(BigDecimal::add).orElse(create(0));

    }
}
