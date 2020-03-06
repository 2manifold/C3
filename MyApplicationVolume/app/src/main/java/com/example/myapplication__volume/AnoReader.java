package com.example.myapplication__volume;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import static com.example.myapplication__volume.MainActivity.getContext;

public class AnoReader {
    ArrayList<ArrayList<Float>> apo_result;
    ArrayList<ArrayList<Float>> swc_result;

    Uri apo_uri;
    Uri swc_uri;

    Context context;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;


    public void read(Uri uri){

        //文件头有多少行
        String headstr = "";
        int head_length = headstr.length();

        context = getContext();
        ArrayList<String> arraylist = new ArrayList<String>();


//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//            int hasWriteContactsPermission = context.checkSelfPermission(Manifest.permission.READ_CONTACTS);
//            if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED){
//                context.requestPermissions(new String[]{Manifest.permission.WRITE_CONTACTS},REQUEST_CODE_ASK_PERMISSIONS);
//                return;
//            }

        try{

            ParcelFileDescriptor parcelFileDescriptor =
                    context.getContentResolver().openFileDescriptor(uri, "r");

            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);
            long filesize = (int)parcelFileDescriptor.getStatSize();

            Log.v("AnoReader", Long.toString(filesize));

            FileInputStream fid = (FileInputStream)(is);
            InputStreamReader isr = new InputStreamReader(fid);
            BufferedReader br = new BufferedReader(isr);
            if (filesize < head_length){
                throw new Exception("The size of your input file is too small and is not correct, -- it is too small to contain the legal header.");
            }
            String str;
            while ((str = br.readLine()) != null) {
                arraylist.add(str);
            }
            br.close();
            isr.close();
            if (arraylist.size() < 0){
                throw new Exception("The number of columns is not correct");
            }

            //一共有多少行数据
            int num = arraylist.size();
            Log.v("AnoReader", Integer.toString(num));
            for (int i = 0; i < num; i++){
                String current = arraylist.get(i);
                String [] s = current.split("=");
                ArrayList<Float> cur_line = new ArrayList<Float>();

                Log.v("AnoReader", s[1]);
                String filetype = s[1].substring(s[1].length()-4);
                switch(filetype){
                    case ".apo":
                        apo_uri = getUri(uri, s[1]);
                        Log.v("AnoReader apofilepath", apo_uri.toString());
                        break;
                    case ".swc":
                        swc_uri = getUri(uri, s[1]);
                        break;
                }
            }

        }catch (Exception e){
            Log.v("ReadAnoException", e.getMessage());
        }

    }

    private Uri getUri(Uri uri, String filename){
        String uri_string = uri.toString();
        int index = uri_string.lastIndexOf("/");

        String filepath = uri_string.substring(0, index+1) + filename;

        Uri result = Uri.parse((String) filepath);
        return result;
    }

    public void CallApoReader(Uri uri){

        context.grantUriPermission(context.getPackageName(), uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{
            ParcelFileDescriptor parcelFileDescriptor =
                    context.getContentResolver().openFileDescriptor(uri, "r");
            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);
            long filesize = (int)parcelFileDescriptor.getStatSize();

            ApoReader apoReader = new ApoReader();
            apo_result = apoReader.read(filesize, is);

        }catch (Exception e){
            Log.v("ReadAnoException", e.getMessage());
        }
    }

    public void CallSwcReader(Uri uri){

        context.grantUriPermission(context.getPackageName(), uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try{
            ParcelFileDescriptor parcelFileDescriptor =
                    context.getContentResolver().openFileDescriptor(uri, "r");
            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);
            long filesize = (int)parcelFileDescriptor.getStatSize();

            ApoReader apoReader = new ApoReader();
            swc_result = apoReader.read(filesize, is);

        }catch (Exception e){
            Log.v("ReadAnoException", e.getMessage());
        }
    }

    public Uri getApo_result(){
        return apo_uri;
    }

    public Uri getSwc_result(){
        return swc_uri;
    }

}
