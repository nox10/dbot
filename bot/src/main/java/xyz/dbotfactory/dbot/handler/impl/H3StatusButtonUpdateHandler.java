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
import xyz.dbotfactory.dbot.helper.PrettyPrintHelper;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.ReceiptItem;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.List;

import static java.util.Collections.singletonList;

@Component
public class H3StatusButtonUpdateHandler implements UpdateHandler, CommonConsts {

    private static final String SQUARED_DONE_EMOJI = "☑️";
    private static final String COLLECTING_FINISHED_BUTTON_TEXT =
            SQUARED_DONE_EMOJI + " That's it, done " + SQUARED_DONE_EMOJI;

    private final ChatService chatService;
    private final TelegramLongPollingBot bot;

    @Autowired
    public H3StatusButtonUpdateHandler(ChatService chatService, TelegramLongPollingBot bot) {
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

        List<ReceiptItem> items = activeReceipt.getItems();
        int maxNameLength = items.stream().mapToInt(x -> x.getName().length()).max().orElse(0);
        int maxPriceLength = items.stream().mapToInt(x -> String.valueOf(x.getPrice()).length()).max().orElse(0);
        for (ReceiptItem item : items) {

            String name = PrettyPrintHelper.padRight(item.getName(), maxNameLength);
            String price = PrettyPrintHelper.padRight(String.valueOf(item.getPrice()), maxPriceLength);
            stringBuilder
                    .append(name).append(" : ")
                    .append(price).append(" x ")
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
