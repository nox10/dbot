package xyz.dbotfactory.dbot.handler;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.ArrayList;

import static xyz.dbotfactory.dbot.model.ChatState.COLLECTING_ITEMS;

@Component
@Log
public class NewReceiptUpdateHandler implements UpdateHandler {

    private static final String COMMAND_NAME = "/new_receipt";

    private final ChatService chatService;

    @Autowired
    public NewReceiptUpdateHandler(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return update.getMessage().isCommand() &&
                update.getMessage().getText().equals(COMMAND_NAME) &&
                chat.getChatState() == ChatState.NO_ACTIVE_RECEIPT;
    }

    @Override
    public void handle(Update update, Chat chat) {
        Receipt receipt = Receipt.builder()
                .items(new ArrayList<>())
                .userBalances(new ArrayList<>())
                .isActive(true)
                .build();
        chat.getReceipts().add(receipt);
        chat.setChatState(COLLECTING_ITEMS);
        chatService.save(chat);
        log.info("Chat " + chat.getId() + " is now in " + chat.getChatState() + " state");
    }
}
