package xyz.dbotfactory.dbot.handler;

import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class OcrHelperImpl implements OcrHelper {


    @Override
    @SneakyThrows
    public JSONObject sendGet(String urlString) {
        URL obj = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        int responseCode = con.getResponseCode();
        JSONObject o = null;
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String line;
            StringBuffer response = new StringBuffer();
            JSONParser j = new JSONParser();
            while ((line = in.readLine()) != null) {
                o = (JSONObject) j.parse(line);
            }
            in.close();
        }
        return o;
    }
}
