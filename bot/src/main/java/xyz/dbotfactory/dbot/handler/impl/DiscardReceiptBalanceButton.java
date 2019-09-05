package xyz.dbotfactory.dbot.handler.impl;

import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;

import static xyz.dbotfactory.dbot.handler.CommonConsts.DISCARD_RECEIPT_BALANCE_BUTTON;

public class DiscardReceiptBalanceButton implements UpdateHandler {
    @Override
    public boolean canHandle(Update update, Chat chat) {
        return update.hasCallbackQuery() &&
               update.getCallbackQuery().getData().startsWith(DISCARD_RECEIPT_BALANCE_BUTTON);
    }

    @Override
    public void handle(Update update, Chat chat) {

    }
}
