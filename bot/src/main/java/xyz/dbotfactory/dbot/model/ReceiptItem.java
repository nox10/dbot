package xyz.dbotfactory.dbot.model;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"id", "telegramUsers"})
public class ReceiptItem {

    @Id
    @GeneratedValue
    private int id;

    private double price;

    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<TelegramUser> telegramUsers = new ArrayList<>();
}
