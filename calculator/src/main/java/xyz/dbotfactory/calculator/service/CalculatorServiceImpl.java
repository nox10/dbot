package xyz.dbotfactory.calculator.service;

import org.springframework.stereotype.Service;
import xyz.dbotfactory.calculator.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.StrictMath.abs;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
public class CalculatorServiceImpl implements CalculatorService {

    @Override
    public List<BalanceChange> calculateTotalBalance(List<BalanceChange> request) {

        return aggregateBalanceChanges(request);
    }

    private List<BalanceChange> aggregateBalanceChanges(List<BalanceChange> balanceChanges) {
        Map<Long, Double> totalBalances = balanceChanges
                .stream()
                .collect(
                        toMap(
                                BalanceChange::getId,
                                BalanceChange::getAmount,
                                (a, b) -> a + b
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
                .filter(x -> x.getAmount() > 0)
                .collect(toList());
        List<BalanceChange> negativeBalances = aggregatedBalanceChanges
                .stream()
                .filter(x -> x.getAmount() < 0)
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

                if (negB.getAmount() == 0)
                    continue;
                if (abs(negB.getAmount()) >= posB.getAmount()) {
                    DebtReturnTransaction transaction = DebtReturnTransaction
                            .builder()
                            .fromId(negB.getId())
                            .toId(posB.getId())
                            .amount(posB.getAmount())
                            .build();

                    resultList.add(transaction);

                    negB.addToAmount(posB.getAmount());
                    posB.setAmount(0);
                    break;
                } else {

                    DebtReturnTransaction transaction = DebtReturnTransaction
                            .builder()
                            .fromId(negB.getId())
                            .toId(posB.getId())
                            .amount(abs(negB.getAmount()))
                            .build();

                    resultList.add(transaction);

                    posB.addToAmount(negB.getAmount());
                    negB.setAmount(0);
                }
            }
        }
        return resultList;
    }

    private void sortPositive(List<BalanceChange> positiveBalances) {
        positiveBalances.sort((a, b) -> (int) (a.getAmount() - b.getAmount()));
    }

    private void sortNegative(List<BalanceChange> negativeBalances) {
        negativeBalances.sort((a, b) -> (int) (b.getAmount() - a.getAmount()));
    }
}
