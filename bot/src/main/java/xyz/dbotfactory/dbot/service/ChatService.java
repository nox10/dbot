package xyz.dbotfactory.dbot.service;

import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.Receipt;

public interface ChatService {
    Chat findOrCreateChatByTelegramId(long chatId);

    Receipt getActiveReceipt(Chat chat);

    void save(Chat chat);
}
