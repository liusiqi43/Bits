package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.siqi.bits.Task;

import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.UnsupportedEncodingException;

/**
 * Created by me on 7/27/14.
 */
public class BitsRestClient {
    private static final String BASE_URL = "http://192.168.178.35:8080";

    private static AsyncHttpClient client = new AsyncHttpClient();

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }

    public void post(Task t, AsyncHttpResponseHandler responseHandler) {
        // Configure GSON
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Task.class, new BitsJsonSerializer());

        gsonBuilder.setPrettyPrinting();
        final Gson gson = gsonBuilder.create();

        StringEntity se;
        try {
            se = new StringEntity(gson.toJson(t));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

        client.post(null, getAbsoluteUrl("/dashboard/create"), se, "application/json", responseHandler);
    }
}
