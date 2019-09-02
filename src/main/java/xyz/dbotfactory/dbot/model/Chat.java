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
public class Chat {

    @Id
    @GeneratedValue
    int id;

    @OneToMany(cascade = CascadeType.ALL)
    List<Receipt> receipts;
}
