package xyz.dbotfactory.dbot;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.UpdateHandlerAggregator;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.service.ChatService;

import javax.annotation.PostConstruct;

@Configuration
@EnableConfigurationProperties
@Log
public class Config {

    @PostConstruct
    public void init() {
        ApiContextInitializer.init();
    }

    @Bean
    public TelegramBotsApi telegramBotsApi() {
        return new TelegramBotsApi();
    }

    @Bean
    @SneakyThrows
    public TelegramLongPollingBot telegramLongPollingBot(ChatService chatService,
                                                         UpdateHandlerAggregator updateHandlerAggregator,
                                                         TelegramBotsApi telegramBotsApi,
                                                         BotProperties botProperties) {
        TelegramLongPollingBot bot = new TelegramLongPollingBot() {
            @Override
            public String getBotToken() {
                return botProperties.getToken();
            }

            @Override
            public String getBotUsername() {
                return botProperties.getUsername();
            }

            @Override
            @SneakyThrows
            public void onUpdateReceived(Update update) {
                try {
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
                } catch (DBotUserException userEx) {
                    log.warning("Error: " + userEx.getMessage());
                }
            }
        };

        telegramBotsApi.registerBot(bot);

        return bot;
    }
}
