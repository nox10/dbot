package xyz.dbotfactory.calculator.model;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebtReturnTransaction {
    long fromId;
    long toId;
    BigDecimal amount;
}
