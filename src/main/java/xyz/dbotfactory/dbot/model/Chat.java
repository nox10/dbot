package xyz.dbotfactory.dbot.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

import static javax.persistence.CascadeType.*;
import static javax.persistence.EnumType.ORDINAL;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Builder
public class Chat {

    @Id
    @GeneratedValue
    private int id;

    @OneToMany(cascade = ALL)
    private List<Receipt> receipts;

    @Enumerated(ORDINAL)
    ChatState chatState;
}
