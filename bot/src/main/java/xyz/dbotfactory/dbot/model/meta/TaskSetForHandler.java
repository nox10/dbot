package xyz.dbotfactory.dbot.model.meta;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskSetForHandler {
    @Id
    @GeneratedValue
    private int id;

    private String eventOrHandlerName;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Task> tasks;
}
