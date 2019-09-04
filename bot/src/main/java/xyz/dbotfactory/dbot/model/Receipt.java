package xyz.dbotfactory.dbot.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

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

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue
    private int id;

    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<ReceiptItem> items = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<UserBalance> userBalances = new ArrayList<>();

    boolean isActive;

    private String receiptMetaInfoJson;

    @SneakyThrows
    public String getReceiptMetaInfoJson(){
        return objectMapper.writeValueAsString(getReceiptMetaInfo());
    }

    @Transient
    @Setter(AccessLevel.NONE)
    private ReceiptMetaInfo receiptMetaInfo;

    @SneakyThrows
    public ReceiptMetaInfo getReceiptMetaInfo(){
        if (receiptMetaInfo == null)
            receiptMetaInfo = objectMapper.readValue(receiptMetaInfoJson, ReceiptMetaInfo.class);
        return receiptMetaInfo;
    }
}
