package xyz.dbotfactory.dbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
