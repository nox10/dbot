package xyz.dbotfactory.dbot.handler;

import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

@Component
public interface OcrHelper {

    JSONObject sendGet(String urlString);
}
