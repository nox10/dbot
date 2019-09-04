package xyz.dbotfactory.dbot.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xyz.dbotfactory.dbot.model.BalanceStatus;
import xyz.dbotfactory.dbot.model.DebtReturnTransaction;

import java.util.List;
import java.util.Objects;

@Service
public class CalcServiceImpl implements CalcService {

    private static final String TOTAL_BALANCE_URI = "http://localhost:8081/calc/total_balance";
    private static final String RETURN_STRATEGY_URI = "http://localhost:8081/calc/return_strategy";

    @Override
    public List<BalanceStatus> getTotalBalance(List<BalanceStatus> balanceStatuses) {
        return  List.of(Objects.requireNonNull(new RestTemplate().postForObject(TOTAL_BALANCE_URI, balanceStatuses, BalanceStatus[].class)));
    }

    @Override
    public List<DebtReturnTransaction> getReturnStrategy(List<BalanceStatus> balanceStatuses) {
        return  List.of(Objects.requireNonNull(new RestTemplate().postForObject(RETURN_STRATEGY_URI, balanceStatuses, DebtReturnTransaction[].class)));
    }
}
