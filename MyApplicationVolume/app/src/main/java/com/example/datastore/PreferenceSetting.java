package com.example.datastore;

import android.content.Context;
import android.content.SharedPreferences;


/**
 * Manager the pref in settings
 * three prefs: downsample mode;  check mode;  contrast
 */

public class PreferenceSetting {

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private Context mContext;

    public PreferenceSetting(Context context){
        mContext = context;
        pref = mContext.getSharedPreferences("settings",Context.MODE_PRIVATE);
    }

    public void setPref(boolean DownSampleMode, boolean CheckMode, int Contrast){
        editor = pref.edit();
        editor.putBoolean("DownSampleMode",DownSampleMode);
        editor.putBoolean("CheckMode",CheckMode);
        editor.putInt("Contrast",Contrast);
        editor.apply();

    }

    public boolean getDownSampleMode(){
        return pref.getBoolean("DownSampleMode",true);
    }

    public boolean getCheckMode(){
        return pref.getBoolean("CheckMode",false);
    }

    public int getContrast(){
        return pref.getInt("Contrast",0);
    }


}