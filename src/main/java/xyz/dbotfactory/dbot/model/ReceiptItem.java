package xyz.dbotfactory.dbot.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Setter
@Getter
@NoArgsConstructor
@Builder
public class ReceiptItem {

    @Id
    @GeneratedValue
    private int id;

    private int price;

    private String name;

    @OneToOne(cascade = CascadeType.ALL)
    private TelegramUser telegramUser;
}
