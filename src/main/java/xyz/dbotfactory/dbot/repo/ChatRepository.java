package xyz.dbotfactory.dbot.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import xyz.dbotfactory.dbot.model.Chat;

@Repository
public interface ChatRepository extends CrudRepository<Chat, Integer> {
}
