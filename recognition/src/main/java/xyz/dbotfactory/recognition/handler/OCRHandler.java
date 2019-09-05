package xyz.dbotfactory.recognition.handler;


public interface OCRHandler {
    OCRResult parseImage(String image);
    int getPriority();
}
