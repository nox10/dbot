package xyz.dbotfactory.calculator.model;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceChange {
    long id;
    BigDecimal amount;

    public void addToAmount(BigDecimal amount) {
        this.setAmount(this.getAmount().add(amount));
    }
}
