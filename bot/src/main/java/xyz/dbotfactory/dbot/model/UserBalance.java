package xyz.dbotfactory.dbot.model;

import lombok.*;

import javax.persistence.*;
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
