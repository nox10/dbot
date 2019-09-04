package xyz.dbotfactory.dbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.model.TelegramUser;
import xyz.dbotfactory.dbot.repo.TelegramUserRepository;

import javax.transaction.Transactional;

@Service
@Transactional
public class TelegramUserServiceImpl implements TelegramUserService {
    private final TelegramUserRepository repository;

    @Autowired
    public TelegramUserServiceImpl(TelegramUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public TelegramUser getTelegramUserByTgId(long telegramId) {
        TelegramUser telegramUser = repository.findTelegramUserByTelegramId(telegramId);
        if (telegramUser == null) {
            telegramUser = TelegramUser.builder()
                    .telegramId(telegramId).build();
            telegramUser = repository.save(telegramUser);
        }
        return telegramUser;
    }
}
