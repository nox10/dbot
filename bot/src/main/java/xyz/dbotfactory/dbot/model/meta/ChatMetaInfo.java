package xyz.dbotfactory.dbot.model.meta;

import lombok.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMetaInfo {
    @Id
    @GeneratedValue
    private int id;

    private String pmUserIds;
    private String metaData;

    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<TaskSetForHandler> tasks;
}
