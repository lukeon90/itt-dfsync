package at.itundt.hallwang.ittsync;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;


public class DbHelper  {
    public JSONObject JsonData = null;
    private final String CACHE_TABLE = "dynamicCacheTable";
    private SQLiteDatabase mDatabase = null;
    private String mErrorMsg = "";
    private Context mContext;
    private boolean mDatabaseLoaded;
    private Account mAccount;
    // Database Info
    private  final String DATABASE_PATH =  Environment.getExternalStorageDirectory().getAbsolutePath() + "/itundt/";
    private  final String DATABASE_NAME = DATABASE_PATH + "itt_dynamic.db";
    public   final String ErrorTag = "errormsg";

    public  static final  String sync_EMUM_ErrorTag = "errormsg";
    public  static final  String sync_EMUM_LastErrorMsg = DbHelper.sync_EMUM_ErrorTag;
    public  static final  String sync_EMUM_LastSuccessSync = "LastSuccessSync";
    public  static final  String sync_EMUM_LastSyncInfos = "LastSyncInfos";


    public DbHelper(Context context) {
        mContext = context;
        loadDatabase();
    }

    public void ShowErrorBox(String Message) {
        if(getContext() != null) {
            final Dialog dialog = new Dialog(getContext()); // Context, this, etc.
            dialog.setContentView(R.layout.dialog_box);
            TextView msg = (TextView)dialog.findViewById(R.id.dialog_info);
            msg.setText(Message);
            dialog.setTitle("ERROR OCCURED");
            dialog.show();

        }
    }


    public DbHelper(Context context, Account account) {
        mAccount = account;
        mContext = context;
        loadDatabase();
    }

    public String getErrorTag(){
        return ErrorTag;
    }
    public boolean isDatabaseOpen(){
        mDatabaseLoaded = false;
        if(mDatabase != null){
            mDatabaseLoaded = mDatabase.isOpen();
        }
        return mDatabaseLoaded;
    }



    public Context getContext() {
        return mContext;
    }

    public Account getAccount(){
        return mAccount;

    } public void setAccount(Account Account){
        mAccount = Account;
    }

    public void setContext(Context context) {
        mContext = context;
    }


    public SQLiteDatabase getDatabase(){
        return mDatabase;
    }

    public String getActivHost(){

        return accountGeFieldValue(getAccount(),"Host");
    }

    public String getErrorMessage(){
        return mErrorMsg;
    }

    public void updateOrCreateCacheTable(){
        mErrorMsg = "";
        String execute = " CREATE TABLE IF NOT EXISTS "+CACHE_TABLE+" (";
        execute += "row_id INTEGER PRIMARY KEY AUTOINCREMENT,";
        execute += "tag TEXT NOT NULL UNIQUE,";
        execute += "lastupdate TEXT,";
        execute += "lastsaved TEXT,";
        execute += "host TEXT NOT NULL,";
        execute += "target TEXT NOT NULL,";
        execute += "cache BLOB);";
        try {
            mDatabase.execSQL(execute);
            mDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_dynamicCache_tag ON " + CACHE_TABLE + " (tag);");
        }catch (Exception e){
            mErrorMsg = e.getMessage();
            dbWriteErrorLog("dbInsertCacheRequest",e.getMessage());
        }

    }

    public void dbInsertdynamicCache(String tag, String cache, String fullUrl){
        mErrorMsg = "";
        String host = accountGeFieldValue(getAccount(), "Host");
        try {
            SimpleDateFormat fdate = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String dateString = fdate.format( new Date()   );

            String execute = "INSERT OR REPLACE INTO "+CACHE_TABLE+" VALUES " +
                    "(NULL, '"+tag+"', '"+dateString+"','NULL','"+getActivHost()+"','"+fullUrl+"','"+cache+"')";
            mDatabase.execSQL(execute);

        }catch (Exception e){
            mErrorMsg = e.getMessage();
            dbWriteErrorLog("dbInsertCacheRequest",e.getMessage());
        }
    }

    public Date dbGetDynamicRow_lastupdate(String tag) {
        mErrorMsg = "";
        Date date = null;
        String sql = "";
        sql += "SELECT * FROM " + CACHE_TABLE;
        sql += " WHERE tag = '" + tag + "'";

        try {
            Cursor cursor = mDatabase.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                do {
                    String  lastupdate = cursor.getString(cursor.getColumnIndex("lastupdate"));
                    SimpleDateFormat fdate = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    date = fdate.parse(lastupdate);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            mErrorMsg = e.getMessage();
            dbWriteErrorLog("dbGetDynamicRow_lastupdate",e.getMessage());
        }

        return date;
    }

    public List<String> dbGetTableNames(){
        List<String> result = new ArrayList<String>();
        String sql = "SELECT name FROM sqlite_master WHERE type='table'";
        try {
            Cursor cursor = mDatabase.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                do {
                    String  tablename = cursor.getString(cursor.getColumnIndex("name"));
                    result.add(tablename);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            mErrorMsg = e.getMessage();
            dbWriteErrorLog("dbGetTableNames",e.getMessage());
        }
        return result;
    }

    public List<String> dbGetTableNamesbyId(String name){
        List<String> result = new ArrayList<String>();
        String sql = "SELECT name FROM sqlite_master WHERE type='table'";
        try {
            Cursor cursor = mDatabase.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                do {
                    String  tablename = cursor.getString(cursor.getColumnIndex("name"));
                    if(tablename.toLowerCase().contains(name.toLowerCase()))
                        result.add(tablename);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            mErrorMsg = e.getMessage();
            dbWriteErrorLog("dbGetTablebyId",e.getMessage());
        }
        return result;
    }

    public JSONObject dbGetDynamicRow_cache(String tag) {
        JSONObject jResult = null;

        String sql = "";
        sql += "SELECT * FROM " + CACHE_TABLE;
        sql += " WHERE tag = '" + tag + "'";

        try {
            Cursor cursor = mDatabase.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                do {
                    String jString = cursor.getString(cursor.getColumnIndex("cache"));
                    jResult = new JSONObject(jString);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            dbWriteErrorLog("dbGetDynamicRow_cache",e.getMessage());
        }
        return jResult;
    }

    public void dbWriteErrorLog(String tag, String message){
        mErrorMsg = "";
        String execute = " CREATE TABLE IF NOT EXISTS app_exceptions (";
        execute += "row_id INTEGER PRIMARY KEY AUTOINCREMENT,";
        execute += "tag TEXT NOT NULL,";
        execute += "message TEXT);";
        try {
            mDatabase.execSQL(execute);
            execute = "INSERT OR REPLACE INTO app_exceptions VALUES (NULL, '"+tag+"', '"+message+"')";
            mDatabase.execSQL(execute);
        }catch (Exception e){
            mErrorMsg = e.getMessage();
        }

    }

    public void updateOrAddStructFromEx(final Account account, final JSONArray m_data,final String id,final String host) {

        mAccount = account;
        for (int i = 0; i < m_data.length(); i++) {
            JSONObject object = m_data.optJSONObject(i);
            Iterator<String> iterator = object.keys();
            while (iterator.hasNext()) {
                String currentKey = iterator.next();
                String cTablename = currentKey;
                String cTablename2 = "";
                String cVersion = "";
                String cTName= "";
                int TableID = 0;
                if (currentKey.contains("dynamictable_")) {
                    try {;
                        JSONObject entr = object.optJSONObject(currentKey);
                        TableID = entr.getInt("tableid");
                        cVersion = Functions.jsonGetString(entr,"version");
                        cTName = Functions.jsonGetString(entr,"name");


                        cTablename = id+ "_tableid_"+cTName;
                        cTablename2 = currentKey.replace("dynamictable_","");

                        //Tabelle wird gelöscht qwenn version unterschied erkannt wird.
                        dbAddNewTableIdRow(id,String.valueOf(TableID),cTName,cTablename2,host,cVersion);

                        //Erstelle Dynamische Tabelle
                        String execute = " CREATE TABLE IF NOT EXISTS "+cTablename+" (";
                        execute += "row_id INTEGER PRIMARY KEY AUTOINCREMENT,";
                        execute += "tag TEXT NOT NULL,";
                        execute += "tag_count REAL,";
                        execute += "oid TEXT NOT NULL UNIQUE,";
                        execute += "host TEXT NOT NULL,";
                        Iterator<String> iter = entr.keys();
                        while (iter.hasNext()) {
                            String key = iter.next();
                            if (true) {
                                try {
                                    Object value = entr.get(key);
                                    String x = value.toString();
                                    if(isValidType(x)){
                                        execute += key.toLowerCase() + " " + value.toString().toUpperCase() + ",";
                                    }
                                } catch (JSONException e) {
                                    // Something went wrong!
                                }
                            }
                        }
                        execute = execute.substring(0, execute.length() - 1);
                        execute += ");";

                        try{
                            mDatabase.execSQL(execute);
                            mDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_oid ON " + cTablename + " (oid);");

                        }
                        catch (Exception e) {
                            setErrorLog(account,e.getMessage());
                        }

                    } catch (Exception e) {
                        setErrorLog(account,e.getMessage());

                    }
                }
            }
        }
    }

    public void dbAddNewTableIdRow(String id, final String tableid,String tablen,
                                   final String tablename, final String host,final String version){

        //Create Table if not exist
        String cExecute = "";
        String cTablename = id+"_dtables";
        String cTablename2  = id+ "_tableid_"+tablen;

        //Check Vesion
        try {
            cExecute = "SELECT version, COUNT(*) FROM " + cTablename + " WHERE dtable_id = '" + tableid + "' ";
            Cursor c = mDatabase.rawQuery(cExecute, null);
            if (c.moveToFirst()) {
                do {
                    // Passing values
                    String c1 = c.getString(0);
                    if(c1 != null) {
                        if (!c1.equals(version)) {
                            //mDatabase.execSQL("UPDATE "+cTablename+" SET version = '"+version+"' WHERE dtable_id = '" + tableid + "' ");
                            mDatabase.execSQL("DROP TABLE IF EXISTS " + cTablename2 + "");
                        }
                    }
                } while (c.moveToNext());
            }
            c.close();
        }catch (Exception e){
            mDatabase.execSQL("DROP TABLE IF EXISTS "+cTablename+"");
        }

        cExecute = " CREATE TABLE IF NOT EXISTS "+cTablename+" (";
        cExecute += "row_id INTEGER PRIMARY KEY AUTOINCREMENT,";
        cExecute += "dtable_id TEXT NOT NULL UNIQUE,";
        cExecute += "dtable_name TEXT NOT NULL,";
        cExecute += "version TEXT NOT NULL,";
        cExecute += "host TEXT NOT NULL);";
        try {
            mDatabase.execSQL(cExecute);
            mDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_dtables_id ON " + cTablename + " (dtable_id);");

            cExecute = "INSERT OR REPLACE INTO " + cTablename + " VALUES (NULL, '" + tableid + "', '"+tablename+"', '"+version+"', '" + host + "')";
            mDatabase.execSQL(cExecute);


        }catch (Exception e){
            setErrorLog(mAccount,e.getMessage());
        }

    }

    public  boolean isValidType(String cText){


        boolean result = false;


        if(cText.equals("BLOB")){
            result = true;
        }
        if(cText.equals("TEXT")){
            result = true;
        }
        if(cText.equals("REAL")){
            result = true;
        }
        if(cText.equals("INTEGER")){
            result = true;
        }
        if(cText.equals("NUMERIC")){
            result = true;
        }


        return  result;


    }

    public void loadDatabase(){
        if(getContext() != null) {
            if (Functions.Permission.hasDbPermission(getContext())) {
                try {
                    mErrorMsg = "";
                    boolean bok = true;
                    File dirFile = new File(DATABASE_PATH);
                    if(!dirFile.exists())
                        bok = dirFile.mkdir();
                    if(bok) {
                        mDatabase = SQLiteDatabase.openOrCreateDatabase(DATABASE_NAME, null);
                        updateOrCreateCacheTable();
                    }
                } catch (Exception ex) {
                    mErrorMsg = ex.getMessage();
                    ShowErrorBox(mErrorMsg);
                } finally {
                }
            } else {
                Functions.Permission.requestForPermission((Activity) getContext(), getContext());
                ShowErrorBox("Keine Berechtigung um die Datenbank zu öffnen");
            }
        }
    }



    public String accountGeFieldValue(final String key){
        String result = "";
        try {
            AccountManager manager = AccountManager.get(mContext);
            result = manager.getUserData(getAccount(), key);
        }catch (Exception e){
            result = "";
        }
        return result;
    }
    public String accountGeFieldValue(final Account account, final String key){
        String result = "";
        try {
            AccountManager manager = AccountManager.get(mContext);
            result = manager.getUserData(account, key);
        }catch (Exception e){
            result = "";
        }
        return result;
    }

    public boolean hasErrors(){
        String cLog = accountGeFieldValue(getAccount(),ErrorTag);
        return  cLog.length() > 1;
    }

    public void setErrorLog(String msg){
        java.util.Date d = new java.util.Date();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        String cLog = accountGeFieldValue(getAccount(),ErrorTag);
        cLog +=date + " - "+ msg+"<br>";
        accountSeFieldValue(getAccount(), ErrorTag, cLog);
    }


    public boolean hasErrors(Account account){
        String cLog = accountGeFieldValue(account,ErrorTag);
        return  cLog.length() > 1;
    }

    public void setErrorLog(Account account, String msg){

        java.util.Date d = new java.util.Date();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        String cLog = accountGeFieldValue(account,ErrorTag);
        cLog +=date + " - "+ msg+"<br>";
        accountSeFieldValue(account, ErrorTag, cLog);
    }

    /*
    public static List<classSearchObjekt> getItemsFromDb(String searchTerm, itt_CustomAutoCompleteView EditView){

        return  getItemsFromDb(searchTerm, EditView.DBTable,EditView.DBSearchField,EditView.DBSearchField2, EditView.MaxCount);
    }*/
/*
    public static List<classSearchObjekt> getItemsFromDb(String searchTerm, String tableName, String fieldObjectName, String fieldObjectName2, Integer MaxCount){

        List<classSearchObjekt> projectresult = new ArrayList<classSearchObjekt>();
        // select query
        String sql = "";
        sql += "SELECT * FROM " + tableName;
        sql += " WHERE " + fieldObjectName + " LIKE '%" + searchTerm + "%'";
        sql += " OR " + fieldObjectName2 + " LIKE '%" + searchTerm + "%'";
        sql += " ORDER BY " + fieldObjectName + " DESC";
        sql += " LIMIT 0,10";

        if(mDatabase == null)
            loadDatabase();

        try {
            Cursor cursor = mDatabase.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                do {
                    String objectName1 = cursor.getString(cursor.getColumnIndex(fieldObjectName));
                    String objectName2 = cursor.getString(cursor.getColumnIndex(fieldObjectName2));
                    String oid = cursor.getString(cursor.getColumnIndex("oid"));
                    classSearchObjekt xObject = new classSearchObjekt(objectName1,objectName2,oid);
                    projectresult.add(xObject);
                } while (cursor.moveToNext());
            }

            cursor.close();
        }catch (Exception e){

        }
        return projectresult;
    }

    */

    public boolean accountSeFieldValue(Account account,String key,String value){
        boolean result = false;
        try {
            AccountManager manager = AccountManager.get(mContext);
            manager.setUserData(account, key, value);
            result = true;
        }catch (Exception e){
        }
        return result;
    }


    public void accountResetSyncValues(Account account){
        this.accountSeFieldValue(account, DbHelper.sync_EMUM_ErrorTag, "");
        this.accountSeFieldValue(account, DbHelper.sync_EMUM_LastSuccessSync, "");
        this.accountSeFieldValue(account, DbHelper.sync_EMUM_LastSyncInfos, "");
    }


}
