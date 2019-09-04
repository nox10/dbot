package xyz.dbotfactory.recognition.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {
    private int id;

    private List<ReceiptItem> items = new ArrayList<>();

    private List<UserBalance> userBalances = new ArrayList<>();

    boolean isActive;
}