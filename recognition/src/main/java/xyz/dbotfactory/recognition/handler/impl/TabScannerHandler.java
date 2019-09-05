package xyz.dbotfactory.recognition.handler.impl;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONArray;
import org.springframework.stereotype.Component;
import xyz.dbotfactory.recognition.handler.OCRHandleInfo;
import xyz.dbotfactory.recognition.handler.OCRHandler;
import xyz.dbotfactory.recognition.model.Receipt;
import org.apache.http.client.HttpClient;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.IntStream;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import xyz.dbotfactory.recognition.model.ReceiptItem;

import static java.util.stream.Collectors.toList;

@Component
public class TabScannerHandler implements OCRHandler {
    private String apiKey = "MsfV5SbXY44ZUIt29Z6kN7w2hjwtT5I9LNfKBsqjs92B7NlGsoXG6t7BWEEQC7zB";

    @Override
    @SneakyThrows
    public OCRHandleInfo parseImage(String imageUrl) {
        JSONObject jsonObject = sendPost(imageUrl);

        if ((Long) jsonObject.get("status_code")== 4)
            return OCRHandleInfo.builder().handled(false).build();
        Thread.sleep(5000);
        JSONObject jsonReceipt = sendGet((String) jsonObject.get("token"));
        if ((Long) jsonReceipt.get("status_code")==1){
            return OCRHandleInfo.builder().handled(false).build();
        }
        Receipt receipt = convert(jsonReceipt);
        return OCRHandleInfo.builder().handled(true).handleInfo(receipt).build();
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @SneakyThrows
    public JSONObject sendPost(String imageUrl){
        String pathToImage = saveImage(imageUrl);

        File file = new File(pathToImage);
        String url = "https://api.tabscanner.com/"+this.apiKey+"/process";

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost post = new HttpPost(url);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addBinaryBody("receiptImage", file, ContentType.DEFAULT_BINARY, pathToImage);

        HttpEntity entity = builder.build();
        post.setEntity(entity);
        HttpResponse response = httpclient.execute(post);

        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        JSONParser j = new JSONParser();
        String line = "";
        while ((line = rd.readLine()) != null) {
            JSONObject o = (JSONObject)j.parse(line);
            return o;
        }
        JSONObject jsonObject = (JSONObject) j.parse("{\"status_code\":4");
        Files.deleteIfExists(Paths.get(pathToImage));
        return jsonObject;
    }

    @SneakyThrows
    private String saveImage(String imageUrl){
//        InputStream in = new URL(imageUrl).openStream();
        String fileName = imageUrl.substring( imageUrl.lastIndexOf('/')+1, imageUrl.length() );
//        BufferedImage image = ImageIO.read(new URL(imageUrl));
//        ImageIO.write(image, "jpg",new File(fileName));
        FileUtils.copyURLToFile(new URL(imageUrl), new File(fileName));
//        Files.copy(in, Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    @SneakyThrows
    private JSONObject sendGet(String token){
        String url = "https://api.tabscanner.com/"+this.apiKey+"/result/"+token;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        int responseCode = con.getResponseCode();
        JSONObject o = null;
        if (responseCode == HttpURLConnection.HTTP_OK){
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String line;
            StringBuffer response = new StringBuffer();
            JSONParser j = new JSONParser();
            while ((line = in.readLine()) != null) {
                o = (JSONObject)j.parse(line);
            }
            in.close();
        }
        return o;
    }

    private Receipt convert(JSONObject object){
        JSONObject result = (JSONObject)object.get("result");
        JSONArray lineItems = (JSONArray)result.get("lineItems");
        List<ReceiptItem> receiptItems = IntStream.range(0, lineItems.size())
                .mapToObj(i -> (JSONObject) lineItems.get(i))
                .map(x -> ReceiptItem.builder()
                        .name((String) x.get("descClean"))
                        .price(Double.valueOf((String) x.get("price")))
                        .amount((Long) x.get("qty")).build()).collect(toList());

        return Receipt.builder().isActive(true).items(receiptItems).build();
    }
}
