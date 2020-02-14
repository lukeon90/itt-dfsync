package at.itundt.hallwang.ittsync;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import at.itundt.hallwang.ittsync.R;

import java.util.Random;

public class ittNotification {

    private  String mChannelID = "";
    private  String mChannelName = "";
    private  Context mContext;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private int id;


    public ittNotification(Context context,String channelName, String title, String text){
        initBuilder(context,channelName,title,text);
    }

    public void init() {
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    mChannelID,
                    mChannelName,
                    NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(mChannelID);
        }
        mNotificationManager.notify(id, mBuilder.build());

    }

    private  void initBuilder(Context context,String channelName, String title, String text){
        mContext = context;
        mChannelID = "ittsync_"+channelName;
        mChannelName = channelName;
        Random r = new Random();
        id = r.nextInt(45 - 28) + 28;
        mBuilder = new NotificationCompat.Builder(mContext.getApplicationContext(), mChannelID);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        mBuilder.setContentTitle(title);
        mBuilder.setOngoing(false);
        mBuilder.setOnlyAlertOnce(true);
        mBuilder.setContentText(text);
    }

    public  void updateProgressNotification(String title, String text,int percent){
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(text);
        mBuilder.setProgress(100,percent,false);
        mNotificationManager.notify(id, mBuilder.build());
    }

    public  void notifyShow(){
        mNotificationManager.notify(id, mBuilder.build());
    }



    public void cancelNotifcation(){
        mNotificationManager.cancel(id);

    }

    public void cancelNotifcation(int time){

        if(time > 1000) {
            Handler h = new Handler();
            long delayInMilliseconds = time;
            h.postDelayed(new Runnable() {
                public void run() {
                    mNotificationManager.cancel(id);
                }
            }, delayInMilliseconds);
        }


    }

    public void cancelNotifcationAll(){
        mNotificationManager.cancelAll();
    }

    public NotificationCompat.Builder getBuilder() {
        return mBuilder;
    }
}
