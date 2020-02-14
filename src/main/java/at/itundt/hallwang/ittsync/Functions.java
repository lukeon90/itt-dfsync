package at.itundt.hallwang.ittsync;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.PrecomputedText;
import com.loopj.android.http.*;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import at.itundt.hallwang.ittsync.DbHelper;
import at.itundt.hallwang.ittsync.R;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.entity.FileEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class Functions extends Application {

    public  static int THREAD_RUNNING_METHOD = 0;
    public  static final int THREAD_METHOD_TASK  = 7000;
    public  static final int THREAD_METHOD_TASK_ENTRIE = 7001;
    public  static final int THREAD_METHOD_CHECK = 7002;
    public  static final int THREAD_METHOD_CHECKCONNECTION = 7004;
    public  static final int THREAD_METHOD_SYNC  = 7100;
    public  static final int THREAD_METHOD_DOCUMENTS  = 7200;
    public  static  final int INIT_VIEWS = 500;
    public  static  final  int i_VALID_METHOD= 9000;
    public  static  final  int i_UNKNOWN_METHOD = 9001;
    public  static  final  int i_DEVICE_DEACTIVATED = 9002;
    public  static  final  int i_EMPTY_DATA = 9002;
    public  static  final  int i_HTTP_TIMEOUT = 9003;
    public  static  final  int i_NO_CONNECTION = 9003;
    public  static  final  int i_CONNECTION_FAILED = 9004;
    public  static  final  int i_UNKNOWN_USER = 9005;
    public  static  final  int i_UNKNOWN = 9006;
    public static final String ACCOUNT_TYPE = "at.hallwang.itundt.sync";
    public  static  final  String CHANNEL_DEFAULT = "Allgemein";
    public  static  final  String CHANNEL_UPLOAD  = "Dateiübertragung";


    private   static boolean showNotfic = false;
    public static Context xContext;
    public static android.accounts.Account[] Accounts;
    public static  AccountManager Manager;
    private static NotificationManager mNotificationManager;
    //public static Activity ShownActivity;


    public  static int getJsonState(JSONObject data){
        int result = jsonGetInt(data,"State");
        if(result == 0)
            result = i_EMPTY_DATA;
        return result;
    }

    public static <T extends View> ArrayList<T> getViewsByType(ViewGroup root, Class<T> tClass) {
        final ArrayList<T> result = new ArrayList<>();
        int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup)
                result.addAll(getViewsByType((ViewGroup) child, tClass));

            if (tClass.isInstance(child))
                result.add(tClass.cast(child));
        }
        return result;
    }

    public  static int jsonGetInt(JSONObject jdata,String value){

        int result = 0;
        try {
            result = jdata.getInt(value);
        }catch(Exception e){
            e.printStackTrace();
        }
        return  result;
    }

    public  static String jsonGetString(JSONObject jdata,String value){

        String result = "";
        try {
            result = jdata.getString(value);
        }catch(Exception e){
            e.printStackTrace();
        }
        return  result;
    }

    public static double ConvertBytesToMegabytes(long bytes)
    {
        return (bytes / 1024f) / 1024f;
    }

    static double ConvertKilobytesToMegabytes(long kilobytes)
    {
        return kilobytes / 1024f;
    }


    public static void TestUpload(final ittHandler mhandler, File file){
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        try {
            params.put("BinaryData", file, "multipart/form-data", "test.pdf");
        }catch (Exception e){

        }
        client.addHeader("Test","1");
        client.post(mhandler.getActivHost()+System.Global.HttpMethodVersion, params, new AsyncHttpResponseHandler() {

            @Override
            public void onStart() {
                Functions.showNotifiacitonProgress(mhandler.getContext(), Functions.CHANNEL_UPLOAD, "DOCUframe upload", "bitte warten...", 0);

            }
            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                super.onProgress(bytesWritten,totalSize);
                long calc = (bytesWritten * 100) / totalSize;
                String tmb =String.format("%.2f", ConvertBytesToMegabytes(totalSize));
                String twr = String.format("%.2f", ConvertBytesToMegabytes(bytesWritten));

                Functions.showNotifiacitonProgress(mhandler.getContext(), Functions.CHANNEL_UPLOAD,"Daten werden übertragen.." , "["+twr+" MB/"+tmb+" MB]", (int) calc);
            }


            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Functions.cancelNotificaion(mhandler.getContext(), Functions.CHANNEL_UPLOAD);
                Functions.showNotifiaciton(mhandler.getContext(), Functions.CHANNEL_DEFAULT, "Upload erfolgreich", "DOCUframe", 5000, R.mipmap.ic_launcher);

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Functions.cancelNotificaion(mhandler.getContext(), Functions.CHANNEL_UPLOAD);
                Functions.showNotifiaciton(mhandler.getContext(), Functions.CHANNEL_DEFAULT,
                        "Upload fehlgeschlagen", "Fehlercode:" + statusCode + " ("+Functions.getHttpStatusCode(statusCode)+")",
                        5000, R.drawable.warning32);
            }
        });

    }

    public  static void showNotifiacitonProgress(Context mContext, String channelid, String title, String text, int percent){
        showNotifiacitonProgress(mContext,channelid,title,text,percent,false);
    }

    public  static void showNotifiacitonProgress(Context mContext, String channelid, String title, String text, int percent , boolean create ){

        String cID = "ittsync_"+channelid;
        NotificationManager mNotificationManager;
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext.getApplicationContext(), cID);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(text);
        //mBuilder.setPriority(Notification.PRIORITY_MAX);
        mBuilder.setProgress(100,percent,false);

        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if(create) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        cID,
                        channelid,
                        NotificationManager.IMPORTANCE_HIGH);
                mNotificationManager.createNotificationChannel(channel);
                mBuilder.setChannelId(cID);
            }
        }
        mNotificationManager.notify(0, mBuilder.build());
    }

    public  static void showNotifiaciton(Context mContext, String channelid, String title, String text, int time, int icon){

        String cID = "ittsync_"+channelid;
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext.getApplicationContext(), cID);

        if(icon == 0)
            icon = R.mipmap.ic_launcher;

        mBuilder.setSmallIcon(icon);
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(text);
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(
                    cID,
                    channelid,
                    NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(cID);
        }
        mNotificationManager.notify(0, mBuilder.build());

        if(time > 1000) {
            Handler h = new Handler();
            long delayInMilliseconds = time;
            h.postDelayed(new Runnable() {
                public void run() {
                    mNotificationManager.cancelAll();
                }
            }, delayInMilliseconds);
        }

    }

    public static void cancelNotificaion(Context mContext, String channelid){
        String cID = "ittsync_"+channelid;
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();

    }

    public static JSONObject makeDfRequest(ittHandler handler, JSONObject request, boolean saveResponse, boolean newx) {

            if (handler == null)
                throw new NoClassDefFoundError();
            if (handler.getActiveAccount() == null)
                throw new NoClassDefFoundError();
            if (handler.getConnection() == null)
                throw new NoClassDefFoundError();
            String host = handler.getConnection().accountGeFieldValue(handler.getActiveAccount(), " ");



            return makeDfRequest(handler, request, saveResponse, host, true);
        }


    public static JSONObject makeDfRequest(ittHandler handler, JSONObject request,boolean saveResponse, String ServerWithPort, boolean withAccount) {

        if(handler == null)
            throw new NoClassDefFoundError();

        if(withAccount) {
            if (handler.getActiveAccount() == null)
                throw new NoClassDefFoundError();
        }
        if(handler.getConnection() == null)
            throw new NoClassDefFoundError();

        JSONObject jsonResult = new JSONObject();
        try {
            ittAsyncHttpClient Connection = new ittAsyncHttpClient(ServerWithPort,handler);
            Connection.putJson(request,"JsonData");
            Connection.runRequestSync();

            jsonResult = new JSONObject(new String(Connection.getResponseBody(), "UTF-8"));

            String data =  Functions.jsonGetString(jsonResult,"Data");
            data        = Functions.decodeBase64(data);
            jsonResult  = new JSONObject(data);
            String oid  = Functions.jsonGetString(jsonResult,"Tag");
            String  message = Functions.jsonGetString(jsonResult,"Message");
            Integer state =  Integer.valueOf(Functions.jsonGetString(jsonResult,"State"));
            jsonResult.put("State", state);

            if(!oid.isEmpty() && withAccount)
                handler.getConnection().dbInsertdynamicCache(oid,jsonResult.toString(),ServerWithPort);

        }catch (Exception error){
            try {
                jsonResult.put("State", i_CONNECTION_FAILED);
                jsonResult.put("Message", error.getMessage());
                //handler.ShowErrorBox("makeDfRequest: "+ error.getMessage());

            }catch (Exception je){
                //handler.ShowErrorBox("makeDfRequest: "+ je.getMessage());
            }
        }
        return  jsonResult;
    }

    public static Object getBuildConfigValue(Context context, String fieldName) {
        try {
            Class<?> clazz = Class.forName(context.getPackageName() + ".BuildConfig");
            Field field = clazz.getField(fieldName);
            return field.get(null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    // String Results
    public static String md5_Hash(String s) {
        MessageDigest m = null;

        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {

        }
        try {
            m.update(s.getBytes(), 0, s.length());
        }catch (NullPointerException e){

        }
        String hash = new BigInteger(1, m.digest()).toString(16);

        return hash;
    }



    public static String getHttpStatusCode(int code){
        String message = "OK";


        switch(code) {
            case 0:
                message = "Timeout - Bitte Internetverbindung prüfen";
                break;
            case 100:
                message = "Continue";
                break;
            case 101:
                message = "SwitchingProtocols";
                break;
            case 200:
                message = "OK";
                break;
            case 201:
                message = "Created";
                break;
            case 202:
                message = "Accepted";
                break;
            case 203:
                message = "NonAuthoritativeInformation";
                break;
            case 204:
                message = "NoContent";
                break;
            case 405:
                message = "MethodNotAllowed";
                break;
            case 206:
                message = "PartialContent";
                break;
            case 300:
                message = "MultipleChoices";
                break;
            case 400:
                message = "BadRequest";
                break;
            case 401:
                message = "Unauthorized";
                break;
            case 403:
                message = "Forbidden";
                break;
            case 404:
                message = "NotFound";
                break;
            case 505:
                message = "HttpVersionNotSupported";
                break;
            default:
                message = "Undefined";
                break;
        }

        return  message;
    }


    public static String getStringFile(File f) {
        InputStream inputStream = null;
        String encodedFile= "", lastVal;
        try {
            inputStream = new FileInputStream(f.getAbsolutePath());

            byte[] buffer = new byte[10240];//specify the size to allow
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output64.write(buffer, 0, bytesRead);
            }


            output64.close();


            encodedFile =  output.toString();

        }
        catch (FileNotFoundException e1 ) {
            e1.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        lastVal = encodedFile;
        return lastVal;
    }

    public static byte[] fileToByteArray(File file) throws IOException {


        byte[] buffer = new byte[(int) file.length()];
        InputStream ios = null;
        try {
            ios = new FileInputStream(file);
            if (ios.read(buffer) == -1) {
                throw new IOException(
                        "EOF reached while trying to read the whole file");
            }
        } finally {
            try {
                if (ios != null)
                    ios.close();
            } catch (IOException e) {
            }
        }
        return buffer;
    }

    public static String decodeBase64(java.lang.String coded){
        byte[] valueDecoded= new byte[0];
        try {
            valueDecoded = Base64.decode(coded.getBytes("UTF-8"), Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
        }
        return new java.lang.String(valueDecoded);
    }

    public static class Permission{

        public static final String[] EXTERNAL_PERMS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        public static final int EXTERNAL_REQUEST = 138;

        public static  boolean canAccessExternalSd(Context context) {
            return (hasPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE));
        }

        private static boolean hasPermission(Context context , String perm) {
            return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, perm));

        }


        public  static  boolean hasDbPermission(final Context context){
            boolean isPermissionOn = true;
            final int version = Build.VERSION.SDK_INT;
            if (version >= 23) {
                isPermissionOn = Functions.Permission.canAccessExternalSd(context);
            }
            return  isPermissionOn;
        }

        public static boolean requestForPermission(final Activity activity, final Context context) {

            boolean isPermissionOn = true;
            final int version = Build.VERSION.SDK_INT;
            if (version >= 23) {
                if (!Functions.Permission.canAccessExternalSd(context)) {
                    isPermissionOn = false;
                    ActivityCompat.requestPermissions(activity,Functions.Permission.EXTERNAL_PERMS,Functions.Permission.EXTERNAL_REQUEST);
                }
            }
            return isPermissionOn;
        }
    }

    public static class AccountCfg{


        public static Account getAccountbyHost(Context context, String hostname){

            Account mAccount = null;
            AccountManager manager = AccountManager.get(context);
            android.accounts.Account[] accounts     = manager.getAccountsByType(Functions.ACCOUNT_TYPE);
            for (android.accounts.Account account : accounts) {
                String xHost = manager.getUserData(account, "Host");
                if(xHost.equals(hostname)){
                    return  account;
                }
            }
            return null;
        }

        public static android.accounts.Account CreateSyncAccount(Context context, String Host, String Description, String Db, String AUTHORITY) {
            android.accounts.Account newAccount = null;
            AccountManager manager = AccountManager.get(context);
            android.accounts.Account[] accounts     = manager.getAccountsByType(Functions.ACCOUNT_TYPE);
            Integer e = 0;
            for (android.accounts.Account account : accounts) {
                String xHost = manager.getUserData(account, "Host");
                if(xHost.equals(Host)){
                    e++;
                    newAccount = account;
                }
            }
            if(false){
                ;
            }else {
                final Bundle extraData = new Bundle();
                String Hash = Functions.md5_Hash(Host);
                Hash = Hash.substring(0, 7);
                extraData.putString("Host", Host);
                extraData.putString("Description", Description);
                extraData.putString("Database", Db);
                extraData.putString("ID","h"+Hash);
                extraData.putString("AppKey", Functions.md5_Hash(Host+Description));
                newAccount = new android.accounts.Account(Db,Functions.ACCOUNT_TYPE);
                AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);

                try {
                    if (android.os.Build.VERSION.SDK_INT >= 22) {
                        accountManager.removeAccountExplicitly(newAccount);
                    } else {

                        accountManager.removeAccount(newAccount, null, null);
                    }

                    if (accountManager.addAccountExplicitly(newAccount, null, extraData)) {
                        ContentResolver.requestSync(newAccount, AUTHORITY, Bundle.EMPTY);
                        ContentResolver.setSyncAutomatically(newAccount, AUTHORITY, true);
                        ContentResolver.addPeriodicSync(newAccount, AUTHORITY, Bundle.EMPTY, 90);



                    } else {
                        Toast.makeText(context, "CreateSyncAccount: error occured", Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception ex){
                    ;
                 }
            }


            return newAccount;
        }


    }


    public  class ittAccountException extends Throwable{

        public ittAccountException() {
            throw new RuntimeException("Kein Account gefunden!");
        }
    }

}
