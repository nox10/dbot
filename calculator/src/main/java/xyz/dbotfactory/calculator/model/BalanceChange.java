package xyz.dbotfactory.calculator.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceChange {
    int id;
    double amount;

    public void addToAmount(double amount) {
        this.setAmount(this.getAmount() + amount);
    }
}
