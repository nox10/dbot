package xyz.dbotfactory.dbot.service;

import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.model.BalanceStatus;
import xyz.dbotfactory.dbot.model.DebtReturnTransaction;

import java.util.List;

@Service
public interface CalcService {
    List<BalanceStatus> getTotalBalance(List<BalanceStatus> balanceStatuses);
    List<DebtReturnTransaction> getReturnStrategy(List<BalanceStatus> balanceStatuses);
}
