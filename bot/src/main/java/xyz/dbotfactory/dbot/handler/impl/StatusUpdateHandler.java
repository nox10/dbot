package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.ReceiptItem;
import xyz.dbotfactory.dbot.service.ChatService;

import static java.util.Collections.singletonList;

@Component
public class StatusUpdateHandler implements UpdateHandler, CommonConsts {

    private static final String SQUARED_DONE_EMOJI = "☑️";
    private static final String COLLECTING_FINISHED_BUTTON_TEXT =
            SQUARED_DONE_EMOJI + " Finish " + SQUARED_DONE_EMOJI;

    private final ChatService chatService;
    private final TelegramLongPollingBot bot;

    @Autowired
    public StatusUpdateHandler(ChatService chatService, TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return chat.getChatState() == ChatState.COLLECTING_ITEMS &&
                update.hasCallbackQuery() &&
                update.getCallbackQuery().getData().equals(COLLECTING_STATUS_CALLBACK_DATA);
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        Receipt activeReceipt = chatService.getActiveReceipt(chat);
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("<pre>");
        for (ReceiptItem item : activeReceipt.getItems()) {
            stringBuilder
                    .append(item.getName()).append(" : ")
                    .append(item.getPrice()).append(" x ")
                    .append(item.getAmount()).append("\n");
        }
        stringBuilder.append("</pre>");
        String result = stringBuilder.toString();

        InlineKeyboardButton collectingFinishedButton = new InlineKeyboardButton()
                .setText(COLLECTING_FINISHED_BUTTON_TEXT)
                .setCallbackData(COLLECTING_FINISHED_CALLBACK_DATA);
        InlineKeyboardMarkup collectingFinishedMarkup = new InlineKeyboardMarkup()
                .setKeyboard(singletonList(singletonList(collectingFinishedButton)));

        SendMessage message = new SendMessage()
                .setChatId(update.getCallbackQuery().getMessage().getChatId())
                .setText(result)
                .setReplyMarkup(collectingFinishedMarkup)
                .setParseMode(ParseMode.HTML);

        bot.execute(message);
    }
}
