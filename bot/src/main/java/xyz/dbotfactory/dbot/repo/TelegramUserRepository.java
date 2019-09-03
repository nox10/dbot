package xyz.dbotfactory.dbot.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import xyz.dbotfactory.dbot.model.TelegramUser;

@Repository
public interface TelegramUserRepository extends CrudRepository<TelegramUser, Integer> {
       TelegramUser findTelegramUserByTelegramId(long telegramId);
}
