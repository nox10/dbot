package xyz.dbotfactory.dbot.handler;

public interface CommonConsts {
    String DISCARD_ACTIVE_RECEIPT_CALLBACK_DATA = "discard_active_receipt";
    String COLLECTING_FINISHED_CALLBACK_DATA = "collecting_is_finished";
    String SUGGEST_DEBT_RETURN_STATEGY = "suggest_debt_return_strategy";
    String ITEM_BUTTON_CALLBACK_DATA_PREFIX = "item";
    String DELIMITER = "/";
    String CONTINUE_DELIMITER = "_";
    String COLLECTING_STATUS_CALLBACK_DATA = "collecting_status";
    String CONTINUE_COMMAND_METADATA_PREFIX = "continue";
    String FINISHED_SETTING_SHARES_CALLBACK_DATA = "shares_done";
    String SHARE_LEFT_BUTTON_CALLBACK_DATA = "left";
    String SHARE_BUTTON_CALLBACK_DATA = "shr";
    String CUSTOM_SHARE_CALLBACK_DATA = "customshare";
    String FINISHED_SETTING_SHARES_BUTTON_TEXT = "Finished";
    String SUGGEST_DEBT_RETURN_STATEGY_MESSAGE = "How to pay off?";
    String SETING_CUSTOM_SHARE_METADATA = "settingcustshr";
    String DONE_MESSAGE_TEXT = "<i>Now each of you can send me how much you have already paid, right in this chat.\n\n" +
            "Total sum is: </i>";
    String GO_TO_GROUP_TEXT = "<i>Go back to group chat</i>";
    String ITEMS_MESSAGE_TEXT = "<i>Tap to items which are yours</i>";
}
