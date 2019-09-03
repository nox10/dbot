package xyz.dbotfactory.calculator.model;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DebtReturnTransaction {
    int fromId;
    int toId;
    double amount;
}
