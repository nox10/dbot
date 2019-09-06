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
public class CountMeInCallbackInfo {
    private static final String CALLBACK_ID = "count_me_in";
    private static final String LABEL = "💰 Count me! 💰";

    private long telegramChatId;

    public static boolean canHandle(Update update) {
        if (update.hasCallbackQuery())
            return canHandle(update.getCallbackQuery().getData());
        else
            return false;
    }

    public InlineKeyboardButton getButton() {
        return new InlineKeyboardButton()
                .setText(LABEL)
                .setCallbackData(CALLBACK_ID + DELIMITER + telegramChatId);
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
