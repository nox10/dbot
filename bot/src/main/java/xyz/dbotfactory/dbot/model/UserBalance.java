package xyz.dbotfactory.dbot.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigDecimal;

@Entity
@Setter
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserBalance {

    @Id
    @GeneratedValue
    private int id;

    private long telegramUserId;
    private BigDecimal balance;
}
