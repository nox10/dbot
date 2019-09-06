package xyz.dbotfactory.dbot.handler;

public interface CommonConsts {
    String DISCARD_ACTIVE_RECEIPT_CALLBACK_DATA = "discard_active_receipt";
    String COLLECTING_FINISHED_CALLBACK_DATA = "collecting_is_finished";
    String ITEM_BUTTON_CALLBACK_DATA_PREFIX = "item";
    String DELIMITER = "/";
    String CONTINUE_DELIMITER = "_";
    String CONTINUE_COMMAND_METADATA_PREFIX = "continue";
    String FINISHED_SETTING_SHARES_CALLBACK_DATA = "shares_done";
    String SHARE_LEFT_BUTTON_CALLBACK_DATA = "left";
    String SHARE_BUTTON_CALLBACK_DATA = "shr";
    String CUSTOM_SHARE_CALLBACK_DATA = "customshare";
    String SETING_CUSTOM_SHARE_METADATA = "settingcustshr";
    String DONE_MESSAGE_TEXT = "💸 Now please send me how much you have paid already, right in this chat.\n\n" +
            "ℹ️ Total sum is: ";
    String GO_TO_GROUP_TEXT = "🔙 Please, return to group chat";
    String ITEMS_MESSAGE_TEXT = "🧾 Tap to items which are yours";
    String YOUR_RECEIPT_TEXT = "<b>🧾 Your receipt:\n</b>";
    String DONE_TEXT = "ℹ️ Feel free to send more items";
    String SQUARED_DONE_EMOJI = "☑️";
    String FINISHED_SETTING_SHARES_BUTTON_TEXT = SQUARED_DONE_EMOJI + " I'm done " + SQUARED_DONE_EMOJI;
    String COLLECTING_FINISHED_BUTTON_TEXT =
            SQUARED_DONE_EMOJI + " No more items " + SQUARED_DONE_EMOJI;
    String SHARES_DONE_TASK_NAME = "shares_done_task";
    String RECEIPT_BALANCES_BUILT = "rceipt_balances_built";
}
