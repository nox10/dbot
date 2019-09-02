package xyz.dbotfactory.dbot;

import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.UpdateHandlerAggregator;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.service.ChatService;

@Component
@ConfigurationProperties("bot")
@Setter
public class TelegramBot extends TelegramLongPollingBot {
    private String token;
    private String username;

    private final UpdateHandlerAggregator updateHandlerAggregator;

    private final ChatService chatService;

    @Autowired
    public TelegramBot(UpdateHandlerAggregator updateHandlerAggregator, ChatService chatService, DefaultBotOptions defaultBotOptions) {
        super(defaultBotOptions);
        this.updateHandlerAggregator = updateHandlerAggregator;
        this.chatService = chatService;
    }

    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {

        long chatId = 0;
        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
        }

        if (chatId == 0) {
            throw new DBotUserException("Unsupported update type, no message and no callbackQuery");
        }

        Chat chat = chatService.findOrCreateChat(chatId);

        UpdateHandler commandHandler = updateHandlerAggregator.getUpdateHandler(update, chat);
        commandHandler.handle(update, chat);
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }
}
