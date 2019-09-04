package xyz.dbotfactory.recognition.model;


import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"id", "telegramUsers"})
public class ReceiptItem {

    private int id;

    private double price;

    private String name;

    private long amount;

    private List<TelegramUser> telegramUsers = new ArrayList<>();
}

