package xyz.dbotfactory.recognition.service;

import xyz.dbotfactory.recognition.handler.OCRResult;

public interface RecognitionService {
    OCRResult parseReceipt(String imageUrl);
}
