package at.itundt.hallwang.ittsync;

import com.loopj.android.http.AsyncHttpResponseHandler;
import cz.msebera.android.httpclient.Header;

public abstract class ittAsyncHttpResponseHandler {

    public ittAsyncHttpResponseHandler(){
        super();
    }

    public abstract void onProgress(long bytesWritten, long totalSize);

    public abstract void onSuccess(int statusCode, byte[] responseBody);

    public abstract void onFailure(int statusCode, byte[] responseBody, Throwable error);

}

