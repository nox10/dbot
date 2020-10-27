package xyz.dbotfactory.dbot.handler;

public interface CommonConsts {
    String DISCARD_ACTIVE_RECEIPT_CALLBACK_DATA = "discard_active_receipt";
    String ITEM_BUTTON_CALLBACK_DATA_PREFIX = "item";
    String DELIMITER = "/";
    String CONTINUE_DELIMITER = "_";
    String CONTINUE_COMMAND_METADATA_PREFIX = "continue";
    String CHECK_STATUS_CALLBACK_DATA = "check_status";
    String SHARE_LEFT_BUTTON_CALLBACK_DATA = "left";
    String SHARE_BUTTON_CALLBACK_DATA = "shr";
    String CUSTOM_SHARE_CALLBACK_DATA = "customshare";
    String SETING_CUSTOM_SHARE_METADATA = "settingcustshr";
    String DONE_MESSAGE_TEXT = "💸 Now please send me how much you have paid already, right in this chat.\n\n" +
            "ℹ️ Just to remind you, the total sum is: ";
    String GO_TO_GROUP_TEXT = "🔙 Please return to the group chat";
    String ITEMS_MESSAGE_TEXT = "🧾 Tap to items which are yours";
    String YOUR_RECEIPT_TEXT = "<b>🧾 Your receipt:\n</b>";
    String DONE_TEXT = "ℹ️ Feel free to send more items or press button below to continue";
    String SQUARED_DONE_EMOJI = "ℹ️";
    String CHECK_STATUS_BUTTON_TEXT = SQUARED_DONE_EMOJI + " Check the status " + SQUARED_DONE_EMOJI;
    String SHARES_DONE_TASK_NAME = "shares_done_task";
    String RECEIPT_BALANCES_BUILT = "rceipt_balances_built";
    String RECEIPT_LINE = "=====================";
    String RECEIPT_LINE_SMALL = "---------------------";
}
