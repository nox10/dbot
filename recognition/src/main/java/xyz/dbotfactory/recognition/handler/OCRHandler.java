package xyz.dbotfactory.recognition.handler;


public interface OCRHandler {
    OCRHandleInfo parseImage(String image);
    int getPriority();
}
