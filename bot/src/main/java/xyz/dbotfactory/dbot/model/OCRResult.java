package xyz.dbotfactory.dbot.model;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class OCRResult {
    boolean handled;
    public Receipt receipt;
}
