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
@ToString(exclude = {"id", "shares"})
@EqualsAndHashCode(exclude = {"id", "amount", "shares"})
public class ReceiptItem {

    @Id
    @GeneratedValue
    private int id;

    private double price;

    private String name;

    private int amount;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Share> shares = new ArrayList<>();
}
