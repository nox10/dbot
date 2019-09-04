package xyz.dbotfactory.calculator.model;

import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebtReturnTransaction {
    long fromId;
    long toId;
    double amount;
}
