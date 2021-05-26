package com.penglab.hi5.core.collaboration.service;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.penglab.hi5.core.collaboration.connector.MsgConnector;

public class CollaborationService extends BasicService {

    protected static final String TAG = "CollaborationService";

    @Override
    public void init() {

        super.TAG = TAG;
        HEART_BEAT_RATE = 5 * 60 * 1000;
        mBasicConnector = MsgConnector.getInstance();
        mReadThread = new ReadThread(mBasicConnector.getSocket());
        mReadThread.start();

    }


    @Override
    public void onCreate() {
        init();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        super.onUnbind(intent);
        if (mReadThread != null)
            mReadThread.needStop = true;
        return mAllowRebind;
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }



}
