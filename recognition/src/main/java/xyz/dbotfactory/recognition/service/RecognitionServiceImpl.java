package xyz.dbotfactory.recognition.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.dbotfactory.recognition.handler.OCRHandleInfo;
import xyz.dbotfactory.recognition.handler.OCRHandlerAggregator;


@Service
public class RecognitionServiceImpl implements RecognitionService {
    @Autowired
    OCRHandlerAggregator ocrHandlerAggregator;

    @Override
    public OCRHandleInfo parseReceipt(String imageUrl) {
        return ocrHandlerAggregator.tryOCRHandlers(imageUrl);
    }
}
