package xyz.dbotfactory.recognition.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.dbotfactory.recognition.handler.OCRResult;
import xyz.dbotfactory.recognition.handler.OCRHandlerAggregator;


@Service
public class RecognitionServiceImpl implements RecognitionService {
    @Autowired
    OCRHandlerAggregator ocrHandlerAggregator;

    @Override
    public OCRResult parseReceipt(String imageUrl) {
        return ocrHandlerAggregator.tryOCRHandlers(imageUrl);
    }
}
