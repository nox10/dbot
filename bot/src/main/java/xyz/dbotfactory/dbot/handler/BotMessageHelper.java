package xyz.dbotfactory.dbot.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import xyz.dbotfactory.dbot.model.CleanupTaskInfo;
import xyz.dbotfactory.dbot.model.meta.ChatMetaInfo;
import xyz.dbotfactory.dbot.model.meta.Task;
import xyz.dbotfactory.dbot.model.meta.TaskSetForHandler;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Component
public class BotMessageHelper {

    @Autowired
    private ObjectMapper objectMapper;

    @SneakyThrows
    public Message sendSimpleMessage(String message, Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage(chatId, message).setParseMode(ParseMode.HTML);
        return bot.execute(sendMessage);
    }

    @SneakyThrows
    public Message sendMessageWithSingleInlineMarkup(Long chatId,
                                                     InlineKeyboardMarkup markup,
                                                     TelegramLongPollingBot bot,
                                                     @Nullable String message) {

        SendMessage sendMessage = new SendMessage()
                .setChatId(chatId)
                .setText(message)
                .setReplyMarkup(markup)
                .setParseMode(ParseMode.HTML);

        return bot.execute(sendMessage);
    }

    @SneakyThrows
    public void notifyCallbackProcessed(String callbackId, TelegramLongPollingBot bot) {
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery()
                .setCallbackQueryId(callbackId);
        bot.execute(answerCallbackQuery);
    }

    @SneakyThrows
    public void deleteMessage(TelegramLongPollingBot bot, Message message) {
        try {
            Integer messageId = message.getMessageId();
            Long chatId = message.getChatId();
            deleteMessage(bot, chatId, messageId);
        } catch (Throwable e) {
        }
    }

    public void deleteMessage(TelegramLongPollingBot bot, long chatId, Integer messageId) {
        deleteMessage(bot, Long.toString(chatId), messageId);
    }

    @SneakyThrows
    public void deleteMessage(TelegramLongPollingBot bot, String chatId, Integer messageId) {
        try {
            bot.execute(new DeleteMessage()
                    .setMessageId(messageId)
                    .setChatId(chatId));
        } catch (Throwable throwable) {
        }
    }

    public void deleteButtons(TelegramLongPollingBot bot, long chatId, int messageId) {
        deleteButtons(bot, Long.toString(chatId), messageId);
    }

    @SneakyThrows
    public void deleteButtons(TelegramLongPollingBot bot, String chatId, Integer messageId) {
        try {
            bot.execute(new EditMessageReplyMarkup()
                    .setMessageId(messageId)
                    .setChatId(chatId)
                    .setReplyMarkup(null));
        } catch (Throwable throwable) {
        }
    }

    public void executeExistingTasks(String handlerName, ChatMetaInfo chatMetaInfo, TelegramLongPollingBot bot,
                                     Integer userId) {
        List<TaskSetForHandler> tasks = chatMetaInfo.getTasks();


        if (tasks.stream().anyMatch(taskSet -> taskSet.getEventOrHandlerName().equals(handlerName))) {
            List<Task> handlerTasks = tasks.stream()
                    .filter(taskSet -> taskSet.getEventOrHandlerName().equals(handlerName))
                    .findFirst().get().getTasks();
            Set<Task> tasksDone = new HashSet<>();
            for (Task handlerTask : handlerTasks) {
                if (!handlerTask.getIsPrivate() || userId.toString().equals(handlerTask.getChatId().toString())) {
                    deleteMessage(bot, handlerTask.getChatId(), handlerTask.getMessageId());
                    tasksDone.add(handlerTask);
                }
            }

            handlerTasks.removeAll(tasksDone);
        }
    }

    public void addNewTasks(List<CleanupTaskInfo> cleanupTaskInfo){
        for (CleanupTaskInfo taskInfo : cleanupTaskInfo)
            addNewTask(taskInfo.getHandlerName(), taskInfo.getChatMetaInfo(), taskInfo.getSentMessage());
    }

    public void addNewTask(String handlerName, ChatMetaInfo chatMetaInfo, Message sentMessage) {
        List<TaskSetForHandler> tasks = chatMetaInfo.getTasks();

        List<Task> handlerTasks;
        TaskSetForHandler taskSet = tasks.stream().filter(task1 -> task1.getEventOrHandlerName().equals(handlerName)).findFirst().orElse(
                TaskSetForHandler.builder().eventOrHandlerName(handlerName).build());
        handlerTasks = taskSet.getTasks();
        if (handlerTasks == null) {
            handlerTasks = new LinkedList<>();
            taskSet.setTasks(handlerTasks);
            tasks.add(taskSet);
        }

        boolean isPrivate = sentMessage.getChat().isUserChat();
        handlerTasks.add(Task.builder().chatId(sentMessage.getChatId()).messageId(sentMessage.getMessageId())
                .isPrivate(isPrivate).build());
    }
}
