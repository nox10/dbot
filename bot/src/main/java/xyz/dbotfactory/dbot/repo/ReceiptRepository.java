package xyz.dbotfactory.dbot.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import xyz.dbotfactory.dbot.model.Receipt;

@Repository
public interface ReceiptRepository extends MongoRepository<Receipt, Integer> {
}
