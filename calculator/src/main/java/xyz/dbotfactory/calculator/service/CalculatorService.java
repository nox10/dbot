package xyz.dbotfactory.calculator.service;

import org.springframework.stereotype.Service;
import xyz.dbotfactory.calculator.model.*;

import java.util.List;

@Service
public interface CalculatorService {

    List<BalanceChange> calculateTotalBalance(List<BalanceChange> request);

    List<DebtReturnTransaction> getDebtReturnStrategy(List<BalanceChange> request);
}
