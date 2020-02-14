package at.itundt.hallwang.ittsync;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.gson.Gson;
import org.json.JSONObject;
import android.app.AlertDialog;

import java.util.ArrayList;
import java.util.function.Function;

public class accountActivity extends AppCompatActivity {

    private ittHandler mHandler;
    private Context mContext;
    private String provider0;
    private String provider1;
    private Button btnConnect;
    private String mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(at.itundt.hallwang.ittsync.R.layout.account_layout);

        if(System.Global.getGlobalHandler() == null)
            System.Global.setGlobalHandler(new ittHandler(this,true));


        mHandler = System.Global.getGlobalHandler();
        mContext = this;
        provider0 = getString(R.string.app_provider);
        provider1 = mHandler.getAuthority();
        mHandler.getContext().getApplicationInfo();
        btnConnect = findViewById(R.id.account_button_connect);
        btnConnect.setEnabled(false);
        setTitle("Überprüfe Provider...");

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (!provider0.equals(provider1)) {
                    setTitle("Provider ungültig..");
                    String txt = "Achtung: Provider wurde nicht erkannt! Bitte erstellen sie in ihrer App eine 'String' " +
                            "Ressource mit folgendem Wert:\n\n<string name=\"app_provider\">" + provider1 + "</string>\n\n damit ihre App mit der Bibliothek synchroniseren kann.";
                    mHandler.ShowErrorBox(txt, mContext);
                    btnConnect.setEnabled(false);
                } else {
                    setTitle("Account hinzufügen");
                    btnConnect.setEnabled(true);
                }

            }
        }, 2000);


        btnConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String cHost = ((TextView) findViewById(R.id.account_txt_server)).getText().toString();
                String cPort = ((TextView) findViewById(R.id.account_txt_port)).getText().toString();
                String cBeschreibung = ((TextView) findViewById(R.id.account_txt_description)).getText().toString();
                boolean isSSL = ((CheckBox) findViewById(R.id.account_cb_ssl)).isChecked();
                cHost = cHost.trim();

                if (isSSL) {
                    cHost = "https://" + cHost + ":" + cPort;
                    cHost = cHost.trim();
                } else {
                    cHost = "http://" + cHost + ":" + cPort;
                    cHost = cHost.trim();
                }

                Account mAccount = Functions.AccountCfg.getAccountbyHost(mContext, cHost);
                setTitle("Verbinde zum Server..");
                btnConnect.setEnabled(false);
                mHandler.itt_RunAsync_dfCheckConnection(ThreadHandler, cHost, cBeschreibung);



            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();


    }

    private void ConnectionSuccess() {

        String cHost = ((TextView) findViewById(R.id.account_txt_server)).getText().toString();
        String cPort = ((TextView) findViewById(R.id.account_txt_port)).getText().toString();
        String cBeschreibung = ((TextView) findViewById(R.id.account_txt_description)).getText().toString();
        boolean isSSL = ((CheckBox) findViewById(R.id.account_cb_ssl)).isChecked();
        cHost = cHost.trim();
        if (isSSL) {
            cHost = "https://" + cHost + ":" + cPort;
            cHost = cHost.trim();
        } else {
            cHost = "http://" + cHost + ":" + cPort;
            cHost = cHost.trim();
        }

        Account acc = Functions.AccountCfg.CreateSyncAccount(mContext,cHost,cBeschreibung,mDatabase, mHandler.getAuthority());

        if (acc != null) {
            setTitle("Verbindung erfolgreich");
            Button Connect = findViewById(R.id.account_button_connect);
            Connect.setEnabled(false);
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Glückwunsch!!");
            alertDialog.setMessage("Verbindung zum Server war erfolgreich");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    });
            alertDialog.show();
        }

    }

    private Handler ThreadHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            try {
                switch(msg.what) {
                    case Functions.THREAD_METHOD_CHECKCONNECTION:
                        JSONObject json = (JSONObject) msg.obj;
                        int result      = Functions.getJsonState(json);
                        mDatabase       =  Functions.jsonGetString(json,"Database");

                        switch (result) {
                            case Functions.i_VALID_METHOD:
                                ConnectionSuccess();
                                break;
                            case Functions.i_DEVICE_DEACTIVATED:
                                ConnectionSuccess();
                                break;
                            default:
                                setTitle("Connection refused..");
                                mHandler.ShowErrorBox("Keine Verbindung zum Server.",mContext);
                                break;
                        }
                }
            }catch (Exception e){
                mHandler.ShowErrorBox(e.getMessage(),mContext);
                return  false;
            }
            btnConnect.setEnabled(true);
            return true;
        }
    });

}
