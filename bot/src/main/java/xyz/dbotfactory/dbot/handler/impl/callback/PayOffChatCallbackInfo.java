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
public class PayOffChatCallbackInfo {
    private static final String CALLBACK_ID = "pay_off_chat";
    private static final String LABEL = "✅ Close all receipts ✅";

    private long telegramChatId;

    public InlineKeyboardButton getButton() {
        return InlineKeyboardButton.builder()
                .text(LABEL)
                .callbackData(CALLBACK_ID + DELIMITER + telegramChatId)
                .build();
    }

    public static boolean canHandle(String callbackData) {
        return callbackData.startsWith(CALLBACK_ID);
    }

    public static PayOffChatCallbackInfo fromCallbackData(String callbackData) {
        String[] split = callbackData.split(DELIMITER);
        long telegramChatId = Long.parseLong(split[1]);
        return new PayOffChatCallbackInfo(telegramChatId);
    }
}
