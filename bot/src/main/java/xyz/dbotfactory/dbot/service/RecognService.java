package xyz.dbotfactory.dbot.service;

import xyz.dbotfactory.dbot.model.OCRResult;

public interface RecognService {
    OCRResult parseReceipt(String imageUrl);
}
