package xyz.dbotfactory.recognition.model;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    @GeneratedValue
    private int id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ReceiptItem> items = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL)
    private List<UserBalance> userBalances = new ArrayList<>();

    boolean isActive;
}