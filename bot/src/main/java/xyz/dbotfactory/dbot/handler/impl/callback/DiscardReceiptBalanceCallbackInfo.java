package xyz.dbotfactory.dbot.handler.impl.callback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import static xyz.dbotfactory.dbot.handler.CommonConsts.DELIMITER;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiscardReceiptBalanceCallbackInfo {

    private static final String CALLBACK_ID = "discard_receipt_item";
    private static final String LABEL = "✅ Close receipt ✅";

    private long telegramChatId;
    private int receiptId;

    public InlineKeyboardButton getButton() {
        return new InlineKeyboardButton()
                .setText(LABEL)
                .setCallbackData(CALLBACK_ID + DELIMITER + telegramChatId + DELIMITER + receiptId);
    }

    public static boolean canHandle(String callbackData) {
        return callbackData.startsWith(CALLBACK_ID);
    }

    public static DiscardReceiptBalanceCallbackInfo fromCallbackData(String callbackData) {
        String[] split = callbackData.split(DELIMITER);
        long telegramChatId = Long.parseLong(split[1]);
        int receiptId = Integer.parseInt(split[2]);
        return new DiscardReceiptBalanceCallbackInfo(telegramChatId, receiptId);
    }
}
