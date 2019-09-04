package xyz.dbotfactory.dbot.model;

import lombok.*;

import javax.persistence.*;

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

    private double balance;
}
