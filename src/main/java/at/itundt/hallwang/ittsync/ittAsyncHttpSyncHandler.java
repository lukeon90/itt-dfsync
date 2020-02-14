package at.itundt.hallwang.ittsync;

import java.util.logging.Handler;
import at.itundt.hallwang.ittsync.ittHandler.*;

public abstract class ittAsyncHttpSyncHandler extends Handler {

    private ittThread  mThread;
    public  ittAsyncHttpSyncHandler(ittThread thread ){
        mThread = thread;
    }

    public  abstract  void  OnFinished(Object data);


}
