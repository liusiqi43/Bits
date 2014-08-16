package utils;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.UnsupportedEncodingException;

/**
 * Created by me on 7/27/14.
 */
public class BitsRestClient {
    private static final String BASE_URL = "http://bits-dashboard.herokuapp.com";

    private static AsyncHttpClient client = new AsyncHttpClient();

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }

  public void post(String json, AsyncHttpResponseHandler responseHandler) {
    StringEntity se = null;
    try {
      se = new StringEntity(json, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
    client.post(null, getAbsoluteUrl("/dashboard/create"), se, "application/json", responseHandler);
    }
}
