package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.DBotUserException;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.ButtonFactory;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.handler.impl.callback.ShareEqualCallbackInfo;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.ReceiptItem;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.telegram.telegrambots.meta.api.methods.ParseMode.HTML;
import static xyz.dbotfactory.dbot.BigDecimalUtils.create;

@Component
@Log
public class H2AddReceiptItemMessageUpdateHandler implements UpdateHandler, CommonConsts {
    //    private static final String CONTINUE_BUTTON_TEXT = "📊️ Share not equally 📊️";  // TODO: Get rid of "equally" or fix it
    private static final String CONTINUE_BUTTON_TEXT = "➡️ Continue ➡️";

    private static final String ITEM_REGEX = "\\d+([\\. ,]\\d+)?\\ \\d+([\\. ,]\\d+)?\\ .+$";
    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final TelegramLongPollingBot bot;
    private final BotMessageHelper botMessageHelper;
    private final ButtonFactory buttonFactory;

    @Autowired
    public H2AddReceiptItemMessageUpdateHandler(ChatService chatService, ReceiptService receiptService,
                                                TelegramLongPollingBot bot, BotMessageHelper botMessageHelper,
                                                ButtonFactory buttonFactory) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.bot = bot;
        this.botMessageHelper = botMessageHelper;
        this.buttonFactory = buttonFactory;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return update.hasMessage() &&
                !update.getMessage().isCommand() && update.getMessage().hasText() &&
                chat.getChatState() == ChatState.COLLECTING_ITEMS
                && update.getMessage().getText().matches(ITEM_REGEX);
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        String text = update.getMessage().getText().replace(',', '.');

        String[] itemInfo = parseItem(text);
        String amountStr = itemInfo[0];
        String priceForUnit = itemInfo[1];
        String name = itemInfo[2];

        Receipt receipt = chatService.getActiveReceipt(chat);

        BigDecimal price = create(priceForUnit);
        BigDecimal amount = create(amountStr);

        if (amount.signum() <= 0 || price.signum() < 0)
            return;

        ReceiptItem receiptItem = ReceiptItem.builder()
                .price(price)
                .name(name)
                .shares(new ArrayList<>())
                .amount(amount)
                .build();

        List<ReceiptItem> items = receipt.getItems();

        addOrUpdateExistingItem(items, receiptItem);

        String formattedReceipt = receiptService.buildBeautifulReceiptString(receipt);

        InlineKeyboardButton continueButton =
                InlineKeyboardButton.builder().url(
                                "https://telegram.me/" + bot.getBotUsername() + "?start=" + CONTINUE_COMMAND_METADATA_PREFIX +
                                        chat.getTelegramChatId() + CONTINUE_DELIMITER + receipt.getId())
                        .text(CONTINUE_BUTTON_TEXT)
                        .build();

        ShareEqualCallbackInfo shareEqualCallbackInfo = new ShareEqualCallbackInfo(chat.getTelegramChatId());
        InlineKeyboardButton shareEqualButton = shareEqualCallbackInfo.getButton();
        InlineKeyboardMarkup itemButtonsMarkup = InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(continueButton)
//                        , List.of(shareEqualButton)  // TODO: Fix unintended taps or complete getting rid of this code
                )).build();

        SendMessage message = SendMessage.builder()
                .chatId(Long.toString(chat.getTelegramChatId()))
                .replyMarkup(itemButtonsMarkup)
                .parseMode(HTML)
                .text(YOUR_RECEIPT_TEXT + formattedReceipt + "\n" + DONE_TEXT)
                .build();

        Message sentMessage = bot.execute(message);

        log.info("item(s) added to receipt" + receipt.getId() + " . Current items:  " + receipt.getItems());

        botMessageHelper.deleteMessage(bot, update.getMessage());
        botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(), chat.getChatMetaInfo(), bot,
                update.getMessage().getFrom().getId());
        addCleanupTasks(chat, sentMessage);

        chatService.save(chat);
    }

    private void addOrUpdateExistingItem(List<ReceiptItem> items, ReceiptItem newItem) {
        Optional<ReceiptItem> existingItem = items
                .stream()
                .filter(item -> item.getName().equals(newItem.getName()) && item.getPrice().compareTo(newItem.getPrice()) == 0)
                .findFirst();
        existingItem.ifPresentOrElse(item -> item.addAmount(newItem.getAmount()), () -> items.add(newItem));
    }

    private String[] parseItem(String item) {
        String[] split = item.split(" ");
        if (split.length < 3)
            throw new DBotUserException("incorrect receipt item format");
        String amount = split[0];
        String priceForUnit = split[1];
        String name = item.substring(amount.length() + priceForUnit.length() + 2).toUpperCase();

        return new String[]{amount, priceForUnit, name};
    }

    private void addCleanupTasks(Chat chat, Message sentMessage) {
        botMessageHelper.addNewTask(this.getClass().getSimpleName(), chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(SHARES_DONE_TASK_NAME, chat.getChatMetaInfo(), sentMessage);  // TODO: ?
        botMessageHelper.addNewTask(ShareEqualButtonUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);  // TODO: ?
    }
}
