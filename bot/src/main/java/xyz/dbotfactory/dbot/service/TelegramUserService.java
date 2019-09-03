package xyz.dbotfactory.dbot.service;

import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.model.TelegramUser;

@Service
public interface TelegramUserService {
    TelegramUser getTelegramUserByTgId(long telegramId);
}
