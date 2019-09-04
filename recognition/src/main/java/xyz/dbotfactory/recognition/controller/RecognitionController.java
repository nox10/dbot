package xyz.dbotfactory.recognition.controller;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.dbotfactory.recognition.handler.OCRHandleInfo;
import xyz.dbotfactory.recognition.service.RecognitionService;

@RestController
@RequestMapping("/parse")
@Log
public class RecognitionController {

    private final RecognitionService recognitionService;

    @Autowired
    public  RecognitionController(RecognitionService recognitionService){
        this.recognitionService = recognitionService;
    }

    @PostMapping("get_result")
    public OCRHandleInfo getParsedReceipt(@RequestBody String imageUrl){
        log.info("new parse request:" + imageUrl);
        return recognitionService.parseReceipt(imageUrl);
    }
}
