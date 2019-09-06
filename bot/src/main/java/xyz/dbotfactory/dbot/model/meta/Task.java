package xyz.dbotfactory.dbot.model.meta;

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
public class Task {
    @Id
    @GeneratedValue
    private int id;

    private Long chatId;
    private Integer messageId;
    private Boolean isPrivate;
}
