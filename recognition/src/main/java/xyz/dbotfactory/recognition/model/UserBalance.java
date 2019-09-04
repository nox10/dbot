package xyz.dbotfactory.recognition.model;

import lombok.*;


@Setter
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserBalance {

    private int id;

    private TelegramUser user;

    private int balance;
}
