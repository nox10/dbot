package xyz.dbotfactory.dbot.handler;

import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.model.Chat;

public interface UpdateHandler {

    boolean canHandle(Update update, Chat chat);

    void handle(Update update, Chat chat);
}
