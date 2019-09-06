package xyz.dbotfactory.dbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.Message;
import xyz.dbotfactory.dbot.model.meta.ChatMetaInfo;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleanupTaskInfo {

    private String handlerName;
    private ChatMetaInfo chatMetaInfo;
    private Message sentMessage;
}
