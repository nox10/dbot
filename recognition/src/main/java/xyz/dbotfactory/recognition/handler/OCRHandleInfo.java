package xyz.dbotfactory.recognition.handler;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;
import xyz.dbotfactory.recognition.model.Receipt;


@Builder
@Data
public class OCRHandleInfo {
    boolean handled;
    public Receipt handleInfo;
}
