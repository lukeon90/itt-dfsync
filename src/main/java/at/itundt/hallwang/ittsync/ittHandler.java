package at.itundt.hallwang.ittsync;

    import android.accounts.Account;
    import android.accounts.AccountManager;
    import android.app.Activity;
    import android.app.Dialog;
    import android.content.*;
    import android.os.Bundle;
    import android.os.Handler;
    import android.os.Message;
    import android.support.v4.widget.SwipeRefreshLayout;
    import android.support.v7.app.AppCompatActivity;
    import android.text.Html;
    import android.widget.TextView;
    import org.json.JSONObject;

    import java.io.ByteArrayInputStream;
    import java.io.File;
    import java.util.UUID;

public class ittHandler {

    //public static Context mContext;
    private  android.accounts.Account[] Accounts;
    private  AccountManager Manager;
    //public static Activity ShownActivity;

    //Private Member
    private  Account ActiveAccount = null;
    private  String threadid;

    private Context mContext;
    private DbHelper mConnection;
    private String mAuthority;

    public ittHandler(Context context, boolean init) {
        mContext   = context;

        if(context == null)
            return;

        mAuthority = mContext.getPackageName() + ".provider";

        if (init) {
            initResources(context);
        }
    }

    public Context getContext() {
        return mContext;
    }
    public String getAuthority() {
        return mAuthority;
    }

    public DbHelper getConnection() {
        return mConnection;
    }

    public void executeSync(Activity activity, BroadcastReceiver itt_SyncReceiver) {
        if(getActiveAccount() == null) {
            ShowErrorBox("Keine Syncaccount konfiguriert!!");
            return;
        }

        if(activity != null && itt_SyncReceiver != null)
            activity.registerReceiver(itt_SyncReceiver, new IntentFilter("account.finished"));

        Bundle settings = new Bundle();
        settings.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getActiveAccount(),getAuthority(), settings);
    }


    public void setActiveContext(Context context) {
        if (context == null)
            return;

        getConnection().setContext(context);
        mContext = context;
    }

    public void initResources(Context context) {
        try {
            Manager = AccountManager.get(context);
            Accounts = Manager.getAccountsByType(Functions.ACCOUNT_TYPE);
            mConnection = new DbHelper(context);
            System.Global.setGlobalHandler(this);

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());

        }
    }

    public void ShowErrorBox(String Message) {
        if(getContext() != null)
            ShowErrorBox(Message,getContext());
    }

    public void ShowErrorBox(String Message, Context context) {
        if(context != null) {
            final Dialog dialog = new Dialog(context); // Context, this, etc.
            dialog.setContentView(R.layout.dialog_box);
            TextView msg = (TextView)dialog.findViewById(R.id.dialog_info);
            msg.setText(Html.fromHtml(Message));
            dialog.setTitle("ERROR OCCURED");
            dialog.show();

        }
    }

    public boolean showSettingDlg(boolean skip){

        boolean result = false;
        if(this.getConnection().isDatabaseOpen()){
            int i = this.getAccounts().length;
            if(i > 0) {
                if (this.getActiveAccount() == null) {
                    this.setActiveAccount(this.getAccounts()[0]);
                }
            }
            if(!skip || i == 0) {
                Intent myIntent = new Intent(getContext(), at.itundt.hallwang.ittsync.accountActivity.class);
                getContext().startActivity(myIntent);
                result = true;
            }


        }
        return  result;
    }

    public  void showSettingDlg(Context context){
        Intent myIntent = new Intent(context,at.itundt.hallwang.ittsync.accountActivity.class);
        context.startActivity(myIntent);
    }

    public Account[] getAccounts() {
        if(Manager == null)
            return Accounts;

        Accounts = Manager.getAccountsByType(Functions.ACCOUNT_TYPE);
        return  Accounts;
    }


    public void setFirstAccount() {
        setFirstAccount(true);
        return;
    }

    public void setFirstAccount(boolean showbox) {
        if(Manager == null)
            return;
        Accounts = Manager.getAccountsByType(Functions.ACCOUNT_TYPE);
        if(Accounts.length > 0) {
            setActiveAccount(Accounts[0]);
        }else{
            if(showbox) {
                ShowErrorBox("Kein Account konfiguriert!!");
            }
        }
        return;
    }

    public String getActiveAccountAppKey() {
        if(getConnection() == null) {
            ShowErrorBox("Keine aktive Datenbank geöffnet");
            return "";
        }
        return  getConnection().accountGeFieldValue("AppKey");
    }

    public void setActiveAccount(Account account) {
        if(getConnection() == null)
            return;
        getConnection().setAccount(account);
        ActiveAccount = account;
    }

    public int getJsonState(JSONObject data) {
        int result = Functions.jsonGetInt(data, "State");
        if (result == 0)
            result = Functions.i_EMPTY_DATA;
        return result;
    }

    public boolean itt_RunAsyncDfRequest(Object Input, Handler handlerThread, int handlerCorrelation) {

        return itt_RunAsyncDfRequest(Input, handlerThread, handlerCorrelation, "", null);
    }

    public boolean itt_RunAsyncDfRequest(Object Input, Handler handlerThread, int handlerCorrelation, String threadID, SwipeRefreshLayout swipeView) {
        threadid = threadID;
        try {
            if (handlerThread == null) return false;
            if (swipeView != null) if (!swipeView.isRefreshing()) swipeView.setRefreshing(true);


        } catch (Exception e) {
            return false;
        }

        final Thread thread = new Thread(new ittThread(this, Input, handlerThread, handlerCorrelation) {
            @Override
            public void run() {
                synchronized (this) {
                    try {
                        if (this.getHandler() != null) {
                            switch (this.getCorrelation()) {

                                case Functions.THREAD_METHOD_CHECKCONNECTION:

                                    try {
                                        if (this.getData() instanceof ittConObj) {
                                            ittConObj con = (ittConObj) this.getData();
                                        this.getHandler().sendMessage(Message.obtain(this.getHandler(), Functions.THREAD_METHOD_CHECKCONNECTION,
                                                makeHTTPRequest_CheckConnection(con)));

                                        }
                                    } catch (Exception e) {
                                    }

                                    break;
                                case Functions.THREAD_METHOD_CHECK:
                                    try {
                                        this.getHandler().sendMessage(Message.obtain(this.getHandler(), Functions.THREAD_METHOD_CHECK,
                                                makeHTTPRequest_CheckAccount()));
                                    } catch (Exception e) {
                                    }
                                    break;
                                case Functions.THREAD_METHOD_SYNC:
                                    try {
                                        if (this.getData() instanceof Account) {
                                            Account account = (Account) this.getData();
                                            if (account != null) {
                                                Bundle settings = new Bundle();
                                                settings.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                                                ContentResolver.requestSync(account, mAuthority, settings);
                                            }
                                        }
                                    } catch (Exception e) {
                                    }
                                    break;
                                default:
                                    try {
                                        if (this.getData() instanceof JSONObject) {
                                            JSONObject jsonData = (JSONObject) this.getData();
                                            this.getHandler().sendMessage(Message.obtain(this.getHandler(), this.getCorrelation(),
                                                    Functions.makeDfRequest(this.getConnction(),jsonData, true,true)));
                                        }else{
                                            this.getHandler().sendMessage(Message.obtain(this.getHandler(), this.getCorrelation(),
                                                    Functions.makeDfRequest(this.getConnction(),new JSONObject(), true,true)));

                                        }

                                    } catch (Exception e) {
                                    }
                                    break;
                            }

                        }
                    } catch (Exception e) {
                    }
                }
            }
        });
        thread.start();
        return true;
    }


    public void itt_RunAsnc_dfSendRequest(File file, ittAsyncHttpResponseHandler handler) {
        try{
            itt_RunAsnc_dfSendRequest(file,file.getName(),handler);
        }catch (Exception e){
            ShowErrorBox("Uploadfehler bei itt_RunAsnc_dfupload "+e.getMessage());
        }
    }

    public void itt_RunAsnc_dfSendRequest(File file, String filename, ittAsyncHttpResponseHandler handler){

        JSONObject json = new JSONObject();
        String uniqueString = UUID.randomUUID().toString();
        String extension = filename.substring(filename.lastIndexOf("."));

        try {
            json.put("Action", "UploadBinary");
            json.put("AppKey", this.getActiveAccountAppKey());
            json.put("BinaryID",uniqueString + extension);
            json.put("Token", "123");

            ittAsyncHttpClient client = new ittAsyncHttpClient(this);
            client.setShowNotf(true);
            client.putJson(json, uniqueString+".json");
            client.putBinary(file, uniqueString + extension);
            client.runRequestAsync(handler);

        }catch (Exception e) {
        }
    }


    public void itt_RunAsnc_dfSendRequest(byte[] binary, String filename, ittAsyncHttpResponseHandler handler){

        JSONObject json = new JSONObject();
        String uniqueString = UUID.randomUUID().toString();
        String extension = filename.substring(filename.lastIndexOf("."));

        try {
            json.put("Action", "UploadBinary");
            json.put("AppKey", this.getActiveAccountAppKey());
            json.put("BinaryID",uniqueString + extension);
            json.put("Token", "123");

            ittAsyncHttpClient client = new ittAsyncHttpClient(this);
            client.setShowNotf(true);
            client.putJson(json, uniqueString+".json");
            client.putBinary( new ByteArrayInputStream(binary), uniqueString + extension);
            client.runRequestAsync(handler);

        }catch (Exception e) {
        }
    }


    public void itt_RunAsnc_dfSendRequest(JSONObject request, ittAsyncHttpResponseHandler handler) {

        String uniqueString = UUID.randomUUID().toString();
        try {
            request.put("Action", "UploadBinary");
            ittAsyncHttpClient client = new ittAsyncHttpClient(this);
            client.setShowNotf(true);
            client.putJson(request, uniqueString);
            client.runRequestAsync(handler);

        }catch (Exception e) {
        }

    }


    public boolean itt_RunAsync_dfSync(Handler handlerThread, SwipeRefreshLayout swipeView) {

        Account account = getActiveAccount();
        if (account == null || handlerThread == null)
            return false;

        return itt_RunAsyncDfRequest(account, handlerThread, Functions.THREAD_METHOD_SYNC, "", swipeView);
    }


    public boolean itt_RunAsync_dfCheckConnection(Handler handlerThread, String hostname, String descr) {
        return itt_RunAsyncDfRequest(new ittConObj(hostname, descr), handlerThread, Functions.THREAD_METHOD_CHECKCONNECTION, "", null);
    }

    public String itt_checkJsonDefaultMsg(Message msg, int state, Context context) {

        String cResult = "";
        if (context instanceof AppCompatActivity) {
            try {
                AppCompatActivity activity = (AppCompatActivity) context;
                activity.getSupportActionBar().setDisplayShowHomeEnabled(false);
            } catch (Exception e) {

            }
        }

        switch (state) {
            case Functions.i_DEVICE_DEACTIVATED:

                cResult = "Gerät ist deaktiviert";
                /*
                if(context.getClass() !=  DeactivatedActivity.class){
                    Intent myIntent = new Intent(context,DeactivatedActivity.class);
                    context.startActivity(myIntent);
                }*/

                break;
            case Functions.i_UNKNOWN:
                if (context instanceof AppCompatActivity) {
                    try {
                        AppCompatActivity activity = (AppCompatActivity) context;
                        activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
                        activity.getSupportActionBar().setIcon(R.drawable.warning32);
                    } catch (Exception e) {
                    }
                }
                cResult = "Internetverbindung prüfen";
                break;
            case Functions.i_HTTP_TIMEOUT:
                if (context instanceof AppCompatActivity) {
                    try {
                        AppCompatActivity activity = (AppCompatActivity) context;
                        activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
                        activity.getSupportActionBar().setIcon(R.drawable.warning32);
                    } catch (Exception e) {

                    }
                }
                cResult = "TIMEOUT vom Sever.";
                break;
            default:
                break;
        }

        return cResult;

    }

    public Account getActiveAccount() {

        try {
            Accounts = Manager.getAccountsByType(Functions.ACCOUNT_TYPE);

            if (ActiveAccount == null) {
                if (Accounts != null) {
                    if (Accounts.length == 1)
                        ActiveAccount = Accounts[0];
                }
            } else if (Accounts.length == 0) {
                ActiveAccount = null;
            }
        } catch (Exception e) {

        }

        return ActiveAccount;
    }

    public String getActivHost() {
        Account account = getActiveAccount();

        if(account == null) {
            ShowErrorBox("Kein Sync-Account konfiguriert.");
        }

        String result = "";
        if (account != null) {
            result = getConnection().accountGeFieldValue(account, "Host");
        }
        return result;
    }

    // fristenbuch sekr. recht form  232 gmbh
    //Only Check - No Result
    public JSONObject makeHTTPRequest_CheckAccount() {

        JSONObject jsonData = new JSONObject();
        Exception ex = null;
        try {
            jsonData.put("AppKey", getConnection().accountGeFieldValue(this.getActiveAccount(), "AppKey"));
            jsonData.put("Action", "CheckAccount");
            jsonData.put("Token", "B3ECFD39-03EE-4A07-9F83-EF58BD7889F");
            jsonData = Functions.makeDfRequest(this, jsonData, true,false);

        } catch (Exception e) {
            getConnection().dbWriteErrorLog("getTaskDataObj", e.getMessage());
        }
        return jsonData;
    }


    public JSONObject makeHTTPRequest_CheckConnection(ittConObj obj) {
        return makeHTTPRequest_CheckConnection(obj.Hostname, obj.getDescription());
    }

    public JSONObject makeHTTPRequest_CheckConnection(String Host, String DeviceDescription) {

        JSONObject jsonData = new JSONObject();
        Exception ex = null;
        try {
            jsonData.put("AppKey",Functions.md5_Hash(Host+DeviceDescription));
            jsonData.put("Description",DeviceDescription);
            jsonData.put("Action", "CheckAccount");
            jsonData.put("Token", "B3ECFD39-03EE-4A07-9F83-EF58BD7889F");
            jsonData = Functions.makeDfRequest(this, jsonData, true,Host,false);

        } catch (Exception e) {
            getConnection().dbWriteErrorLog("getTaskDataObj", e.getMessage());
        }
        return jsonData;
    }

    public  class ittConObj{

        private String Hostname;
        private String Description;

        public  ittConObj(String mHostname, String mDescription){
            Hostname = mHostname;
            Description = mDescription;
        }
        public String getHostname() {
            return Hostname;
        }
        public String getDescription() {
            return Description;
        }
    }

    public class ittThread implements Runnable {
        private Object data;
        private byte[] binary;
        private Handler threadHandler;
        private int threadCorrelation;
        private ittHandler connection;

        public ittThread(ittHandler _connection, Object _data, Handler _handler, int _correlation) {
            this.data = _data;
            this.threadHandler = _handler;
            this.threadCorrelation = _correlation;
            this.connection = _connection;
        }

        @Override
        public void run() {

        }
        public void setBinary(byte[] data){
            this.binary = data;
        }

        public byte[] getBinary(){

            return binary;

        }

        public Object getData() {
            return data;
        }

        public Handler getHandler() {
            return threadHandler;
        }

        public int getCorrelation() {
            return threadCorrelation;
        }
        public ittHandler getConnction() {
            return connection;
        }

    }

}
