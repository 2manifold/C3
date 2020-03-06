package com.example.myapplication__volume;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Exception;
import java.util.ArrayList;

import static com.example.myapplication__volume.MainActivity.getContext;

public class ApoReader {

    //读写权限
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static int REQUEST_PERMISSION_CODE = 1;

    ArrayList<ArrayList<Float>> read(Uri uri){

        Context context = getContext();
//        context.grantUriPermission(context.getPackageName(), uri,
//                Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        //文件头有多少行
        String headstr = " ";
        int head_length = headstr.length();

        ArrayList<ArrayList<Float>> result = new ArrayList<ArrayList<Float>>();
        ArrayList<String> arraylist = new ArrayList<String>();
        try{
            ParcelFileDescriptor parcelFileDescriptor =
                    context.getContentResolver().openFileDescriptor(uri, "r");

            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);
            long filesize = (int)parcelFileDescriptor.getStatSize();

            Log.v("SwcReader", Long.toString(filesize));



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
//            float [][] result = new float[num][11];
            for (int i = 0; i < num; i++){
                String current = arraylist.get(i);
                String [] s = current.split(",");
                ArrayList<Float> cur_line = new ArrayList<Float>();
                for (int j = 0; j < 12; j++){
                    String cur_string = s[j].replace(" ","");
                    if (cur_string.equals(""))
                        cur_string = "1234";
                    cur_line.add(Float.parseFloat(cur_string));
//                    cur_line.add(Float.parseFloat("111"));
//                    result[i][j] = Float.parseFloat(s[j]);
                }
                result.add(cur_line);
            }
            return result;
        }catch (Exception e){
            Log.v("ReadApoException", e.getMessage());
            return null;
        }
    }



    ArrayList<ArrayList<Float>> read(long length, InputStream is){

        //文件头有多少行
//        String headstr = "#name vaa3d_traced_neuron\n#comment \n##n,type,x,y,z,radius,parent,seg_id,level,mode,timestamp,feature_value\n";
        String headstr = " ";
        int head_length = headstr.length();
        long filesize = length;

        ArrayList<ArrayList<Float>> result = new ArrayList<ArrayList<Float>>();
        ArrayList<String> arraylist = new ArrayList<String>();
        try{

            Log.v("ApoReader", Long.toString(filesize));
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
//            float [][] result = new float[num][11];
            for (int i = 0; i < num; i++){
                String current = arraylist.get(i);
                String [] s = current.split(",");
                ArrayList<Float> cur_line = new ArrayList<Float>();
                for (int j = 0; j < 12; j++){
                    String cur_string = s[j].replace(" ","");
                    if (cur_string.equals(""))
                        cur_string = "1234";
                    cur_line.add(Float.parseFloat(cur_string));
//                    cur_line.add(Float.parseFloat("111"));
//                    result[i][j] = Float.parseFloat(s[j]);
                }
                result.add(cur_line);
            }
            return result;
//            byte [] by = new byte[head_length];
//            long preread = fid.read(by);
//            if (preread != head_length){
//                throw new Exception("File unrecognized or corrupted file.");
//            }
//            String prestr = new String(by);
//            if (!prestr.equals(headstr)){
//                throw new Exception("Unrecognized file format.");
//            }

        }catch (Exception e){
            Log.v("Exception of ReadApo", e.getMessage());
            return null;
        }
    }


}
