package xyz.dbotfactory.dbot.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BalanceStatus {
    private long id;
    private BigDecimal amount;

    public void addToAmount(BigDecimal amount) {
        this.setAmount(this.getAmount().add(amount));
    }
}
