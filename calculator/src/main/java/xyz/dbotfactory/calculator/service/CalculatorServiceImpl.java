package xyz.dbotfactory.calculator.service;

import org.springframework.stereotype.Service;
import xyz.dbotfactory.calculator.model.BalanceChange;
import xyz.dbotfactory.calculator.model.DebtReturnTransaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
public class CalculatorServiceImpl implements CalculatorService {

    @Override
    public List<BalanceChange> calculateTotalBalance(List<BalanceChange> request) {

        return aggregateBalanceChanges(request);
    }

    private List<BalanceChange> aggregateBalanceChanges(List<BalanceChange> balanceChanges) {
        Map<Long, BigDecimal> totalBalances = balanceChanges
                .stream()
                .collect(
                        toMap(
                                BalanceChange::getId,
                                BalanceChange::getAmount,
                                BigDecimal::add
                        )
                );

        return totalBalances
                .entrySet()
                .stream()
                .map(x -> BalanceChange
                        .builder()
                        .amount(x.getValue())
                        .id(x.getKey())
                        .build())
                .collect(toList());
    }

    public List<DebtReturnTransaction> getDebtReturnStrategy(List<BalanceChange> request) {
        List<BalanceChange> aggregatedBalanceChanges = aggregateBalanceChanges(request);

        List<BalanceChange> positiveBalances = aggregatedBalanceChanges
                .stream()
                .filter(x -> x.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(toList());
        List<BalanceChange> negativeBalances = aggregatedBalanceChanges
                .stream()
                .filter(x -> x.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .collect(toList());

        sortPositive(positiveBalances);
        sortNegative(negativeBalances);

        return getDebtReturnTransactions(positiveBalances, negativeBalances);
    }

    private List<DebtReturnTransaction> getDebtReturnTransactions(List<BalanceChange> positiveBalances, List<BalanceChange> negativeBalances) {
        List<DebtReturnTransaction> resultList = new ArrayList<>();

        for (BalanceChange posB : positiveBalances) {
            sortNegative(negativeBalances);
            for (BalanceChange negB : negativeBalances) {

                if (negB.getAmount().equals(BigDecimal.ZERO) )
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

    private void sortPositive(List<BalanceChange> positiveBalances) {
        positiveBalances.sort(Comparator.comparing(BalanceChange::getAmount));
    }

    private void sortNegative(List<BalanceChange> negativeBalances) {
        negativeBalances.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
    }
}
