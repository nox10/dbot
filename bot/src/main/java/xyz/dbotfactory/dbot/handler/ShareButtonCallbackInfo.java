package xyz.dbotfactory.dbot.handler;

import lombok.Builder;
import lombok.Data;

import static xyz.dbotfactory.dbot.handler.CommonConsts.DELIMITER;
import static xyz.dbotfactory.dbot.handler.CommonConsts.SHARE_BUTTON_CALLBACK_DATA;

@Data
@Builder
public class ShareButtonCallbackInfo {

    private String shareAmount;
    private int itemId;
    private int receiptId;
    private long tgGroupChatId;

    public static ShareButtonCallbackInfo parseCallbackString(String string){
        String[] dataArray = string.substring(SHARE_BUTTON_CALLBACK_DATA.length()).split(DELIMITER);
        String shareAmount = dataArray[0];
        int itemId = Integer.parseInt(dataArray[1]);
        int receiptId = Integer.parseInt(dataArray[2]);
        long tgGroupChatId = Integer.parseInt(dataArray[3]);
       return ShareButtonCallbackInfo
               .builder()
               .itemId(itemId)
               .receiptId(receiptId)
               .tgGroupChatId(tgGroupChatId)
               .shareAmount(shareAmount)
               .build();
    }

}
