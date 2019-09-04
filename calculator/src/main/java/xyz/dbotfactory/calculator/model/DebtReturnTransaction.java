package xyz.dbotfactory.calculator.model;

import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebtReturnTransaction {
    int fromId;
    int toId;
    double amount;
}
