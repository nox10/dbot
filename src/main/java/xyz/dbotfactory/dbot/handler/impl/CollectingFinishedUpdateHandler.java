package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.telegram.telegrambots.meta.api.methods.ParseMode.HTML;
import static xyz.dbotfactory.dbot.model.ChatState.DETECTING_OWNERS;

@Component
@Log
public class CollectingFinishedUpdateHandler implements UpdateHandler, CommonConsts {

    private static final String FINISH_EMOJI = "🏁";
    private static final String FINISH_BUTTON_TEXT = FINISH_EMOJI + " Finish " + FINISH_EMOJI;
    private static final String MESSAGE_TEXT = "Press to items which are yours, " +
            "wait for others to do the same and then press " +
            "[" + FINISH_BUTTON_TEXT + "] button below at the end of items list.";

    private final ChatService chatService;
    private final TelegramLongPollingBot bot;

    public CollectingFinishedUpdateHandler(ChatService chatService, TelegramLongPollingBot bot) {
        this.chatService = chatService;
        this.bot = bot;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return update.hasCallbackQuery() &&
                update.getCallbackQuery().getData().equals(COLLECTING_FINISHED_CALLBACK_DATA) &&
                chat.getChatState() == ChatState.COLLECTING_ITEMS;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery()
                .setCallbackQueryId(callbackQuery.getId());

        chat.setChatState(DETECTING_OWNERS);

        Receipt receipt = chatService.getActiveReceipt(chat);
        List<List<InlineKeyboardButton>> itemButtons = receipt.getItems()
                .stream()
                .map(receiptItem -> singletonList(new InlineKeyboardButton()
                        .setText(receiptItem.getName())
                        .setCallbackData(ITEM_BUTTON_CALLBACK_DATA_PREFIX + DELIMITER + receiptItem.getId())))
                .collect(Collectors.toList());
        itemButtons.add(singletonList(new InlineKeyboardButton()
                .setCallbackData(ITEMS_ARE_CHOSEN_CALLBACK_DATA)
                .setText(FINISH_BUTTON_TEXT)));
        InlineKeyboardMarkup itemButtonsMarkup = new InlineKeyboardMarkup()
                .setKeyboard(itemButtons);

        SendMessage message = new SendMessage()
                .setChatId(chat.getTelegramChatId())
                .setReplyMarkup(itemButtonsMarkup)
                .setParseMode(HTML)
                .setText(MESSAGE_TEXT);

        bot.execute(message);
        bot.execute(answerCallbackQuery);
        chatService.save(chat);

        log.info("Chat " + chat.getId() + " is now in " + chat.getChatState() + " state");
    }
}
