package xyz.dbotfactory.dbot.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import xyz.dbotfactory.dbot.model.Receipt;

@Repository
public interface ReceiptRepository extends CrudRepository<Receipt, Integer> {
}
