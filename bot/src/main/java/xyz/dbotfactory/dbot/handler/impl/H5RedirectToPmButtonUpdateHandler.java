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
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
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
public class H5RedirectToPmButtonUpdateHandler implements UpdateHandler, CommonConsts {

    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final TelegramLongPollingBot bot;
    private final BotMessageHelper botMessageHelper;

    @Autowired
    public H5RedirectToPmButtonUpdateHandler(ChatService chatService, ReceiptService receiptService,
                                             TelegramLongPollingBot bot, BotMessageHelper botMessageHelper) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.bot = bot;
        this.botMessageHelper = botMessageHelper;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                String text = message.getText();
                if (text.startsWith("/start") && (!text.equals("/start")) && text.substring(1 + 5 + 1).startsWith(CONTINUE_COMMAND_METADATA_PREFIX)) {
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
        int telegramUserId = update.getMessage().getFrom().getId();
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
                .setText(ITEMS_MESSAGE_TEXT)
                .setChatId((long) telegramUserId)
                .setParseMode(ParseMode.HTML);

        String pmUserIds = groupChat.getChatMetaInfo().getPmUserIds();
        if (!pmUserIds.contains(Long.toString(telegramUserId))) {
            groupChat.getChatMetaInfo().setPmUserIds(pmUserIds + telegramUserId + DELIMITER);
        }

        Message sentMessage = bot.execute(message);

        botMessageHelper.deleteMessage(bot, update.getMessage());
        botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(), groupChat.getChatMetaInfo(), bot,
                telegramUserId);
        botMessageHelper.addNewTask(this.getClass().getSimpleName(), groupChat.getChatMetaInfo(), sentMessage);
        addCleanupTasks(groupChat, sentMessage);

        chatService.save(groupChat);
    }

    private void addCleanupTasks(Chat groupChat, Message sentMessage) {
        botMessageHelper.addNewTask(SHARES_DONE_TASK_NAME, groupChat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                groupChat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                groupChat.getChatMetaInfo(), sentMessage);
    }
}
