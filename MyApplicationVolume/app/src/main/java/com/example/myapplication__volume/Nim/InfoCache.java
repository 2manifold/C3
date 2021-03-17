package com.example.myapplication__volume.Nim;

import android.content.Context;

import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nimlib.sdk.StatusBarNotificationConfig;

/**
 * Created by jezhee on 2/20/15.
 */
public class InfoCache {

    private static Context context;

    private static String account;

    private static String token;

    private static StatusBarNotificationConfig notificationConfig;

    public static void clear() {
        account = null;
    }

    public static String getAccount() {
        return account;
    }

    public static String getToken() {
        return token;
    }

    private static boolean mainTaskLaunching;

    public static void setAccount(String account) {
        InfoCache.account = account;
        NimUIKit.setAccount(account);
//        AVChatKit.setAccount(account);
//        RTSKit.setAccount(account);
    }

    public static void setToken(String token) {
        InfoCache.token = token;
    }

    public static void setNotificationConfig(StatusBarNotificationConfig notificationConfig) {
        InfoCache.notificationConfig = notificationConfig;
    }

    public static StatusBarNotificationConfig getNotificationConfig() {
        return notificationConfig;
    }

    public static Context getContext() {
        return context;
    }

    public static void setContext(Context context) {
        InfoCache.context = context.getApplicationContext();

//        AVChatKit.setContext(context);
//        RTSKit.setContext(context);
    }

    public static void setMainTaskLaunching(boolean mainTaskLaunching) {
        InfoCache.mainTaskLaunching = mainTaskLaunching;

//        AVChatKit.setMainTaskLaunching(mainTaskLaunching);
    }

    public static boolean isMainTaskLaunching() {
        return mainTaskLaunching;
    }
}