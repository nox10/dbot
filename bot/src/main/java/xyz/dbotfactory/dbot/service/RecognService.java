package xyz.dbotfactory.dbot.service;

import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.model.OCRResult;

@Service
public interface RecognService {
    OCRResult parseReceipt(String imageUrl);
}
