package xyz.dbotfactory.recognition.service;

import xyz.dbotfactory.recognition.handler.OCRHandleInfo;

public interface RecognitionService {
    OCRHandleInfo parseReceipt(String imageUrl);
}
