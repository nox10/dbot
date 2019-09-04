package xyz.dbotfactory.recognition.model;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramUser {

    private int id;

    private int telegramId;

    private String firstName;

    private String lastName;
}
