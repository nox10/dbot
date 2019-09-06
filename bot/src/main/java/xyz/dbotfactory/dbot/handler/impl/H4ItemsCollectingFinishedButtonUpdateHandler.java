package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.impl.callback.ShareEqualCallbackInfo;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.List;

import static org.telegram.telegrambots.meta.api.methods.ParseMode.HTML;
import static xyz.dbotfactory.dbot.model.ChatState.DETECTING_OWNERS;

@Component
@Log
public class H4ItemsCollectingFinishedButtonUpdateHandler implements UpdateHandler, CommonConsts {

    private static final String MESSAGE_TEXT = "<i>Press button below to continue</i>";
    private static final String CONTINUE_BUTTON_TEXT = "📊️ Share not equally 📊️";

    private final ChatService chatService;
    private final TelegramLongPollingBot bot;
    private final BotMessageHelper botMessageHelper;

    @Autowired
    public H4ItemsCollectingFinishedButtonUpdateHandler(ChatService chatService, TelegramLongPollingBot bot,
                                                        BotMessageHelper botMessageHelper) {
        this.chatService = chatService;
        this.bot = bot;
        this.botMessageHelper = botMessageHelper;
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
                .setKeyboard(List.of(List.of(continueButton), List.of(shareEqualButton)));

        SendMessage message = new SendMessage()
                .setChatId(chat.getTelegramChatId())
                .setReplyMarkup(itemButtonsMarkup)
                .setParseMode(HTML)
                .setText(MESSAGE_TEXT);

        Message sentMessage = bot.execute(message);
        bot.execute(answerCallbackQuery);

        log.info("Chat " + chat.getId() + " is now in " + chat.getChatState() + " state");

        botMessageHelper.deleteButtons(bot, update.getCallbackQuery().getMessage().getChatId(),
                update.getCallbackQuery().getMessage().getMessageId());
        botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(), chat.getChatMetaInfo(), bot,
                update.getCallbackQuery().getFrom().getId());
        addCleanupTasks(chat, sentMessage);

        chatService.save(chat);
    }

    private void addCleanupTasks(Chat chat, Message sentMessage) {
        botMessageHelper.addNewTask(SHARES_DONE_TASK_NAME, chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(ShareEqualButtonUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
    }
}
