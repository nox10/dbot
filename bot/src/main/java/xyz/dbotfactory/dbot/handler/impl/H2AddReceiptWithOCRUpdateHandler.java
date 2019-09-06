package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.OCRResult;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;

@Service
@Log
public class H2AddReceiptWithOCRUpdateHandler implements UpdateHandler, CommonConsts {
    private final ChatService chatService;
    private final TelegramLongPollingBot bot;
    private final ReceiptService receiptService;
    private final BotMessageHelper botMessageHelper;

    private static final String CANT_PARSE_TEXT = "Sorry, we can't parse your receipt, do it yourself.";
    private static final String UNDO_RECEIPT = "Start again";

    @Autowired
    public H2AddReceiptWithOCRUpdateHandler(ChatService chatService, TelegramLongPollingBot bot,
                                            ReceiptService receiptService, BotMessageHelper botMessageHelper) {
        this.bot = bot;
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.botMessageHelper = botMessageHelper;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return update.hasMessage() &&
                !update.getMessage().isCommand() && update.getMessage().hasPhoto() &&
                chat.getChatState() == ChatState.COLLECTING_ITEMS;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        Receipt receipt = chatService.getActiveReceipt(chat);
        List<PhotoSize> photoSizes = update.getMessage().getPhoto();
        String fileId = photoSizes.get(photoSizes.size() - 1).getFileId();
        String getPathQuery = "https://api.telegram.org/bot" + this.bot.getBotToken() + "/getFile?file_id=" + fileId;

        JSONObject jsonObject = this.sendGet(getPathQuery);
        if ((Boolean) jsonObject.get("ok") == true) {
            JSONObject result = (JSONObject) jsonObject.get("result");
            String filePath = (String) result.get("file_path");
            String imageUrl = "https://api.telegram.org/file/bot" + this.bot.getBotToken() + "/" + filePath;

            OCRResult ocrResult = this.receiptService.parseReceipt(imageUrl);
            if (ocrResult.isHandled()) {
                receipt.setItems(ocrResult.getReceipt().getItems());

                String formattedReceipt = this.receiptService.buildBeautifulReceiptString(ocrResult.receipt);
                InlineKeyboardButton collectingFinishedButton = new InlineKeyboardButton()
                        .setText(COLLECTING_FINISHED_BUTTON_TEXT)
                        .setCallbackData(COLLECTING_FINISHED_CALLBACK_DATA);
                InlineKeyboardButton undoReceiptButton = new InlineKeyboardButton()
                        .setText(UNDO_RECEIPT)
                        .setCallbackData(DISCARD_ACTIVE_RECEIPT_CALLBACK_DATA + DELIMITER + chat.getTelegramChatId());
                InlineKeyboardMarkup collectingFinishedMarkup = new InlineKeyboardMarkup()
                        .setKeyboard(Arrays.asList(singletonList(collectingFinishedButton), singletonList(undoReceiptButton)));
                SendMessage message = new SendMessage()
                        .setChatId(update.getMessage().getChatId())
                        .setText(YOUR_RECEIPT_TEXT + formattedReceipt + "\n" + DONE_TEXT)
                        .setReplyMarkup(collectingFinishedMarkup)
                        .setParseMode(ParseMode.HTML);

                Message sentMessage = bot.execute(message);
                log.info("item(s) added to receipt" + receipt.getId() + " . Current items:  " + receipt.getItems());

                botMessageHelper.deleteMessage(bot, update.getMessage());
                botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(), chat.getChatMetaInfo(), bot,
                        update.getMessage().getFrom().getId());
                botMessageHelper.addNewTask(this.getClass().getSimpleName(), chat.getChatMetaInfo(), sentMessage);
                botMessageHelper.addNewTask(H2AddReceiptItemMessageUpdateHandler.class.getSimpleName(),
                        chat.getChatMetaInfo(), sentMessage);
                botMessageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                        chat.getChatMetaInfo(), sentMessage);
                botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                        chat.getChatMetaInfo(), sentMessage);

                chatService.save(chat);
                return;
            }
        }
        SendMessage message = new SendMessage()
                .setChatId(update.getMessage().getChatId())
                .setText(CANT_PARSE_TEXT)
                .setParseMode(ParseMode.HTML);

        Message sentMessage = bot.execute(message);
        botMessageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(H2AddReceiptItemMessageUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);

        botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(), chat.getChatMetaInfo(), bot,
                update.getMessage().getFrom().getId());

        chatService.save(chat);
    }

    @SneakyThrows
    private JSONObject sendGet(String urlString) {
        URL obj = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        int responseCode = con.getResponseCode();
        JSONObject o = null;
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String line;
            StringBuffer response = new StringBuffer();
            JSONParser j = new JSONParser();
            while ((line = in.readLine()) != null) {
                o = (JSONObject) j.parse(line);
            }
            in.close();
        }
        return o;
    }
}
