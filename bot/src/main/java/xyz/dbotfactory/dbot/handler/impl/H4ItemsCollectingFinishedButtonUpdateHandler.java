package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
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
import xyz.dbotfactory.dbot.handler.impl.callback.ShareEqualCallbackInfo;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.telegram.telegrambots.meta.api.methods.ParseMode.HTML;
import static xyz.dbotfactory.dbot.model.ChatState.DETECTING_OWNERS;

@Component
@Log
public class H4ItemsCollectingFinishedButtonUpdateHandler implements UpdateHandler, CommonConsts {

    private static final String MESSAGE_TEXT = "<i>Press button below to continue</i>";
    private static final String CONTINUE_BUTTON_TEXT = "Continue";

    private final ChatService chatService;
    private final TelegramLongPollingBot bot;

    @Autowired
    public H4ItemsCollectingFinishedButtonUpdateHandler(ChatService chatService, TelegramLongPollingBot bot) {
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

        InlineKeyboardButton continueButton =
                new InlineKeyboardButton().setUrl(
                        "https://telegram.me/" + bot.getBotUsername() + "?start=" + CONTINUE_COMMAND_METADATA_PREFIX +
                                chat.getTelegramChatId() + CONTINUE_DELIMITER + receipt.getId())
                        .setText(CONTINUE_BUTTON_TEXT);


        ShareEqualCallbackInfo shareEqualCallbackInfo = new ShareEqualCallbackInfo(chat.getTelegramChatId());
        InlineKeyboardButton shareEqualButton = shareEqualCallbackInfo.getButton();

        InlineKeyboardMarkup itemButtonsMarkup = new InlineKeyboardMarkup()
                .setKeyboard(singletonList(List.of(continueButton, shareEqualButton)));

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
