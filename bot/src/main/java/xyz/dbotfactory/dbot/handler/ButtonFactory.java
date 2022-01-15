package xyz.dbotfactory.dbot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import static java.util.Collections.singletonList;

@Component
public class ButtonFactory {

    public InlineKeyboardMarkup getSingleButton(String label, String callbackData) {
        InlineKeyboardButton collectingStatusButton = InlineKeyboardButton.builder()
                .text(label)
                .callbackData(callbackData)
                .build();
        return InlineKeyboardMarkup.builder()
                .keyboard(singletonList(singletonList(collectingStatusButton))).build();
    }
}
