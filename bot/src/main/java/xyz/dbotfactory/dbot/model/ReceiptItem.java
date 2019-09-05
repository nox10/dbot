package xyz.dbotfactory.dbot.model;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
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

    private BigDecimal price;

    private String name;

    private BigDecimal amount;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Share> shares = new ArrayList<>();

    public void addAmount(BigDecimal amount) {
        setAmount(getAmount().add(amount));
    }
}
