package xyz.dbotfactory.dbot.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import xyz.dbotfactory.dbot.model.Chat;

@Repository
public interface ChatRepository extends MongoRepository<Chat, Integer> {
    Chat findFirstByTelegramChatId(long chatId);
}
