package xyz.dbotfactory.dbot.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
@Setter
@Getter
@NoArgsConstructor
@Builder
public class UserBalance {

    @Id
    @GeneratedValue
    private int id;

    @OneToOne
    private TelegramUser user;

    private int balance;
}
