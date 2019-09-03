package xyz.dbotfactory.recognition.handler.impl;

import lombok.SneakyThrows;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import xyz.dbotfactory.recognition.handler.OCRHandleInfo;
import xyz.dbotfactory.recognition.handler.OCRHandler;
import xyz.dbotfactory.recognition.model.Receipt;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;

@Component
public class TabScannerHandler implements OCRHandler {
    private String apiKey = "MsfV5SbXY44ZUIt29Z6kN7w2hjwtT5I9LNfKBsqjs92B7NlGsoXG6t7BWEEQC7zB";

    @Override
    public OCRHandleInfo parseImage(String image) {
        return null;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @SneakyThrows
    private JSONObject sendPost(){
        String url = "https://api.tabscanner.com/"+this.apiKey+"/process";
        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        return null;
    }

    private JSONObject sendGet(String token){
        String url = "https://api.tabscanner.com/"+this.apiKey+"/result/"+token;
        return null;
    }

    private Receipt convert(JSONObject object){
        return null;
    }
}
