package xyz.dbotfactory.dbot.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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
}
