package xyz.dbotfactory.dbot.handler;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.Receipt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static xyz.dbotfactory.dbot.BigDecimalUtils.create;
import static xyz.dbotfactory.dbot.BigDecimalUtils.toStr;
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
    public List<Message> sendTotalPriceForEachUser(Chat groupChat, Receipt receipt, TelegramLongPollingBot bot) {

        String[] pmUserIds = groupChat.getChatMetaInfo().getPmUserIds().split(DELIMITER);
        List<Message> sentMessages = new ArrayList<>();
        for (String pmUserId : pmUserIds) {
            BigDecimal totalPrice = getTotalPriceForUser(receipt, pmUserId);
            String text = "Your total price is " + toStr(totalPrice) + " \n" + GO_TO_GROUP_TEXT;
            sentMessages.add(botMessageHelper.sendSimpleMessage(text, Long.parseLong(pmUserId), bot));
        }

        groupChat.getChatMetaInfo().setPmUserIds("");
        return sentMessages;
    }

    private BigDecimal getTotalPriceForUser(Receipt receipt, String pmUserId) {
        return receipt.getItems()
                .stream()
                .flatMap(item -> item.getShares()
                        .stream()
                        .filter(share -> share.getTelegramUserId() == Long.parseLong(pmUserId))
                        .map(share -> share.getShare().multiply(item.getPrice()))
                )
                .reduce(BigDecimal::add).orElse(create(0));

    }
}
