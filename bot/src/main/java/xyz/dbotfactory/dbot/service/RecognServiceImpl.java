package xyz.dbotfactory.dbot.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xyz.dbotfactory.dbot.model.OCRResult;

import java.util.Objects;

@Service
public class RecognServiceImpl implements RecognService {
    private static final String GET_RESULT_URI = "http://localhost:8082/parse/get_result";

    @Override
    public OCRResult parseReceipt(String imageUrl) {

        return Objects.requireNonNull(new RestTemplate().postForObject(GET_RESULT_URI, imageUrl, OCRResult.class));

    }
}
