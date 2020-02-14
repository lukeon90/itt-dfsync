package at.itundt.hallwang.ittsync.Sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;
import at.itundt.hallwang.ittsync.DbHelper;
import at.itundt.hallwang.ittsync.Functions;
import at.itundt.hallwang.ittsync.ittAsyncHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import javax.net.ssl.HttpsURLConnection;

public class SyncAdapter extends AbstractThreadedSyncAdapter  {

    private static final String TAG = SyncAdapter.class.getSimpleName();

    // Define a variable to contain a content resolver instance
    private ContentResolver mContentResolver;
    private Context mContext;
    private Integer sCount = -1;
    private DbHelper mDbHelper;

    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        try {
            mContext = context;
            mDbHelper = new DbHelper(mContext);
            mDbHelper.loadDatabase();
            mContentResolver = context.getContentResolver();
            Log.d(TAG, "SyncAdapter:  ctr");
        }catch (Exception e){

        }


    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        sCount   = 1;
        mContext = context;
        mContentResolver = context.getContentResolver();
        mDbHelper = new DbHelper(mContext);
        mDbHelper.loadDatabase();
        Log.d(TAG, "SyncAdapter:  ctr");
    }

    /*
     * Specify the code you want to run in the sync adapter. The entire
     * sync adapter runs in a background thread, so you don't have to set
     * up your own background processing.
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult)  {

        mDbHelper.setAccount(account);
        Log.d(TAG, "onPerformSync:  + called");
        mDbHelper.accountResetSyncValues(account);
        /*
        ******************************************************************************
        * Starte Synchronisation solange bis keine Daten mehr zum abholen sind.
        */
        try {
            sCount = 1;
            while (sCount > 0){
                sCount = xRA_xMAGetSyncObjects(account,true);
                Thread.sleep(1000);
            }
        }catch (Exception ex){
            mDbHelper.setErrorLog(account,ex.getMessage());
        }

        mDbHelper.accountSeFieldValue(account, DbHelper.sync_EMUM_LastSyncInfos, (String)new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));



        /*
         ******************************************************************************
        */
        //Wenn Errors vorhanden sind dann im syncresult markieren
        String errors = mDbHelper.accountGeFieldValue(account, mDbHelper.getErrorTag());
        if(errors.length() > 1) {
            syncResult.databaseError = true;
        }
        //Schicke Broadcast an die die MainActivity um bescheid zu sagen dass der Sync fertig ist.
        try {
            Intent i = new Intent("account.finished");
            mContext.sendBroadcast(i);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private JSONObject xRAPerformSync(JSONObject request, Account account) {

        JSONObject jsonResult = new JSONObject();
        AccountManager manager = AccountManager.get(mContext);
        String host = manager.getUserData(account, "Host");
        host = host.trim();


        try {
            ittAsyncHttpClient Connection = new ittAsyncHttpClient(host, null);
            Connection.putJson(request, "JsonData");
            Connection.runRequestSync();

            if(Connection.getResponseBody() != null) {
                jsonResult = new JSONObject(new String(Connection.getResponseBody(), "UTF-8"));
            }

        }catch (Exception error){
            mDbHelper.setErrorLog(account,error.getMessage());

        }

        return  jsonResult;
    }

    private Integer xRA_xMAGetSyncObjects(Account account, boolean SyncStruct){
        String Action_SyncObjects     = "xMAGetSyncObjects";
        String Action_SyncResult      = "xMAGetSyncStruct";
        String UpdateIDs =  "";
        String ID = "";
        Integer iCount = 0;
        AccountManager manager = AccountManager.get(mContext);
        String AppKey = manager.getUserData(account, "AppKey");
        String host   = manager.getUserData(account, "Host");
        String descr   = manager.getUserData(account, "Description");
        try {
            ID =  manager.getUserData(account, "ID");
            UpdateIDs = manager.getUserData(account, "UpdateIDs");
            if(UpdateIDs == null){
                UpdateIDs = "";
            }
        }catch (Exception e0){
            e0.printStackTrace();
        }

        JSONObject jsonRequest = new JSONObject();
        Exception  ex = null;
        try {
            jsonRequest.put("Action", Action_SyncObjects);
            jsonRequest.put("AppKey", AppKey);
            jsonRequest.put("Description", descr);
            jsonRequest.put("SyncStruct", SyncStruct);
            jsonRequest.put("Token", "B3ECFD39-03EE-4A07-9F83-EF58BD7889F");

            if(!UpdateIDs.isEmpty()){
                jsonRequest.put("UpdateIDs", UpdateIDs);
            }
            //HTTPCLIENT
            JSONObject jsonOutput = xRAPerformSync(jsonRequest,account);

            if(jsonOutput != null && !mDbHelper.hasErrors()) {
                String data = "";
                try {
                    data = jsonOutput.getString("Data");
                    data = Functions.decodeBase64(data);

                    if (!data.isEmpty()) {
                        jsonOutput = new JSONObject(data);

                        // Überprüfe ob die Sync Struktur enthalten ist. Wenn im Request SyncStruct = true mit gegeben wurde, wird die struktur geladen.
                        // Check xMAGetSyncStruct_Result if exist
                        try {
                            JSONArray arrStruct = jsonOutput.getJSONArray(Action_SyncResult + "_Result");
                            mDbHelper.updateOrAddStructFromEx(account, arrStruct, ID, host);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // Suche nach dem Syncobject Result
                        try {
                            JSONArray arr = jsonOutput.getJSONArray(Action_SyncObjects + "_Result");
                            //Setze UpdateFlag auf null
                            UpdateIDs = "";
                            manager.setUserData(account, "UpdateIDs", "");
                            mDbHelper.accountSeFieldValue(account, "Licence", "active");
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject object = arr.optJSONObject(i);
                                Iterator<String> iterator = object.keys();
                                while (iterator.hasNext()) {
                                    String currentKey = iterator.next();
                                    if (currentKey.contains("dynamictable_")) {
                                        JSONArray arr2 = object.getJSONArray(currentKey);
                                        iCount = arr2.length();
                                        for (int r = 0; r < arr2.length(); r++) {
                                            JSONObject entr = arr2.optJSONObject(r);
                                            String cExecute = "";
                                            int SyncTagCount = entr.getInt("SyncTagCount");
                                            String SyncTagID = entr.getString("SyncTagID");
                                            String cOid = entr.getString("ObjectID");
                                            String cTableID = entr.getString("TableID");
                                            String cTableName = entr.getString("TableName");
                                            String cTablename = ID + "_tableid_" + cTableName;

                                            cExecute = "INSERT OR REPLACE INTO " + cTablename + " VALUES " +
                                                    " (NULL, '" + SyncTagID + "', " + SyncTagCount + ", '" + cOid + "', '" + host + "'";

                                            Iterator<String> iter = entr.keys();
                                            while (iter.hasNext()) {
                                                String key = iter.next();
                                                if (key.startsWith("_")) {  // Benutzerfelder starten mit  _
                                                    try {
                                                        String value = entr.get(key).toString();
                                                        cExecute += ",'" + value + "'";
                                                    } catch (JSONException e) {
                                                        mDbHelper.setErrorLog(account, e.getMessage());
                                                    }
                                                }
                                            }
                                            cExecute += ");";
                                            //Mark updateID
                                            //Wenn keine fehler auftreten-> freigeben um die objekte als gesynct zu markieren
                                            if (!mDbHelper.hasErrors(account)) {
                                                UpdateIDs += SyncTagID + "," + SyncTagCount + "|";
                                            }
                                            try {
                                                mDbHelper.getDatabase().execSQL(cExecute);
                                            } catch (Exception error) {
                                                mDbHelper.setErrorLog(account, error.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                            if (!UpdateIDs.isEmpty()) {
                                manager.setUserData(account, "UpdateIDs", UpdateIDs);
                            }
                        } catch (Exception e) {
                            iCount = 0;
                            mDbHelper.setErrorLog(account, e.getMessage());
                            try {
                                String message = jsonOutput.getString("Message");

                                if (message.contains("deac")) {
                                    mDbHelper.accountSeFieldValue(account, "Licence", " deactivted");
                                }

                                mDbHelper.setErrorLog(account, message);
                            } catch (Exception error) {
                                mDbHelper.setErrorLog(account, error.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    iCount = 0;
                    mDbHelper.setErrorLog(account,e.getMessage());
                }
            }else{
                if(!mDbHelper.hasErrors())
                    mDbHelper.setErrorLog(account,"JSONObject empty on xRAPerformSync");
            }
        }catch (Exception e){
            iCount = 0;
            mDbHelper.setErrorLog(account,e.getMessage());
        }
        if(mDbHelper.hasErrors(account)) {
            iCount = 0;
        }


        return iCount;
    }
}


/**
 public void xRA_xMAGetSyncStruct(Account account){
 String Action     = "xMAGetSyncStruct";
 AccountManager manager = AccountManager.get(mContext);
 String AppKey = manager.getUserData(account, "Description");
 String ID = manager.getUserData(account, "ID");
 String host = manager.getUserData(account, "Host");
 JSONObject jsonRequest = new JSONObject();
 Exception  ex = null;
 try {
 jsonRequest.put("Action", Action);
 jsonRequest.put("AppKey", AppKey);
 jsonRequest.put("Token", "B3ECFD39-03EE-4A07-9F83-EF58BD7889F");
 //Send Request to Server
 JSONObject jsonOutput = xRAPerformSync(jsonRequest,account, ex);
 if(jsonOutput != null) {
 String data = "";
 try {
 data = jsonOutput.getString("Data");
 data = Functions.decodeBase64(data);
 jsonOutput = new JSONObject(data);
 JSONArray arr = jsonOutput.getJSONArray(Action+"_Result");
 DbHelper.updateOrAddStructFromEx(account,arr,ID,host);

 } catch (Exception error) {

 }
 }

 }catch (Exception error){



 }finally {

 }
 }
 **/
