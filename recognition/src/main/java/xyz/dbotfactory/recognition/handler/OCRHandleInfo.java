package xyz.dbotfactory.recognition.handler;

import lombok.Builder;
import lombok.Data;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import xyz.dbotfactory.recognition.model.Receipt;

@Component
@Builder
@Data
public class OCRHandleInfo {
    boolean handled;
    public Receipt handleInfo;
}
