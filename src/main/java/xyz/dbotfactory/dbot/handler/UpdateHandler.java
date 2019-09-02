package xyz.dbotfactory.dbot.handler;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface UpdateHandler {

    boolean canHandle(Update update);
    void handle(Update update);

}
