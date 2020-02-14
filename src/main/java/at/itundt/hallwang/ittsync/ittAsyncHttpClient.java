package at.itundt.hallwang.ittsync;
import android.app.Notification;
import android.content.Context;
import android.os.Looper;
import com.loopj.android.http.*;
import cz.msebera.android.httpclient.Header;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

public class ittAsyncHttpClient {

   public  ittNotification mittNotification;
   private  AsyncHttpClient clientAsync;
   private  SyncHttpClient clientSync;
   private ittAsyncHttpResponseHandler mResponseHandler;
   private RequestParams Params;
   private boolean showNotf = false;
   private  Context mContext;
   private  ittHandler mhandler;
   private  JSONObject inputStream;
   private  String appKey;
   private int state = -1;
   private  String dfAction;
   private  byte[] responseBody;
   private  int responseState;
   private  String notfTitle = "Daten werden übertragen..";
   private  String notfText = "bitte warten...";
   private  String host;

    public ittAsyncHttpClient(ittHandler handler){
        this.Params = new RequestParams();
        if(handler != null) {
            this.mhandler = handler;
            this.host = mhandler.getActivHost();
            this.mContext = handler.getContext();
        }

        this.host = host.trim();

    }

    public ittAsyncHttpClient(String host, ittHandler handler){
        this.Params = new RequestParams();
        this.showNotf = false;
        if(handler != null) {
            this.mhandler = handler;
            this.mContext = handler.getContext();
        }
        this.host = host;
        this.host = host.trim();
    }

    public ittAsyncHttpClient(String host){
        this.Params = new RequestParams();
        this.showNotf = false;
        this.host = host;
        this.host = host.trim();
    }
    private String getAppKey() {
        return appKey;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    private void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    private void setDfAction(String dfAction) {
        this.dfAction = dfAction;
    }

    private String getDfAction() {
        return dfAction;
    }

    public void setShowNotf(boolean showNotf) {
        this.showNotf = showNotf;
    }

    public void setNotfText(String notfText) {
        this.notfText = notfText;
    }

    public void setNotfTitle(String notfTitle) {
        this.notfTitle = notfTitle;
    }

    public String getNotfText() {
        return notfText;
    }

    public String getNotfTitle() {
        return notfTitle;
    }

    public void putJson(JSONObject stream, String name) {
        inputStream = stream;
        try {
            Params.put("JsonData", new ByteArrayInputStream(stream.toString().getBytes("utf-8")), name, "multipart/form-data");

            if(inputStream  != null){
                setAppKey(Functions.jsonGetString(inputStream,"AppKey"));
                setDfAction(Functions.jsonGetString(inputStream,"Action"));
            }

        } catch (Exception e) {
        }
    }
    public void putBinary(InputStream stream, String name) {


        Params.put("BinaryData", stream, name, "multipart/form-data");
    }

    public void putBinary(File file, String name) {
        try {
            Params.put("BinaryData", file, "multipart/form-data", name);
        }catch (Exception e){

        }
    }

    public  void runRequestSync(){

        if(inputStream == null)
            return;
        clientSync = new SyncHttpClient();

        if(getAppKey().isEmpty())
            return;

        if (Looper.myLooper()==null)
            Looper.prepare();

        //clientSync.setTimeout(5000);
        //clientSync.setMaxRetriesAndTimeout(1,5000);
        clientSync.addHeader("AppKey",getAppKey());
        clientSync.addHeader("DfAction",getDfAction());
        clientSync.post(this.host+System.Global.HttpMethodVersion,Params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                try {
                    if(response != null)
                        setResponseBody(response.toString().getBytes("utf-8"));

                    setResponseState(statusCode);
                }catch (Exception e){
                }
                if(showNotf) {
                    Functions.cancelNotificaion(mContext, Functions.CHANNEL_UPLOAD);
                    Functions.showNotifiaciton(mContext, Functions.CHANNEL_DEFAULT, "Upload erfolgreich", "DOCUframe", 5000, R.mipmap.ic_launcher);
                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                try {
                    if(errorResponse != null)
                        setResponseBody(errorResponse.toString().getBytes("utf-8"));

                    setResponseState(statusCode);
                }catch (Exception e){
                }
                if(showNotf) {
                    Functions.cancelNotificaion(mContext, Functions.CHANNEL_UPLOAD);
                    Functions.showNotifiaciton(mContext, Functions.CHANNEL_DEFAULT, "Upload erfolgreich", "DOCUframe", 5000, R.mipmap.ic_launcher);
                }
            }


        });


    }

    public void runRequestAsync(final ittAsyncHttpResponseHandler handler){
        if(inputStream == null)
           return;

        clientAsync = new AsyncHttpClient();

        if(getAppKey().isEmpty())
            return;

        //clientAsync.addHeader("Test","1");
        clientAsync.addHeader("AppKey",getAppKey());
        clientAsync.addHeader("DfAction",getDfAction());
        //clientAsync.setTimeout(5000);
        //clientAsync.setMaxRetriesAndTimeout(1,5000);
        clientAsync.put(this.host+System.Global.HttpMethodVersion, Params, new AsyncHttpResponseHandler() {

            @Override
            public void onStart(){
                if(showNotf) {
                    mittNotification = new ittNotification(mContext,Functions.CHANNEL_UPLOAD,getNotfTitle(),"");
                    mittNotification.getBuilder().setProgress(100,0,false);
                    mittNotification.init();
                }
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                if(state == -1) {
                    if (showNotf) {
                        long calc = (bytesWritten * 100) / totalSize;
                        String tmb =String.format("%.2f", Functions.ConvertBytesToMegabytes(totalSize));
                        String twr = String.format("%.2f",Functions.ConvertBytesToMegabytes(bytesWritten));

                        if(mittNotification != null){
                            mittNotification.updateProgressNotification(getNotfTitle() , "["+twr+" MB/"+tmb+" MB]", (int) calc);
                        }

                    }
                    handler.onProgress(bytesWritten, totalSize);
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                setState(statusCode);
                if(statusCode == 200) {
                    if (showNotf) {

                        try {

                            mittNotification.getBuilder().setProgress(0,0,false)
                                    .setContentText("DOCUframe")
                                    .setContentTitle("Upload erfolgreich")
                                    .setPriority(Notification.PRIORITY_MAX)
                                    .setOnlyAlertOnce(false)
                                    .setOngoing(false);
                            mittNotification.notifyShow();
                            mittNotification.cancelNotifcation(5000);

                            //Thread.sleep(500);
                            //Functions.showNotifiaciton(mContext, Functions.CHANNEL_DEFAULT, "Upload erfolgreich", "DOCUframe", 5000, R.mipmap.ic_launcher);
                        } catch (Exception e) {

                        }
                    }
                    setResponseBody(responseBody);
                    setResponseState(statusCode);
                    handler.onSuccess(statusCode, responseBody);
                }else{
                    if(showNotf) {
                        try {
                            Functions.cancelNotificaion(mContext, Functions.CHANNEL_UPLOAD);
                            Thread.sleep(500);
                            Functions.showNotifiaciton(mContext, Functions.CHANNEL_DEFAULT,
                                    "Upload fehlgeschlagen", "Fehlercode:" + statusCode + " (" + Functions.getHttpStatusCode(statusCode) + ")",
                                    5000, R.drawable.warning32);

                            handler.onFailure(statusCode, responseBody, null);
                        }catch (Exception e){

                        }
                    }

                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                setState(statusCode);
                if(showNotf) {
                    Functions.cancelNotificaion(mContext, Functions.CHANNEL_UPLOAD);
                    Functions.showNotifiaciton(mContext, Functions.CHANNEL_DEFAULT,
                            "Upload fehlgeschlagen", "Fehlercode:" + statusCode + " ("+Functions.getHttpStatusCode(statusCode)+")",
                            5000, R.drawable.warning32);                }
                setResponseBody(responseBody);
                setResponseState(statusCode);
                handler.onFailure(statusCode,responseBody,error);

            }
        });
    }


    public byte[] getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(byte[] responseBody) {
        this.responseBody = responseBody;
    }

    public int getResponseState() {
        return responseState;
    }

    public void setResponseState(int responseState) {
        this.responseState = responseState;
    }

}
