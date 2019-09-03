package xyz.dbotfactory.dbot.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import xyz.dbotfactory.dbot.model.Receipt;

@Component
public interface ReceiptRepository extends CrudRepository<Receipt, Integer> {
}
