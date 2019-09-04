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
public class ReceiptMetaInfo {
    @Id
    @GeneratedValue
    private int id;

    private String testMetaData;
}
