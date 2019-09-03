package xyz.dbotfactory.dbot.handler.impl;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;

@Service
@Log
public class ItemButtonHandlerImpl implements UpdateHandler, CommonConsts {
    @Override
    public boolean canHandle(Update update, Chat chat) {
        return chat.getChatState() == ChatState.DETECTING_OWNERS &&
                update.hasCallbackQuery() &&
                update.getCallbackQuery().getData().split(DELIMITER)[0]
                        .equals(ITEM_BUTTON_CALLBACK_DATA_PREFIX);
    }

    @Override
    public void handle(Update update, Chat chat) {
        // TODO
    }
}
