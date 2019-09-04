package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Service
@Log
public class ContinueToDetectOwnersUpdateHandler implements UpdateHandler, CommonConsts {

    private final static String MESSAGE_TEXT = "<i>Tap to items which are yours</i>";

    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final TelegramLongPollingBot bot;

    @Autowired
    public ContinueToDetectOwnersUpdateHandler(ChatService chatService, ReceiptService receiptService,
                                               TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                String text = message.getText();
                if (text.startsWith("/start") && text.substring(1 + 5 + 1).startsWith(CONTINUE_COMMAND_METADATA_PREFIX)) {
                    String[] metadata = text.substring(1 + 5 + 1 + CONTINUE_COMMAND_METADATA_PREFIX.length())
                            .split(CONTINUE_DELIMITER);
                    long telegramGroupChatId = Long.parseLong(metadata[0]);
                    int receiptId = Integer.parseInt(metadata[1]);
                    Chat publicGroup = chatService.findOrCreateChatByTelegramId(telegramGroupChatId);

                    return chatService.getActiveReceipt(publicGroup).getId() == receiptId &&
                            publicGroup.getChatState() == ChatState.DETECTING_OWNERS;
                }
            }
        }

        return false;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        long telegramUserId = chat.getTelegramChatId();
        String[] metadata = update.getMessage().getText()
                .substring(1 + 5 + 1 + CONTINUE_COMMAND_METADATA_PREFIX.length())
                .split(CONTINUE_DELIMITER);
        long telegramGroupChatId = Long.parseLong(metadata[0]);
        int receiptId = Integer.parseInt(metadata[1]);

        Chat groupChat = chatService.findOrCreateChatByTelegramId(telegramGroupChatId);
        Receipt receipt = chatService.getActiveReceipt(groupChat);

        List<List<InlineKeyboardButton>> itemButtons = receipt.getItems().stream()
                .map(item -> singletonList(new InlineKeyboardButton()
                        .setText(item.getName() + receiptService.getShareStringForButton(item, telegramUserId))
                        .setCallbackData(ITEM_BUTTON_CALLBACK_DATA_PREFIX + item.getId() + DELIMITER +
                                receiptId + DELIMITER + telegramGroupChatId)
                )).collect(Collectors.toList());

        InlineKeyboardButton finishedButton = new InlineKeyboardButton()
                .setText(FINISHED_SETTING_SHARES_BUTTON_TEXT)
                .setCallbackData(FINISHED_SETTING_SHARES_CALLBACK_DATA + telegramGroupChatId + DELIMITER + receiptId);

        itemButtons.add(singletonList(finishedButton));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup()
                .setKeyboard(itemButtons);

        SendMessage message = new SendMessage()
                .setReplyMarkup(markup)
                .setText(MESSAGE_TEXT)
                .setChatId(telegramUserId)
                .setParseMode(ParseMode.HTML);

        bot.execute(message);
    }
}
