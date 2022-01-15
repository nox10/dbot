package xyz.dbotfactory.dbot.handler.impl.callback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import static xyz.dbotfactory.dbot.handler.CommonConsts.DELIMITER;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShareEqualCallbackInfo {
    private static final String CALLBACK_ID = "share_equal";
    private static final String LABEL = "🍕 Share equally 🍕";

    private long telegramChatId;

    public static boolean canHandle(Update update) {
        if (update.hasCallbackQuery())
            return canHandle(update.getCallbackQuery().getData());
        else
            return false;
    }

    public InlineKeyboardButton getButton() {
        return InlineKeyboardButton.builder()
                .text(LABEL)
                .callbackData(CALLBACK_ID + DELIMITER + telegramChatId)
                .build();
    }

    public static boolean canHandle(String callbackData) {
        return callbackData.startsWith(CALLBACK_ID);
    }

    public static PayOffCallbackInfo fromCallbackData(String callbackData) {
        String[] split = callbackData.split(DELIMITER);
        long telegramChatId = Long.parseLong(split[1]);
        return new PayOffCallbackInfo(telegramChatId);
    }
}
