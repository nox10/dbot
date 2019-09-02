package xyz.dbotfactory.dbot.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Builder
public class Receipt {

    @Id
    @GeneratedValue
    private int id;

    @OneToMany(cascade = CascadeType.ALL)
    private List<ReceiptItem> items;

    @OneToMany(cascade = CascadeType.ALL)
    private List<UserBalance> userBalances;
}
