package xyz.dbotfactory.dbot.helper;

import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import xyz.dbotfactory.dbot.model.BalanceStatus;

import java.util.List;

import static xyz.dbotfactory.dbot.BigDecimalUtils.toStr;

public class PrettyPrintUtils {
    public static String padRight(String inputString, int length) {
        if (inputString.length() >= length)
            return inputString;

        StringBuilder sb = new StringBuilder();
        sb.append(inputString);

        while (sb.length() < length)
            sb.append(' ');

        return sb.toString();
    }

    @SneakyThrows
    public static String getPrettyBalanceStatuses(List<BalanceStatus> totalBalanceStatuses, TelegramLongPollingBot bot) {
        StringBuilder sb = new StringBuilder();
        for (BalanceStatus balanceStatus : totalBalanceStatuses) {
            GetChat getChat = new GetChat(Long.toString(balanceStatus.getId()));
            String userName = bot.execute(getChat).getUserName();
            sb.append("💵 @").append(userName).append(" : <code>").append(toStr(balanceStatus.getAmount()))
                    .append("</code>\n");
        }
        return sb.toString();
    }

}
