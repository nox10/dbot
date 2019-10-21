package xyz.dbotfactory.dbot.service;

import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.model.BalanceStatus;
import xyz.dbotfactory.dbot.model.DebtReturnTransaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
public class CalcServiceImpl implements CalcService {
    @Override
    public List<BalanceStatus> getTotalBalance(List<BalanceStatus> balanceStatuses) {
        return aggregateBalanceChanges(balanceStatuses);
    }

    @Override
    public List<DebtReturnTransaction> getReturnStrategy(List<BalanceStatus> request) {
        List<BalanceStatus> aggregatedBalanceChanges = aggregateBalanceChanges(request);

        List<BalanceStatus> positiveBalances = aggregatedBalanceChanges
                .stream()
                .filter(x -> x.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(toList());
        List<BalanceStatus> negativeBalances = aggregatedBalanceChanges
                .stream()
                .filter(x -> x.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .collect(toList());

        sortPositive(positiveBalances);
        sortNegative(negativeBalances);

        return getDebtReturnTransactions(positiveBalances, negativeBalances);
    }

    private List<BalanceStatus> aggregateBalanceChanges(List<BalanceStatus> balanceChanges) {
        Map<Long, BigDecimal> totalBalances = balanceChanges
                .stream()
                .collect(
                        toMap(
                                BalanceStatus::getId,
                                BalanceStatus::getAmount,
                                BigDecimal::add
                        )
                );

        return totalBalances
                .entrySet()
                .stream()
                .map(x -> BalanceStatus
                        .builder()
                        .amount(x.getValue())
                        .id(x.getKey())
                        .build())
                .collect(toList());
    }

    private List<DebtReturnTransaction> getDebtReturnTransactions(List<BalanceStatus> positiveBalances, List<BalanceStatus> negativeBalances) {
        List<DebtReturnTransaction> resultList = new ArrayList<>();

        for (BalanceStatus posB : positiveBalances) {
            sortNegative(negativeBalances);
            for (BalanceStatus negB : negativeBalances) {

                if (negB.getAmount().equals(BigDecimal.ZERO))
                    continue;
                if (negB.getAmount().abs().compareTo(posB.getAmount()) > 0) {
                    DebtReturnTransaction transaction = DebtReturnTransaction
                            .builder()
                            .fromId(negB.getId())
                            .toId(posB.getId())
                            .amount(posB.getAmount())
                            .build();

                    resultList.add(transaction);

                    negB.addToAmount(posB.getAmount());
                    posB.setAmount(BigDecimal.ZERO);
                    break;
                } else {

                    DebtReturnTransaction transaction = DebtReturnTransaction
                            .builder()
                            .fromId(negB.getId())
                            .toId(posB.getId())
                            .amount(negB.getAmount().abs())
                            .build();

                    resultList.add(transaction);

                    posB.addToAmount(negB.getAmount());
                    negB.setAmount(BigDecimal.ZERO);
                }
            }
        }
        return resultList;
    }

    private void sortPositive(List<BalanceStatus> positiveBalances) {
        positiveBalances.sort(Comparator.comparing(BalanceStatus::getAmount));
    }

    private void sortNegative(List<BalanceStatus> negativeBalances) {
        negativeBalances.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
    }
}
