package com.penglab.hi5.core.collaboration;

import android.util.Log;

import com.penglab.hi5.core.MainActivity;
import com.penglab.hi5.core.collaboration.basic.ReconnectionInterface;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static com.penglab.hi5.core.Myapplication.ToastEasy;

public class MsgSender {

    private final String TAG = "MsgSender";

    public MsgSender(){
    }

    public boolean SendMsg(Socket socket, String message, boolean waited, boolean resend, ReconnectionInterface reconnectionInterface){

        final boolean[] flag = {true};
        if (socket == null || !socket.isConnected()){
            ToastEasy("Fail to Send Message, Try Again Please !");
            return false;
        }

        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();

                try {
//                    Log.d(TAG, "Start to Send Message");
                    OutputStream out = socket.getOutputStream();

                    String data = message + "\n";
                    int datalength = data.getBytes(StandardCharsets.UTF_8).length;

                    /*
                    msg header = type + msglength
                     */
                    String header = String.format("DataTypeWithSize:%d;;%s\n",0, datalength);
                    int headerlength = header.getBytes().length;

                    Log.d(TAG,"header: " + header + ",  data: " + data);

                    out.write(header.getBytes(StandardCharsets.UTF_8));
                    out.write(data.getBytes(StandardCharsets.UTF_8));
                    out.flush();


                    /*
                     * show the progressbar
                     */
                    if (message.startsWith("/Imgblock:")){
                        MainActivity.showProgressBar();
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    Log.d(TAG, "Fail to get OutputStream");
                    flag[0] = false;

                    if (resend){
                        reconnectionInterface.onReconnection(message);
                    }
                }

            }
        };

        thread.start();

        try {
            if (waited)
                thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        return flag[0];
    }




    public void SendFile(Socket socket, String filename, InputStream is, long filelength){

        if (!socket.isConnected()){
            ToastEasy("Socket is not Connected, Try Again Please !");
            return;
        }

        Thread thread = new Thread()  {
            public void run(){
                try {

                    Log.d(TAG, "Start to Send File");
                    OutputStream out = socket.getOutputStream();

                    /*
                    file header = type + filename + filelength
                     */
                    String header = String.format("DataTypeWithSize:%d %s %d\n",1, filename, filelength);

                    out.write(header.getBytes());
                    Log.d(TAG, "File length: " + Integer.toString(IOUtils.copy(is, out)));

                    out.flush();
                    is.close();

                }catch (Exception e){
                    e.printStackTrace();
                    ToastEasy("Fail to get OutputStream");
                }
            }
        };
        thread.start();


        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ToastEasy("Send File Successfully !");

    }



    /**
     * Transform bytes[] to long
     * @param bytes the byte[]
     * @return the long of byte[]
     */
    public static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        return buffer.getLong();
    }


    /**
     * Transform long to byte[]
     * @param x the long
     * @return the byte[]
     */
    public byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(x);
        return buffer.array();
    }
}
