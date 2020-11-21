package com.example.server_communicator;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.example.ImageReader.BigImgReader;
import com.example.datastore.ExcelUtil;
import com.example.myapplication__volume.GameActivity;
import com.example.myapplication__volume.MainActivity;
import com.example.myapplication__volume.R;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.interfaces.OnSelectListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.carbs.android.library.MDDialog;

import static com.example.datastore.SettingFileManager.getArborNum;
import static com.example.datastore.SettingFileManager.getArbor_List__Check;
import static com.example.datastore.SettingFileManager.getBoundingBox;
import static com.example.datastore.SettingFileManager.getFilename_Remote;
import static com.example.datastore.SettingFileManager.getFilename_Remote_Check;
import static com.example.datastore.SettingFileManager.getNeuronNumber_Remote;
import static com.example.datastore.SettingFileManager.getRES;
import static com.example.datastore.SettingFileManager.getUserAccount_Check;
import static com.example.datastore.SettingFileManager.getoffset_Remote;
import static com.example.datastore.SettingFileManager.getoffset_Remote_Check;
import static com.example.datastore.SettingFileManager.setArborNum;
import static com.example.datastore.SettingFileManager.setBoundingBox;
import static com.example.datastore.SettingFileManager.setFilename_Remote;
import static com.example.datastore.SettingFileManager.setFilename_Remote_Check;
import static com.example.datastore.SettingFileManager.setNeuronNumber_Remote;
import static com.example.datastore.SettingFileManager.setRES;
import static com.example.datastore.SettingFileManager.setoffset_Remote;
import static com.example.datastore.SettingFileManager.setoffset_Remote_Check;

public class Remote_Socket extends Socket {

    private Context mContext;
    private Activity mActivity;
    public String ip;
    public int id;
    public Socket ManageSocket = null;
    public PrintWriter mPWriter;  //PrinterWriter  用于接收消息
    public BufferedReader ImgReader;//BufferedWriter 用于推送消息
    public PrintWriter ImgPWriter;  //PrinterWriter  用于接收消息

    public boolean isSocketSet;
    public String Store_path;
    public volatile boolean flag;

    private Socket_Send socket_send;
    private Socket_Receive socket_receive;

    private static String SOCKET_CLOSED = "socket is closed or fail to connect";
    private static String EMPTY_MSG = "the msg is empty";
    private static String EMPTY_FILE_PATH = "the file path is empty";

    public static final String TAG = "Remote_Socket";
    public static String ArborNumber_Selected = "Empty";
    public static String BrainNumber_Selected = "Empty";
    public static Vector<String> RES_List = new Vector<>();
    public static Vector<String> Arbor_Check_List = new Vector<>();
    public static Vector<String> Neuron_Number_List = new Vector<>();
    public static HashMap<String, Vector<String>> Neuron_Info = new HashMap<>();
    public static Vector<String> Soma_List = new Vector<>();
    public static ArrayList<Integer> marker_List = new ArrayList<Integer>();
    public static String RES_Selected = "Empty";
    public static String Neuron_Number_Selected = "Empty";
    public static String Pos_Selected = "Empty";
    public static String Offset_Selected = "Empty";
    public static boolean isDrawMode = true;

    public static HashMap<String, Vector<String>> Neuron_Arbor_checked = new HashMap<>();
    public static HashMap<String, String> Neuron_checked = new HashMap<>();


    public Remote_Socket(Context context){

        isSocketSet = false;
        mContext = context;
        mActivity = getActivity(mContext);
        socket_send = new Socket_Send(context);
        socket_receive = new Socket_Receive(context);

        Store_path = context.getExternalFilesDir(null).toString();

        if (!getFilename_Remote(mContext).equals("--11--")){
            BrainNumber_Selected = getFilename_Remote(mContext).split("/")[0];
        }

    }


    /**
     * connect with server
     * @param ip_server
     */
    public void connectServer(String ip_server){

        Log.e("connectServer","Start to Connect Server !");

        /*
        如果已经和服务器建立连接了，就返回
         */
        if (ManageSocket != null && !ManageSocket.isClosed() && ManageSocket.isConnected()){
            return;
        }

        //新建一个线程，用于初始化socket和检测是否有接收到新的消息
        Thread thread = new Thread() {
            @Override
            public void run() {

                try {
                    ip = ip_server;
                    Log.e(TAG,"ip_server: "+ ip_server);
                    ManageSocket = new Socket(ip_server, Integer.parseInt("9000"));             // 服务器的ip和端口号
//                    ManageSocket = new Socket(ip_server, Integer.parseInt("9100"));
//                    ManageSocket = new Socket("192.168.1.120", Integer.parseInt("8000"));
//                    ManageSocket = new Socket("192.168.1.120", Integer.parseInt("8000"));
                    ImgReader = new BufferedReader(new InputStreamReader(ManageSocket.getInputStream(), "UTF-8"));
                    ImgPWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(ManageSocket.getOutputStream(), StandardCharsets.UTF_8)));

                    /*
                    判断是否成功建立连接
                     */
                    if (ManageSocket.isConnected()) {
                        isSocketSet = true;
                        Log.e("connectServer", "Connect Server Successfully !");
                    } else {
                        Toast_in_Thread("Can't Connect Server, Try Again Please !");
                    }


                } catch (IOException e) {
                    Toast_in_Thread("Something Wrong When Connect Server");
                    e.printStackTrace();
                }

            }
        };
        thread.start();


        /*
        用于暂停主线程，等待子线程执行完成
         */
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    /**
     * process the message received from the server
     * @param msg
     */
    public void processMsg(String msg){

        String LoginRex = ":log in success.";
        String LogoutRex = ":log out success.";
        String ImportRex = ":import port.";
        String CurrentDirDownExp = ":currentDir_down.";
        String CurrentDirLoadExp = ":currentDir_load.";
        String CurrentDirImgDownExp = ":currentDirImg";
        String CurrentDirArborDownExp = ":currentDirArbor";
        String MessagePortExp = ":messageport.\n";
        String ImportportExp = ":importport.\n";


        String fileListPattern = "(.*);BRAINS;(.*)";
        String neuronListPattern = "List:(.*)";


        Log.e("processMsg", "msg: " + msg);

        if (!msg.equals("")){
            if (Pattern.matches(fileListPattern, msg)){

                Pattern pattern = Pattern.compile(fileListPattern);
                Matcher matcher = pattern.matcher(msg);
                if (matcher.find()){
//                    String file_name = matcher.group(3);
                    String[] file_list = matcher.group(3).split("_");
                    for (int i = 0; i < file_list.length; i++)
                        Log.e("onReadyRead", file_list[i]);

                    /*
                    handle the file list
                     */
                    showListDialog(file_list);
                }
            }else if (Pattern.matches(neuronListPattern, msg)){
                Pattern pattern = Pattern.compile(neuronListPattern);
                Matcher matcher = pattern.matcher(msg);
                if (matcher.find()){
//                    String file_name = matcher.group(3);
                    String[] neuron_list = matcher.group(2).split("/");
                    for (int i = 0; i < neuron_list.length; i++)
                        Log.e("onReadyRead", neuron_list[i]);

                    /*
                    handle the file list
                     */
                    showListDialog(neuron_list);
                }
            }

        }

//        if(msg != null){
//
//            if (msg.contains(LoginRex)){
//
//                Toast.makeText(mContext, "login successfully.", Toast.LENGTH_SHORT).show();
//            }else if (msg.contains(LogoutRex)){
//
//                Toast.makeText(mContext, "logout successfully.", Toast.LENGTH_SHORT).show();
//
//            }else if (msg.contains(ImportRex)){
//
//                if (!ManageSocket.isConnected()){
//
//                    Toast.makeText(mContext, "can not connect with Manageserver.", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                /**
//                 *  something
//                 */
//            }else if (msg.contains(CurrentDirDownExp)){
//                Log.e("onReadyRead", "CurrentDirDownExp  here we are");
//                String [] file_string = msg.split(":");
//                String [] file_list = file_string[0].split(";");
//
//                for (int i = 0; i < file_list.length; i++)
//                    Log.e("onReadyRead", file_list[i]);
//
//                showListDialog(mContext, file_list, "CurrentDirDownExp");
//
//            }else if (msg.contains(CurrentDirLoadExp)){
//                String [] file_string = msg.split(":");
//                String [] file_list = file_string[0].split(";");
//
//                for (int i = 0; i < file_list.length; i++)
//                    Log.e("onReadyRead", file_list[i]);
//
//                showListDialog(mContext, file_list, "CurrentDirLoadExp");
//            }else if (msg.contains(CurrentDirImgDownExp)) {
//                String[] file_string = msg.split(":");
//                String[] file_list = file_string[0].split(";");
//
////                for (int i = 0; i < file_list.length; i++)
////                    Log.e("onReadyRead", file_list[i]);
//
//                showListDialog(file_list);
//            }else if (msg.contains(CurrentDirArborDownExp)){
//                String[] file_string = msg.split(":");
//                String[] file_list = file_string[0].split(";");
//
//                for (int i = 0; i < file_list.length; i++){
////                    Log.e("onReadyRead", file_list[i]);
//                    Arbor_Check_List.add(file_list[i]);
//                }
//
//                setArbor_List_Check(file_list,mContext);
//                Vector<String> adjust_str = Adjust_Index_Check();
//                showListDialog_Check(Transform(adjust_str, 0, adjust_str.size()));
//            }
//        }



    }



    // --------------------------------------------   TEST   -------------------------------------------------


    public void setIsDrawMode(boolean isDrawMode){
        Log.e("setIsDrawMode"," ---- Start ----");
        this.isDrawMode = isDrawMode;
    }

    public void brainTest(){

        /*
        return: 0;BRAINS;18454
         */

        Log.e("brainTest"," ---- Start ----");
        this.isDrawMode = true;
//        isDrawMode = false;

        if (this.isDrawMode){
            sendMessage("0" + ":choose3.\n");
        }else {
            sendMessage("1" + ":choose3.\n");
        }

        String Msg = getMessage();
        if (Msg == null){
            Toast_in_Thread("Socket disconnect When Select_Brain !");
            return;
        }
        if (Msg.equals(EMPTY_MSG)){
            Toast_in_Thread("Fail to read the Brain List !");
            return;
        }

        Log.e("brainTest","Msg: " + Msg);
    }




    public void neuronListTest(){

        /*
        return: 0:List:18454_00001;0;4548_14718_5466
         */

        Log.e("neuronListTest"," ---- Start ----");
        this.isDrawMode = true;
//        isDrawMode = false;

        String neuron_num = "18454";
        if (this.isDrawMode){
            sendMessage(neuron_num + ";1;0" + ":BrainNumber.\n");
        }else {
            sendMessage(neuron_num + ";1;1" + ":BrainNumber.\n");
        }

        String Msg = getMessage();
        if (Msg == null){
            Toast_in_Thread("Socket disconnect When Select_Brain !");
            return;
        }
        if (Msg.equals(EMPTY_MSG)){
            Toast_in_Thread("Fail to read the Brain List !");
            return;
        }

        Log.e("neuronListTest","Msg: " + Msg);
    }


    public void BRINRESReSTest(){

        /*
            return: RES:6;RES(13149x17500x5520);RES(1643x2187x690);RES(26298x35000x11041);RES(3287x4375x1380);RES(6574x8750x2760);RES(821x1093x345)
         */

        Log.e("BRINRESReSTest"," ---- Start ----");
        makeConnect();

        //Brain_Num
        //18465

        sendMessage("18454" + ":BRAINRES.\n");

        String Msg = getMessage();
        if (Msg == null){
            Toast_in_Thread("Socket disconnect When Select_Brain !");
            return;
        }
        if (Msg.equals(EMPTY_MSG)){
            Toast_in_Thread("Fail to read the Brain List !");
            return;
        }

        Log.e("BRINRESRexTest","Msg: " + Msg);

    }


    /*
    Get next img that need process
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void neuronNextTest(){

        Log.e("neuronNextTest"," ---- Start ----");


        this.isDrawMode = true;
//        isDrawMode = false;

        String neuron_num = "18454";
        if (this.isDrawMode){
            sendMessage(neuron_num + ";0;0" + ":BrainNumber.\n");
        }else {
            sendMessage(neuron_num + ";0;1" + ":BrainNumber.\n");
        }

        String Store_path_Img = Store_path + "/Img";
        getImg(Store_path_Img +"/Img",true);

    }



    @RequiresApi(api = Build.VERSION_CODES.N)
    public void imgBlockTest(){
        Log.e("imgBlockTest"," ---- Start ----");
        makeConnect();

        // brain_id;res;x;y;z;size
        // 18465;1;1200;2800;3900;128

//        String file_path = mContext.getExternalFilesDir(null).toString() + "/Sync/BlockGet";
//        String SwcFileName = "18454_00001" + "__" +
//                "x_start" + "__" + "y_start" + "__" + "z_start" + "__" + "x_end" + "__" + "y_end" + "__" + "z_end";

        // ratio: highest res / current res
//        sendMessage(SwcFileName + "__" + "ratio" + ":imgblock.\n");
//        getFile(file_path, true);

        sendMessage("18454;1;1200;2800;3900;128" + ":imgblock.\n");

        String Store_path_Img = Store_path + "/Img";
        getImg(Store_path_Img +"/Img",true);


    }


    // 同之前一样
    public String GetBBSwcTest(){
        Log.e("GetBBSwcTest"," ---- Start ----");
        makeConnect();

        String file_path = mContext.getExternalFilesDir(null).toString() + "/Sync/BlockGet";
//        String SwcFileName = "neuron_num" + "__" +
//                "x_start" + "__" + "y_start" + "__" + "z_start" + "__" + "x_end" + "__" + "y_end" + "__" + "z_end";

        String SwcFileName = "18454_00001" + "__" +
                "100" + "__" + "200" + "__" + "300" + "__" + "228" + "__" + "328" + "__" + "428";

        // ratio: highest res / current res
        sendMessage(SwcFileName + "__" + "2" + ":GetBBSwc.\n");
        getFile(file_path, true);

        String SwcFilePath = file_path + "/blockGet__" + SwcFileName + "__" + "ratio"  + ".swc";
        return SwcFilePath;

    }




    public void ArborCheckTest(){
        Log.e("ArborCheckTest"," ---- Start ----");
        makeConnect();

        // neuron_num + arbor_num;flag;id;                flag -- 0:no  1:yes  2:uncertain
        // 17302_00001_00001;0;xf;

        sendMessage("18454_00001_00001;0;xf" + ":ArborCheck.\n");

    }




    public void GetArborResultTest(){
        Log.e("BRINRESRexTest"," ---- Start ----");
        makeConnect();

        //Brain_Num
        //18465

        sendMessage("Brain_Num" + ":GetArborResult.\n");

    }





    /**
     * Choose the file you want down
     * @param context the activity context
     * @param items the list of filename
     * @param type the communication type
     */
    private void showListDialog(final Context context, final String[] items, final String type) {

        AlertDialog.Builder listDialog =
                new AlertDialog.Builder(context);
        listDialog.setTitle("选择要下载的文件");
        listDialog.setItems(items, new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(DialogInterface dialog, int which) {

//                Toast.makeText(context,"你点击了" + items[which], Toast.LENGTH_SHORT).show();

                Log.e("showListDialog", type);

                Send_Brain_Number(items[which]);

                if (type.equals("CurrentDirDownExp"))
//                    send1(items[which], context);
                if (type.equals("CurrentDirLoadExp"))
//                    send1(items[which], context);
                if (type == "CurrentDirImgDownExp" ){
                    Log.e("showListDialog","Start to Send BrainNumber.");
                    Send_Brain_Number(items[which]);
                }

//                Log.e("showListDialog","Start to Send BrainNumber.");

            }
        });
        listDialog.show();

    }


    public void showListDialog(final String[] items) {
        /*
         * 根据脑图的id和模式返回神经元列表
         * 17302;0;0
         * 0:脑图像名称
         * 1:是否是下一个/列表 0：下一个，1:列表
         * 2：预重建/校验 0:预重建，1：校验
         */

        new XPopup.Builder(mContext)
                .maxHeight(1350)
                .asCenterList("Select a Brain", items,
                        new OnSelectListener() {
                            @RequiresApi(api = Build.VERSION_CODES.N)
                            @Override
                            public void onSelect(int position, String text) {
                                Send_Brain_Number(text);
                                if (Remote_Socket.isDrawMode){
                                    String brain_num = text + ";" + "1" + "0";
                                    sendBrainNumber(brain_num);
                                }else {
                                    String brain_num = text + ";" + "1" + "0";
                                    sendBrainNumber(brain_num);
                                }
                            }
                        })
                .show();

    }


    public void showListDialog_Check(final String[] items){

        new XPopup.Builder(mContext)
//        .maxWidth(400)
//        .maxHeight(1350)
                .asCenterList("Select a Arbor", items,
                        new OnSelectListener() {
                            @RequiresApi(api = Build.VERSION_CODES.N)
                            @Override
                            public void onSelect(int position, String text) {
                                System.out.println("text: " + text);
                                System.out.println("text: " + text.replace(".v3draw",""));

                                Send_Arbor_Number(text.replace(".v3draw",""));
                            }
                        })
                .show();

    }



    public String PullSwc_block(boolean isDrawMode){

        makeConnect();

        String SwcFilePath = "Error";

        if (checkConnection()){

            if (isDrawMode){

                String file_path = mContext.getExternalFilesDir(null).toString() + "/Sync/BlockGet";

                String filename = getFilename_Remote(mContext);
                String neuron_number = getNeuronNumber_Remote(mContext, filename);
                String offset = getoffset_Remote(mContext, filename);
                int[] index = BigImgReader.getIndex(offset);
                System.out.println(filename);

                String ratio = Integer.toString(getRatio_SWC());

                String SwcFileName = neuron_number + "__" +
                        index[0] + "__" +index[3] + "__" + index[1] + "__" + index[4] + "__" + index[2] + "__" + index[5];

                sendMessage(SwcFileName + "__" + ratio + ":GetBBSwc.\n");
                getFile(file_path, true);

                SwcFilePath = file_path + "/blockGet__" + SwcFileName + "__" + ratio  + ".swc";

            }else {

                String file_path = mContext.getExternalFilesDir(null).toString() + "/Check/Sync/BlockGet";

                String filename = getFilename_Remote_Check(mContext);
                String offset = getoffset_Remote_Check(mContext, filename);
                System.out.println(filename);

                String offset_final = offsetSwitch(filename, offset);
                String SwcFileName = filename + offset_final;

                sendMessage(SwcFileName + ":GetArborSwc.\n");
                getFile(file_path, true);

                SwcFilePath = file_path + "/blockGet__" + SwcFileName.replaceAll(";","__") + ".swc";

            }


        }else {
            Toast_in_Thread("Can't Connect Server, Try Again Later !");
        }

        return SwcFilePath;
    }




    public String PullApo_block(){

        makeConnect();

        String ApoFilePath = "Error";

        if (checkConnection()){


            String file_path = mContext.getExternalFilesDir(null).toString() + "/Sync/BlockGet/Apo";

            String filename = getFilename_Remote(mContext);
            String neuron_number = getNeuronNumber_Remote(mContext, filename);
            String offset = getoffset_Remote(mContext, filename);
            int[] index = BigImgReader.getIndex(offset);
            System.out.println(filename);

            String ratio = Integer.toString(getRatio_SWC());

            String ApoFileName = neuron_number + "__" +
                    index[0] + "__" +index[3] + "__" + index[1] + "__" + index[4] + "__" + index[2] + "__" + index[5];

            sendMessage(ApoFileName + "__" + ratio + ":GetBBApo.\n");
            getFile(file_path, true);

            ApoFilePath = file_path + "/blockGet__" + ApoFileName + "__" + ratio  + ".apo";

        }else {
            Toast_in_Thread("Can't Connect Server, Try Again Later !");
        }

        return ApoFilePath;
    }


    public void PushSwc_block(String filename, InputStream is, long length){

        makeConnect();

        if (checkConnection()){

            socket_send.Send_File(ManageSocket, filename, is, length);

        }else {
            Toast_in_Thread("Can't Connect Server, Try Again Later !");
        }

    }


    public void PushApo_block(String filename, InputStream is, long length){

        makeConnect();

        if (checkConnection()){

            socket_send.Send_File(ManageSocket, filename, is, length);

        }else {
            Toast_in_Thread("Can't Connect Server, Try Again Later !");
        }

    }


    public void Select_Arbor(){

        sendMessage("connect for android client" + ":GetArborList.\n");
        String Msg = getMessage();

        if (Msg.equals(EMPTY_MSG)){
            Toast_in_Thread("Something Wrong When Select_Arbor !");
            return;
        }

        processMsg(Msg);

    }


    /*
     * 要求发送全脑图像列表
     * 0:预重建
     * 1:检查
     */
    public void select_Brain(boolean isDrawMode){

        backup();
        Remote_Socket.isDrawMode = isDrawMode;

        if (isDrawMode){
            sendMessage("0" + ":choose3.\n");
        }else {
            sendMessage("1" + ":choose3.\n");
        }

        String Msg = getMessage();
        if (Msg == null){
            Toast_in_Thread("Socket disconnect When Select_Brain !");
            return;
        }
        if (Msg.equals(EMPTY_MSG)){
            Toast_in_Thread("Fail to read the Brain List !");
            return;
        }
        processMsg(Msg);

    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public void Send_Arbor_Number(String ArborNumber){

        makeConnect();

        Log.e("Send_Brain_Number","Start to Send ArborNumber.");

        ArborNumber_Selected = ArborNumber;
        setFilename_Remote_Check(ArborNumber, mContext);
        String offset_check = getoffset_Remote_Check(mContext, ArborNumber);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendMessage(ArborNumber + ";" + offset_check + ":GetArbor.\n");

                String Store_path_Img = Store_path + "/Img/Check";
                getImg(Store_path_Img,true);
            }
        });

        thread.start();

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void Send_Brain_Number(String BrainNumber){

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                makeConnect();

                Log.e("Send_Brain_Number","Start to Send BrainNumber.");

                BrainNumber_Selected = BrainNumber.replace("check","");
                setFilename_Remote(BrainNumber.replace("check",""), mContext);
                sendMessage(BrainNumber + ":BrainNumber.\n");

                String Store_path_txt = Store_path + "/BrainInfo";
                String Final_Path = getFile(Store_path_txt, true);

                if (Final_Path.equals(EMPTY_FILE_PATH) || Final_Path.equals(SOCKET_CLOSED)){
                    Toast_in_Thread("Failed to get BrainInfo");
                    return;
                }

                analyzeTXT(Final_Path);

                Select_RES(Transform(RES_List, 0, RES_List.size()));


            }
        });
        thread.start();


    }

    public void sendBrainNumber(String BrainNumber){

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                /*
                ensure the connection
                 */
                makeConnect();

                Log.e("sendBrainNumber","Start to Send BrainNumber.");
                sendMessage(BrainNumber + ":BrainNumber.\n");

                String Msg = getMessage();

                if (Msg == null){
                    Toast_in_Thread("Socket disconnect When Select_Brain !");
                    return;
                }

                if (Msg.equals(EMPTY_MSG)){
                    Toast_in_Thread("Fail to read the Brain List !");
                    return;
                }
                processMsg(Msg);



                Log.e("Send_Brain_Number","Start to Send BrainNumber.");

                BrainNumber_Selected = BrainNumber.replace("check","");
                setFilename_Remote(BrainNumber.replace("check",""), mContext);
                sendMessage(BrainNumber + ":BrainNumber.\n");

                String Store_path_txt = Store_path + "/BrainInfo";
                String Final_Path = getFile(Store_path_txt, true);

                if (Final_Path.equals(EMPTY_FILE_PATH) || Final_Path.equals(SOCKET_CLOSED)){
                    Toast_in_Thread("Failed to get BrainInfo");
                    return;
                }

                analyzeTXT(Final_Path);

                Select_RES(Transform(RES_List, 0, RES_List.size()));


            }
        });
        thread.start();


    }



    public void selectBlock(){

        if (getFilename_Remote(mContext).equals("--11--")){
            Toast.makeText(mContext,"Select a Remote File First, please !", Toast.LENGTH_SHORT).show();
            Log.e("SelectBlock","The File is not Selected");
            return;
        }

        makeConnect();

        if (checkConnection()){
            PopUp(true);
        }else {
            Toast_in_Thread("Can't Connect Server, Try Again Later !");
        }


    }

    private void PopUp(boolean isDirect){

        String offset_x, offset_y, offset_z, size;

        if (isDirect){
            String filename = getFilename_Remote(mContext);
            String offset = getoffset_Remote(mContext, filename);
            offset_x = offset.split("_")[0];
            offset_y = offset.split("_")[1];
            offset_z = offset.split("_")[2];
            size     = offset.split("_")[3];
        }else {
            String[] offset_transform = transform_offset();
            offset_x = offset_transform[0];
            offset_y = offset_transform[1];
            offset_z = offset_transform[2];
            size     = "128";
        }

        String offset = offset_x + "_" + offset_y + "_" + offset_z + "_" + size;
        String filename = getFilename_Remote(mContext);
        setoffset_Remote(offset, filename, mContext);

        String[] input = judgeEven(offset_x, offset_y, offset_z, size);

        if (!judgeBounding(input)){
            PopUp(isDirect);
            Toast.makeText(mContext, "Please Make Sure All the Information is Right !", Toast.LENGTH_SHORT).show();
        }else {
            makeConnect();

            if (checkConnection()){
                PullImageBlock(input[0], input[1], input[2], input[3], false);
            }else {
                Toast_in_Thread("Can't Connect Server, Try Again Later !");
            }

        }

    }




    private void PullImageBlock(final String offset_x, final String offset_y, final String offset_z, final String size, boolean NeedWaited){

        Thread thread = new Thread() {

            @RequiresApi(api = Build.VERSION_CODES.N)
            public void run() {

                try {

                    String filename = getFilename_Remote(mContext);

                    String Store_path_Img = Store_path + "/Img";
                    String StoreFilename = filename +
                            "_" + offset_x + "_" + offset_y + "_" + offset_z + "_" + size +"_" + size +"_" + size + ".v3dpbd";

                    File img = new File(Store_path_Img + "/" + StoreFilename);

                    if (img.exists() && img.length()>0){

                        Log.d("PullImageBlock","The File exists Already !");

                        ActivityManager activityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
                        String runningActivity = activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
                        Log.e("RunningActivity", runningActivity);
                        MainActivity.LoadBigFile_Remote(Store_path_Img + "/" + StoreFilename);

                    } else {

                        String msg = filename + "__" + offset_x + "__" + offset_y + "__" + offset_z + "__" + size + ":imgblock.\n";
                        sendMessage(msg);

                        getImg(Store_path_Img,true);
                        Log.e("PullImageBlock", "x: " + offset_x + ", y:" + offset_y + ", z:" +offset_z + ", size: " + size +  " successfully---------");

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast_in_Thread("Can't Connect, Try Again Please !");
                }

            }
        };

        thread.start();

        if (NeedWaited){
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    public void pullImageBlockWhenLoadGame(String filename, String offset){

        Thread thread = new Thread(){

            @Override
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void run(){
                String offset_x = offset.split("_")[0];
                String offset_y = offset.split("_")[1];
                String offset_z = offset.split("_")[2];
                String size = offset.split("_")[3];

                String store_path = mContext.getExternalFilesDir(null).toString();
                String storeFilename = filename +
                        "_" + offset_x + "_" + offset_y + "_" + offset_z + "_" + size +"_" + size +"_" + size + ".v3dpbd";

                File img = new File(store_path + "/Img/" + storeFilename);

                if (img.exists() && img.length()>0){

                    Log.d("PullImageBlock","The File exists Already !");

                    ActivityManager activityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
                    String runningActivity = activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
                    Log.e("RunningActivity", runningActivity);
                    GameActivity.LoadBigFile_Remote(store_path + "/Img/" + storeFilename);

                } else {

                    String msg = filename + "__" + offset_x + "__" + offset_y + "__" + offset_z + "__" + size + ":imgblock.\n";
                    sendMessage(msg);

                    getImg(store_path +"/Img",true);
                    Log.e("PullImageBlock", "x: " + offset_x + ", y:" + offset_y + ", z:" +offset_z + ", size: " + size +  " successfully---------");

                }
            }
        };

        thread.start();

//        try{
//            thread.join();
//        } catch (Exception e){
//            e.printStackTrace();
//        }

    }


    public void Select_RES(String[] RESs){

        try{
            if (isDrawMode){
                RES_Selected = RESs[RESs.length - 1];
            }else {
                if (2 > RESs.length - 1){
                    RES_Selected = RESs[RESs.length - 1];
                }else {
                    RES_Selected = RESs[2];
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            Toast_in_Thread("Something Wrong with the BrainInfo.txt");
            return;
        }



        setFilename_Remote(BrainNumber_Selected + "/" + RES_Selected, mContext);   //  18454/RES250x250x250

        String filename = getFilename_Remote(mContext);
        String neuron_num = getNeuronNumber_Remote(mContext,filename);
        Vector<String> Neuron_Number_List_show = new Vector<>();

        if (!neuron_num.equals("--11--")){
            Neuron_Number_List_show = Adjust_Index();
        }else {
            Neuron_Number_List_show = Neuron_Number_List;
        }

        if (!isDrawMode){
            Update_Check_Result();
            for (int i=0; i<Neuron_Number_List_show.size(); i++){
                String neuron_num_display = Neuron_Number_List_show.get(i);
                if (Neuron_checked.containsKey(neuron_num_display)){
                    Neuron_Number_List_show.set(i, neuron_num_display + " " + Neuron_checked.get(neuron_num_display));
                }
            }
        }

        String[] Neuron_Number_List_show_array = Transform(Neuron_Number_List_show, 0, Neuron_Number_List_show.size());
        Select_Neuron(Neuron_Number_List_show_array, ellipsize(Neuron_Number_List_show_array));

//        new XPopup.Builder(mContext)
////        .maxWidth(400)
////        .maxHeight(1350)
//                .asCenterList("Select a RES", RESs,
//                        new OnSelectListener() {
//                            @Override
//                            public void onSelect(int position, String text) {
//                                RES_Selected = text;
//                                Select_Neuron(Transform(Neuron_Number_List));
//                                setFilename_Remote(BrainNumber_Selected + "/" + RES_Selected, mContext);   //  18454/RES250x250x250
//                            }
//                        })
//                .show();

    }

    public void Select_Neuron(String[] Neurons, String[] Neurons_show){

//        for (int i=0; i<Neurons.length; i++){
//            Log.e("Select_Neuron",Neurons[i] + "Neurons_show[]: " + Neurons_show[i]);
//        }

        new XPopup.Builder(mContext)
//        .maxWidth(400)
        .maxHeight(1350)
                .asCenterList("Select a Neuron", Neurons_show,
                        new OnSelectListener() {
                            @Override
                            public void onSelect(int position, String text) {

                                Log.d(TAG,"position: " + position);
                                Neuron_Number_Selected = Neurons[position].split(" ")[0];

                                if (isDrawMode){
                                    System.out.println(Neuron_Number_Selected);
                                    if (Neuron_Info.get(Neuron_Number_Selected) != null){
                                        // Select soma without choose pos
                                        Pos_Selected = Neuron_Info.get(Neuron_Number_Selected).get(0);
                                        PopUp(false);

//                                    Select_Pos(Transform(Neuron_Info.get(Neuron_Number_Selected)));
                                        setNeuronNumber_Remote(Neuron_Number_Selected, BrainNumber_Selected + "/" + RES_Selected, mContext);  //18454_00002
                                    }else {
                                        Toast_in_Thread("Information not Exist !");
                                    }
                                }else {
                                    System.out.println(Neuron_Number_Selected);
                                    if (Neuron_Info.get(Neuron_Number_Selected) != null){
                                        // Select soma without choose pos
//                                        Pos_Selected = Neuron_Info.get(Neuron_Number_Selected).get(0);
//                                        PopUp(false);

                                        Vector<String> arbor_list = Neuron_Info.get(Neuron_Number_Selected);
                                        if (Neuron_Arbor_checked.containsKey(Neuron_Number_Selected)){
                                            Vector<String> arbor_list_checked = Neuron_Arbor_checked.get(Neuron_Number_Selected);
                                            for (int i=1; i<arbor_list.size(); i++){
                                                String arbor_num = arbor_list.get(i);
                                                if (arbor_list_checked.contains(arbor_num.split(":")[0])){
                                                    arbor_list.set(i, "√ " + arbor_num);
                                                }
                                            }
                                            Neuron_Info.put(Neuron_Number_Selected,arbor_list);
                                        }

                                        Select_Pos(Transform(Neuron_Info.get(Neuron_Number_Selected), 1, Neuron_Info.get(Neuron_Number_Selected).size()));
                                        setNeuronNumber_Remote(Neuron_Number_Selected, BrainNumber_Selected + "/" + RES_Selected, mContext);  //18454_00002
                                    }else {
                                        Toast_in_Thread("Information not Exist !");
                                    }
                                }

                            }
                        })
                .show();

    }


    public void Select_Neuron_Fast(){

        backup();
        Update_Check_Result();
        Vector<String> Neuron_Number_List_show = Adjust_Index();

        for (int i=0; i<Neuron_Number_List_show.size(); i++){
            String neuron_num_display = Neuron_Number_List_show.get(i);
            if (Neuron_checked.containsKey(neuron_num_display)){
                Neuron_Number_List_show.set(i, neuron_num_display + " " + Neuron_checked.get(neuron_num_display));
            }
        }

        String[] Neuron_Number_List_show_array = Transform(Neuron_Number_List_show, 0, Neuron_Number_List_show.size());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Select_Neuron(Neuron_Number_List_show_array, ellipsize(Neuron_Number_List_show_array));
            }
        });

        thread.start();
    }

    public void Select_Arbor_Fast(){

        String filename = getFilename_Remote(mContext);
        Neuron_Number_Selected = getNeuronNumber_Remote(mContext,filename);

        Update_Check_Result();

        System.out.println(Neuron_Number_Selected);
        if (Neuron_Info.get(Neuron_Number_Selected) != null) {

            Vector<String> arbor_list = Neuron_Info.get(Neuron_Number_Selected);
            if (Neuron_Arbor_checked.containsKey(Neuron_Number_Selected)){
                Vector<String> arbor_list_checked = Neuron_Arbor_checked.get(Neuron_Number_Selected);

                if (! arbor_list_checked.contains(getArborNum(mContext,
                        filename.split("/")[0] + "_" + Neuron_Number_Selected).split(":")[0])){
                    Toast_in_Thread("You haven't finished the check of current Arbor yet !");
                }

                for (int i=1; i<arbor_list.size(); i++){
                    String arbor_num = arbor_list.get(i);
                    if (arbor_list_checked.contains(arbor_num.split(":")[0])){
                        arbor_list.set(i, "√ " + arbor_num);
                    }
                }
                Neuron_Info.put(Neuron_Number_Selected,arbor_list);
            }else {
                Toast_in_Thread("You haven't finished the check of current Arbor yet !");
            }

            Select_Pos(Transform(Neuron_Info.get(Neuron_Number_Selected), 1, Neuron_Info.get(Neuron_Number_Selected).size()));
            setNeuronNumber_Remote(Neuron_Number_Selected, BrainNumber_Selected + "/" + RES_Selected, mContext);  //18454_00002
        }else {
            Toast_in_Thread("Something Wrong When choose Arbor");
        }

    }

    public void Next_Neuron(){

        String filename = getFilename_Remote(mContext);
        String neuron_num = getNeuronNumber_Remote(mContext,filename);

        int index = Neuron_Number_List.indexOf(neuron_num);
        Neuron_Number_Selected = Neuron_Number_List.get( (index+1)%Neuron_Number_List.size() );

        if (Neuron_Info.get(Neuron_Number_Selected) != null){
            // Select soma without choose pos
            Pos_Selected = Neuron_Info.get(Neuron_Number_Selected).get(0);
            PopUp(false);
            setNeuronNumber_Remote(Neuron_Number_Selected, BrainNumber_Selected + "/" + RES_Selected, mContext);  //18454_00002
        }else {
            Toast_in_Thread("Information not Exist !");
        }

    }


    private Vector<String> Adjust_Index(){

        Vector<String> Neuron_Number_List_show = new Vector<String>();

        String filename = getFilename_Remote(mContext);
        String neuron_num = getNeuronNumber_Remote(mContext,filename);

        int index = Neuron_Number_List.indexOf(neuron_num);
        Log.e("Adjust_Index"," " + index);

        int start = Math.max(index - 1, 0);
        int end   = (index - 1) > 0 ? Neuron_Number_List.size() + index-1 : Neuron_Number_List.size();
        for (int i = start; i < end; i++){
            Neuron_Number_List_show.add(Neuron_Number_List.get(i % Neuron_Number_List.size()));
        }

        return Neuron_Number_List_show;
    }


    private Vector<String> Adjust_Index_Check(){

        Vector<String> Arbor_Number_List_show = new Vector<String>();

        if (Arbor_Check_List != null){
            String arbor_name = getFilename_Remote_Check(mContext);

            int index = Neuron_Number_List.indexOf(arbor_name);
            Log.e("Adjust_Index"," " + index);
            if (index >= 0){

                int start = Math.max(index - 1, 0);
                int end   = (index - 1) > 0 ? Arbor_Check_List.size() + index : Arbor_Check_List.size() + index - 1;
                for (int i = start; i <= end; i++){
                    Arbor_Number_List_show.add(Arbor_Check_List.get(i % Arbor_Check_List.size()));
                }

            }else {
                Arbor_Number_List_show = Arbor_Check_List;
            }

        }else {
            Toast_in_Thread("Something Wrong When Show the Arbor List !");
        }

        return Arbor_Number_List_show;
    }


    public void Select_Pos(String[] Pos){

        new XPopup.Builder(mContext)
//        .maxWidth(400)
//        .maxHeight(1350)
                .asCenterList("Select a Pos", Pos,
                        new OnSelectListener() {
                            @Override
                            public void onSelect(int position, String text) {
                                Pos_Selected = text.replace("√ ","");
                                text = text.replace("√ ","");

                                if (!isDrawMode){
                                    String boundingbox = text.substring(ordinalIndexOf(text, ";", 3)+1);
                                    String ArborNum = text.substring(0, ordinalIndexOf(text, ";", 3));
                                    String filename = getFilename_Remote(mContext);
                                    String neuron_num = getNeuronNumber_Remote(mContext,filename);
                                    setArborNum(ArborNum, filename.split("/")[0] + "_" + neuron_num, mContext);

                                    Log.e(TAG,ArborNum);
                                    Log.e(TAG,filename.split("RES")[0]);

                                    Vector<String> res_temp = getRES(mContext, BrainNumber_Selected);
                                    String res_cur  = filename.split("/")[1];   // RES: RES250x250x250
                                    float ratio = getRatio(res_cur, res_temp.lastElement());
                                    String boundingbox_new = getNewBoundingBox(boundingbox, ratio);
                                    setBoundingBox(boundingbox_new, filename + "/" + neuron_num + "/" + ArborNum.split(":")[0].replace(" ","_"), mContext);
                                }

                                PopUp(false);
                            }
                        })
                .show();

    }



    public void Selectblock_fast(Context context, boolean source, String direction){

        if (getFilename_Remote(context) == "--11--"){
            Toast.makeText(context,"Select file first!", Toast.LENGTH_SHORT).show();
            return;
        }

        makeConnect();

        if (checkConnection()){
            switch (direction){
                case "Left":
                    PullImageBlock_fast(context, "Left");
                    break;
                case "Right":
                    PullImageBlock_fast(context, "Right");
                    break;
                case "Top":
                    PullImageBlock_fast(context, "Top");
                    break;
                case "Bottom":
                    PullImageBlock_fast(context, "Bottom");
                    break;
                case "Front":
                    PullImageBlock_fast(context, "Front");
                    break;
                case "Back":
                    PullImageBlock_fast(context, "Back");
                    break;
                default:
                    Toast.makeText(context,"Something wrong when pull img", Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast_in_Thread("Can't Connect Server, Try Again Later !");
        }

    }

    private void PullImageBlock_fast(Context context, String direction){
        backup();

        String filename_root = getFilename_Remote(context);        // BrainNumber: 18465/RES250x250x250
        String offset = getoffset_Remote(context, filename_root);  // offset:

        String offset_x = offset.split("_")[0];
        String offset_y = offset.split("_")[1];
        String offset_z = offset.split("_")[2];
        String size     = offset.split("_")[3];

        int size_i     = Integer.parseInt(size);
        int offset_x_i = Integer.parseInt(offset_x);
        int offset_y_i = Integer.parseInt(offset_y);
        int offset_z_i = Integer.parseInt(offset_z);

        String filename = filename_root.replace(")","").replace("(","");
        String img_size = filename.split("RES")[1];

        System.out.println("img_size: hhh-------" + img_size + "--------hhh");

        // current filename mouse18864_teraconvert/RES(35001x27299x10392)   : y x z
        int img_size_x_i = Integer.parseInt(img_size.split("x")[1]);
        int img_size_y_i = Integer.parseInt(img_size.split("x")[0]);
        int img_size_z_i = Integer.parseInt(img_size.split("x")[2]);

        switch (direction){

            case "Left":
                if ( (offset_x_i - size_i/2 -1) == 0 ){
                    System.out.println("----- You have already reached left boundary!!! -----");
                    Toast_in_Thread("You have already reached left boundary!!!");
                    return;
                }else {
                    offset_x_i -= size_i/2 + 1;
                    if (offset_x_i - size_i/2 <= 0)
                        offset_x_i = size_i/2 + 1;
                }
                break;

            case "Right":
                if ( (offset_x_i + size_i/2) == img_size_x_i - 1 ){
                    Toast_in_Thread("You have already reached right boundary!!!");
                    return;
                }else {
                    offset_x_i += size_i/2;
                    if (offset_x_i + size_i/2 > img_size_x_i - 1)
                        offset_x_i = img_size_x_i - 1 - size_i/2;
                }
                break;

            case "Top":
                if ( (offset_y_i - size_i/2 -1) == 0 ){
                    Toast_in_Thread("You have already reached top boundary!!!");
                    return;
                }else {
                    offset_y_i -= size_i/2 + 1;
                    if (offset_y_i - size_i/2 <= 0)
                        offset_y_i = size_i/2 + 1;
                }
                break;

            case "Bottom":
                if ( (offset_y_i + size_i/2) == img_size_y_i - 1 ){
                    Toast_in_Thread("You have already reached bottom boundary!!!");
                    return;
                }else {
                    offset_y_i += size_i/2;
                    if (offset_y_i + size_i/2 > img_size_y_i - 1)
                        offset_y_i = img_size_y_i - 1 - size_i/2;
                }
                break;

            case "Front":
                if ( (offset_z_i - size_i/2 -1) == 0 ){
                    Toast_in_Thread("You have already reached front boundary!!!");
                    return;
                }else {
                    offset_z_i -= size_i/2 + 1;
                    if (offset_z_i - size_i/2 <= 0)
                        offset_z_i = size_i/2 + 1;
                }
                break;

            case "Back":
                if ( (offset_z_i + size_i/2) == img_size_z_i - 1 ){
                    Toast_in_Thread("You have already reached back boundary!!!");
                    return;
                }else {
                    offset_z_i += size_i/2;
                    if (offset_z_i + size_i/2 > img_size_z_i - 1)
                        offset_z_i = img_size_z_i - 1 - size_i/2;
                }
                break;

        }

        offset_x = Integer.toString(offset_x_i);
        offset_y = Integer.toString(offset_y_i);
        offset_z = Integer.toString(offset_z_i);

        offset = offset_x + "_" + offset_y + "_" + offset_z + "_" + size;
        setoffset_Remote(offset, filename_root, context);

        System.out.println("---------" + offset + "---------");
        String[] input = judgeEven(offset_x, offset_y, offset_z, size);

        if (!judgeBounding(input)){
            Toast_in_Thread("Please make sure the size of block not beyond the img !");
        }else {
            PullImageBlock(input[0], input[1], input[2], input[3], true);
        }

    }

    public void PullImageBlock_Dir(Context context, float [] dir){
        String filename_root = getFilename_Remote(context);        // BrainNumber: 18465/RES250x250x250
        String offset = getoffset_Remote(context, filename_root);  // offset:

        String offset_x = offset.split("_")[0];
        String offset_y = offset.split("_")[1];
        String offset_z = offset.split("_")[2];
        String size     = offset.split("_")[3];

        int size_i     = Integer.parseInt(size);
        int offset_x_i = Integer.parseInt(offset_x);
        int offset_y_i = Integer.parseInt(offset_y);
        int offset_z_i = Integer.parseInt(offset_z);

        String filename = filename_root.replace(")","").replace("(","");
        String img_size = filename.split("RES")[1];

        System.out.println("img_size: hhh-------" + img_size + "--------hhh");

        // current filename mouse18864_teraconvert/RES(35001x27299x10392)   : y x z
        int img_size_x_i = Integer.parseInt(img_size.split("x")[1]);
        int img_size_y_i = Integer.parseInt(img_size.split("x")[0]);
        int img_size_z_i = Integer.parseInt(img_size.split("x")[2]);

        float [] normalDir = new float[3];
        normalDir[0] = dir[0] / (float)Math.sqrt((dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]));
        normalDir[1] = dir[1] / (float)Math.sqrt((dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]));
        normalDir[2] = dir[2] / (float)Math.sqrt((dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]));

        offset_x_i += normalDir[0] * size_i / 2;
        offset_y_i += normalDir[1] * size_i / 2;
        offset_z_i += normalDir[2] * size_i / 2;

        if (offset_x_i - size_i / 2 < 1){
            Toast_in_Thread("You have already reached boundary!!!");
            offset_x_i = size_i / 2 + 1;
        } else if (offset_x_i + size_i/2 > img_size_x_i - 1){
            Toast_in_Thread("You have already reached  boundary!!!");
            offset_x_i = img_size_x_i - size_i / 2 - 1;
        }

        if (offset_y_i - size_i / 2 < 1){
            Toast_in_Thread("You have already reached  boundary!!!");
            offset_y_i = size_i / 2 + 1;
        } else if (offset_y_i + size_i / 2 > img_size_y_i - 1){
            Toast_in_Thread("You have already reached  boundary!!!");
            offset_y_i = img_size_y_i - size_i / 2 - 1;
        }

        if (offset_z_i - size_i / 2 < 1){
            Toast_in_Thread("You have already reached  boundary!!!");
            offset_z_i = size_i / 2 + 1;
        } else if (offset_z_i + size_i / 2 > img_size_z_i - 1){
            Toast_in_Thread("You have already reached  boundary!!!");
            offset_z_i = img_size_z_i - size_i / 2 - 1;
        }

        offset_x = Integer.toString(offset_x_i);
        offset_y = Integer.toString(offset_y_i);
        offset_z = Integer.toString(offset_z_i);

        offset = offset_x + "_" + offset_y + "_" + offset_z + "_" + size;
        setoffset_Remote(offset, filename_root, context);

        System.out.println("---------" + offset + "---------");
        String[] input = judgeEven(offset_x, offset_y, offset_z, size);

        if (!judgeBounding(input)){
            Toast_in_Thread("Please make sure the size of block not beyond the img !");
        }else {
            PullImageBlock(input[0], input[1], input[2], input[3], true);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public void Selectblock_fast_Check(Context context, boolean source, String direction){

        if (getFilename_Remote_Check(context).equals("--11--")){
            Toast.makeText(context,"Select file first!", Toast.LENGTH_SHORT).show();
            return;
        }

        makeConnect();

        if (checkConnection()){

            String[] Direction = {"Left", "Right", "Top", "Bottom", "Front", "Back"};

            if (Arrays.asList(Direction).contains(direction)){
                PullImageBlock_fast_Check(context, direction);
            }else {
                Toast.makeText(context,"Something wrong when pull img", Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast_in_Thread("Can't Connect Server, Try Again Later !");
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public void PullImageBlock_fast_Check(Context context, String direction){
        String filename = getFilename_Remote_Check(context);
        String offset = getoffset_Remote_Check(context, filename);

        int img_size_x_i = getImg_size()[0];
        int img_size_y_i = getImg_size()[1];
        int img_size_z_i = getImg_size()[2];

        int offset_x_i = ( Integer.parseInt(offset.split(";")[0]) + Integer.parseInt(offset.split(";")[1]) ) / 2 -1;
        int offset_y_i = ( Integer.parseInt(offset.split(";")[2]) + Integer.parseInt(offset.split(";")[3]) ) / 2 -1;
        int offset_z_i = ( Integer.parseInt(offset.split(";")[4]) + Integer.parseInt(offset.split(";")[5]) ) / 2 -1;
        int size_i     = ( Integer.parseInt(offset.split(";")[1]) - Integer.parseInt(offset.split(";")[0]) );

        switch (direction){
            case "Left":
                if ( (offset_x_i - size_i/2) == 0 ){
                    System.out.println("----- You have already reached left boundary!!! -----");
                    Toast_in_Thread("You have already reached left boundary!!!");
                    return;
                }else {
                    offset_x_i -= size_i/2;
                    if (offset_x_i - size_i/2 < 0)
                        offset_x_i = size_i/2;
                }
                break;

            case "Right":
                if ( (offset_x_i + size_i/2) == img_size_x_i - 1 ){
                    Toast_in_Thread("You have already reached right boundary!!!");
                    return;
                }else {
                    offset_x_i += size_i/2;
                    if (offset_x_i + size_i/2 > img_size_x_i - 1)
                        offset_x_i = img_size_x_i - 1 - size_i/2;
                }
                break;

            case "Top":
                if ( (offset_y_i - size_i/2) == 0 ){
                    Toast_in_Thread("You have already reached top boundary!!!");
                    return;
                }else {
                    offset_y_i -= size_i/2;
                    if (offset_y_i - size_i/2 < 0)
                        offset_y_i = size_i/2;
                }
                break;

            case "Bottom":
                if ( (offset_y_i + size_i/2) == img_size_y_i - 1 ){
                    Toast_in_Thread("You have already reached bottom boundary!!!");
                    return;
                }else {
                    offset_y_i += size_i/2;
                    if (offset_y_i + size_i/2 > img_size_y_i - 1)
                        offset_y_i = img_size_y_i - 1 - size_i/2;
                }
                break;

            case "Front":
                if ( (offset_z_i - size_i/2) == 0 ){
                    Toast_in_Thread("You have already reached front boundary!!!");
                    return;
                }else {
                    offset_z_i -= size_i/2;
                    if (offset_z_i - size_i/2 < 0)
                        offset_z_i = size_i/2;
                }
                break;

            case "Back":
                if ( (offset_z_i + size_i/2) == img_size_z_i - 1 ){
                    Toast_in_Thread("You have already reached back boundary!!!");
                    return;
                }else {
                    offset_z_i += size_i/2;
                    if (offset_z_i + size_i/2 > img_size_z_i - 1)
                        offset_z_i = img_size_z_i - 1 - size_i/2;
                }
                break;
        }

        String[] index = {Integer.toString(offset_x_i - size_i/2 + 1), Integer.toString(offset_y_i - size_i/2 + 1), Integer.toString(offset_z_i - size_i/2 +1 ),
                Integer.toString(offset_x_i + size_i/2 + 1), Integer.toString(offset_y_i + size_i/2 + 1), Integer.toString(offset_z_i + size_i/2 +1 )};

        offset = index[0] + ";" + index[3] + ";" + index[1] + ";" + index[4] + ";" + index[2] + ";" + index[5];
        setoffset_Remote_Check(offset, filename, context);

        Send_Arbor_Number(filename);

    }

    private int[] getImg_size(){

        String filename = getFilename_Remote_Check(mContext);

        int start_x = Integer.parseInt(filename.split("RES")[1].split("__")[1]);
        int end_x   = Integer.parseInt(filename.split("RES")[1].split("__")[2]);
        int start_y = Integer.parseInt(filename.split("RES")[1].split("__")[3]);
        int end_y   = Integer.parseInt(filename.split("RES")[1].split("__")[4]);
        int start_z = Integer.parseInt(filename.split("RES")[1].split("__")[5]);
        int end_z   = Integer.parseInt(filename.split("RES")[1].split("__")[6]);

        return new int[]{end_x - start_x, end_y - start_y, end_z - start_z};
    }

    public float[] getImg_size_f(float[] block_offset){

        String filename = getFilename_Remote(mContext);
        String neuron_num = getNeuronNumber_Remote(mContext,filename);
        String ArborNum = getArborNum(mContext,filename.split("/")[0] + "_" + neuron_num);
        String boundingBox = getBoundingBox(mContext,filename + "/" + neuron_num + "/" + ArborNum.split(":")[0].replace(" ","_"));

        String[] boundingBox_arr = boundingBox.split(";");
        int[] boundingbox_arr_i = new int[boundingBox_arr.length];
        for (int i=0; i<boundingBox_arr.length; i++){
            boundingbox_arr_i[i] =  Integer.parseInt(boundingBox_arr[i]);
        }
        block_offset[0] -= boundingbox_arr_i[0];
        block_offset[1] -= boundingbox_arr_i[2];
        block_offset[2] -= boundingbox_arr_i[4];

        Log.e(TAG,Arrays.toString(boundingbox_arr_i));
        Log.e(TAG,Arrays.toString(block_offset));

        return new float[]{boundingbox_arr_i[1] - boundingbox_arr_i[0], boundingbox_arr_i[3] - boundingbox_arr_i[2], boundingbox_arr_i[5] - boundingbox_arr_i[4]};
    }



    public void Zoom_in(){

        Vector<String> res_temp = getRES(mContext, BrainNumber_Selected);
        String filename = getFilename_Remote(mContext);   // BrainNumber: 18465/RES250x250x250
        String res_cur  = filename.split("/")[1];   // RES: RES250x250x250
        int res_index = res_temp.indexOf(res_cur);

        if (res_index >= res_temp.size() - 1){
            Toast_in_Thread("You have been in the Highest Resolution !");
            return;
        }

        switchRES(res_cur, res_temp.get(res_index + 1));

    }

    public void Zoom_out(){

        Vector<String> res_temp = getRES(mContext, BrainNumber_Selected);
        String filename = getFilename_Remote(mContext);
        String res_cur  = filename.split("/")[1];
        int res_index = res_temp.indexOf(res_cur);

        if (res_index <= 0){
            Toast_in_Thread("You have been in the Lowest Resolution !");
            return;
        }

        switchRES(res_cur, res_temp.get(res_index - 1));

    }


    private void switchRES(String RES_current, String RES_object){

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String filename = getFilename_Remote(mContext);
                String offset = getoffset_Remote(mContext, filename);

                float ratio = getRatio(RES_object, RES_current);
                String[] offset_new = getNewOffset(offset, ratio);

                String filename_new = filename.split("/")[0] + "/" + RES_object;
                setFilename_Remote(filename_new, mContext);
                RES_Selected = RES_object;

                String[] input = judgeEven(offset_new[0], offset_new[1], offset_new[2], offset_new[3]);
                judgeBounding(input);

                makeConnect();
                if (checkConnection()){
                    offset = input[0] + "_" + input[1] + "_" + input[2] + "_" + input[3];
                    setoffset_Remote(offset, filename_new, mContext);
                    setNeuronNumber_Remote(Neuron_Number_Selected, filename_new, mContext);

                    if (!isDrawMode){
                        String ArborNum = getArborNum(mContext,filename.split("/")[0] + "_" + Neuron_Number_Selected);
                        String boundingbox = getBoundingBox(mContext,filename + "/" + Neuron_Number_Selected + "/" + ArborNum.split(":")[0].replace(" ","_"));
                        String boundingbox_new = getNewBoundingBox(boundingbox, ratio);
                        setBoundingBox(boundingbox_new, filename_new + "/" + Neuron_Number_Selected + "/" + ArborNum.split(":")[0].replace(" ","_"), mContext);
                    }

                    PullImageBlock(input[0], input[1], input[2], input[3], true);
                }else {
                    setFilename_Remote(filename, mContext);
                    RES_Selected = RES_current;
                    Toast_in_Thread("Can't Connect Server, Try Again Later !");
                }
            }
        });
        thread.start();

    }


    public void pullCheckResult(boolean ifShare){

        makeConnect();

        if (checkConnection()){

            sendMessage("From Android Client :GetArborResult.\n");

            String Store_path_check_txt = Store_path + "/Check/Check_Result";
            String Final_Path = getFile(Store_path_check_txt, true);

            if (Final_Path.equals("Error")){
                Toast_in_Thread("Something Error When Pull Check result");
                return;
            }

            if (!ifShare){
                Display_Result(Final_Path);
            }else {
                try {
                    String excel_filepath = Final_Path.replace(".txt",".xls");
                    CreateCheckExcel(excel_filepath, Final_Path);

                    File excel_file = new File(excel_filepath);
                    if (excel_file.exists()){
                        Intent intent = new Intent();
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        intent.setAction(Intent.ACTION_SEND);
                        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(mContext, "com.example.myapplication__volume.provider", excel_file));  //传输图片或者文件 采用流的方式
                        intent.setType("*/*");   //分享文件
                        mContext.startActivity(Intent.createChooser(intent, "Share From C3"));
                    }else {
                        Toast_in_Thread("File does not exist");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }

    }


    private void Display_Result(String File_Path){
        ArrayList<String> arraylist = new ArrayList<String>();
        File file = new File(File_Path);

        if (!file.exists()){
            Toast_in_Thread("Fail to Open TXT File !");
            return;
        }

        try {
            FileInputStream fid = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fid);
            BufferedReader br = new BufferedReader(isr);
            String str;
            while ((str = br.readLine()) != null) {
                arraylist.add(str.trim());
            }

            String info = "";

            for (int i = 0; i < arraylist.size(); i++){
                String line = arraylist.get(i);

                String display = line.split(" ")[0] + ": " + "arbor " + line.split(" ")[1];

                display = display + "  by " + line.split(" ")[9];

                if (line.split(" ")[8].equals("0")){
                    display = display + "  No";
                }else if (line.split(" ")[8].equals("1")){
                    display = display + "  Yes";
                }else {
                    display = display + "  Uncertain";
                }

                info = info + display + "\n\n";
            }

            br.close();
            isr.close();
            fid.close();

            String finalInfo = info;
            MDDialog mdDialog = new MDDialog.Builder(mContext)
                    .setContentView(R.layout.check_result)
                    .setContentViewOperator(new MDDialog.ContentViewOperator() {
                        @Override
                        public void operate(View contentView) {//这里的contentView就是上面代码中传入的自定义的View或者layout资源inflate出来的view
                            TextView display_info = (TextView) contentView.findViewById(R.id.check_result);
                            display_info.setText(finalInfo);
                        }
                    })
                    .setTitle("Check Result")
                    .create();
            mdDialog.show();
            mdDialog.getWindow().setLayout(1000, 1500);

        } catch (IOException e) {
            e.printStackTrace();
            Toast_in_Thread("Fail to Read TXT File !");
        }


    }


    private void CreateCheckExcel(String excel_filepath, String filepath){

        try{
            File excel_file = new File(excel_filepath);
            File file = new File(filepath);
            if (!excel_file.exists()){
                if (excel_file.createNewFile()){
                    Log.e(TAG,"PullCheckResult create file successfully !");
                }
            }

            if (!file.exists()){
                Log.e(TAG,"CreateCheckExcel Can't open the file");
            }

            ArrayList<String[]> arrayList = new ArrayList<>();
            FileInputStream fid = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fid);
            BufferedReader br = new BufferedReader(isr);
            String str;

            while ((str = br.readLine()) != null) {
                arrayList.add(str.trim().split(" "));
            }
            br.close();
            isr.close();
            fid.close();

            String[] titles = {"Neuron_num", "Arbor_num", "X_start", "X_end", "Y_start", "Y_end", "Z_start", "Z_end",
                                "Check_result (0 for no, 1 for yes, 2 for uncertain)", "Checker", "Check_Time"};

            ExcelUtil.writeExcel(excel_filepath, titles, arrayList);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void Update_Check_Result(){

        makeConnect();

        if (checkConnection()){

            sendMessage("From Android Client :GetArborResult.\n");
            String Store_path_check_txt = Store_path + "/Check/Check_Result";
            String Final_Path = getFile(Store_path_check_txt, true);

            if (Final_Path.equals(EMPTY_MSG) || Final_Path.equals(SOCKET_CLOSED)){
                Toast_in_Thread("Something Wrong When Update_Check_Result");
                return;
            }
            Process_Result(Final_Path);

        }else {
            Toast_in_Thread("Socket disconnect When Update_Check_Result");
            return;
        }

    }


    public void Process_Result(String File_Path){

        ArrayList<String> arraylist = new ArrayList<String>();
        File file = new File(File_Path);

        if (!file.exists()){
            Toast_in_Thread("Fail to Open TXT File !");
            return;
        }

        try {
            FileInputStream fid = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fid);
            BufferedReader br = new BufferedReader(isr);
            String str;
            while ((str = br.readLine()) != null) {
                arraylist.add(str.trim());
            }

            for (int i = 0; i < arraylist.size(); i++) {
                String line = arraylist.get(i);

                String neuron_num = line.split(" ")[0];
                String arbor_num  = "arbor " + line.split(" ")[1];
                String account    = line.split(" ")[9];

                if (Neuron_Arbor_checked.containsKey(neuron_num)){
                    Vector<String> arbor_list = Neuron_Arbor_checked.get(neuron_num);
                    if (!arbor_list.contains(arbor_num)){
                        arbor_list.add(arbor_num);
                    }
                    Neuron_Arbor_checked.put(neuron_num,arbor_list);
                }else {
                    Vector<String> arbor_list_new = new Vector<>();
                    arbor_list_new.add(arbor_num);
                    Neuron_Arbor_checked.put(neuron_num,arbor_list_new);
                }

                if (!Neuron_checked.containsKey(neuron_num)){
                    Neuron_checked.put(neuron_num,account);
                }
            }

            for (String neuron_num : Neuron_Arbor_checked.keySet()){
                if ( (Neuron_Info.get(neuron_num) == null) || !(Neuron_Arbor_checked.get(neuron_num).size() >= Neuron_Info.get(neuron_num).size()-1) ){
                    Neuron_checked.remove(neuron_num);
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
            Toast_in_Thread("Fail to Read TXT File !");
        }

    }



    public void Check_Result(String check_result){

        String result_code = "";

        switch(check_result){
            case "NO":
                result_code = ";0;";
                break;
            case "YES":
                result_code = ";1;";
                break;
            case "UNCERTAIN":
                result_code = ";2;";
                break;
            default:
                Toast_in_Thread("Unsupported check result !");
                return;
        }

        makeConnect();

        if (checkConnection()){

            String boundingbox = Pos_Selected.substring(ordinalIndexOf(Pos_Selected, ";", 3)+1);
            String brain_num = getFilename_Remote(mContext);
            String neuron_num = getNeuronNumber_Remote(mContext, brain_num);

            String result = neuron_num + ";" +boundingbox;
            String Check_Info = result + result_code + getUserAccount_Check(mContext) + ";" +
                    getArborNum(mContext,brain_num.split("/")[0] + "_" + neuron_num).split(":")[0].split(" ")[1] +":ArborCheck.\n";

            Log.e(TAG,Check_Info);
            sendMessage(Check_Info);

        }else {
            Toast_in_Thread("Can't Connect Server, Try Again Later !");
        }

    }


    /**
     * just for backup
     * @param isDrawMode if in DrawMode
     */
    public void Check_Yes(boolean isDrawMode){

        makeConnect();

        if (checkConnection()){

            if (isDrawMode){

                String filename = getFilename_Remote(mContext);
                String neuron_number = getNeuronNumber_Remote(mContext, filename);
                String offset = getoffset_Remote(mContext, filename);
                int[] index = BigImgReader.getIndex(offset);
                System.out.println(filename);

                String ratio = Integer.toString(getRatio_SWC());

                String Check_info = neuron_number + "__" +
                        index[0] + "__" +index[3] + "__" + index[1] + "__" + index[4] + "__" + index[2] + "__" + index[5];

                sendMessage(Check_info + "__" + ratio + "__1" + ":SwcCheck.\n");

            }else {

//                String filename = getFilename_Remote_Check(mContext);
//                String offset = getoffset_Remote_Check(mContext, filename);
//                System.out.println(filename);
                //                String offset_final = offsetSwitch(filename, offset);


                String boundingbox = Pos_Selected.substring(ordinalIndexOf(Pos_Selected, ";", 3)+1);
                String brain_num = getFilename_Remote(mContext);
                String neuron_num = getNeuronNumber_Remote(mContext, brain_num);
//                String result = brain_num.split("_")[0] + "_" + neuron_num.split("_")[0] + "_" + getArborNum(mContext,brain_num.split("RES")[0]);

//                String result = neuron_num + ";" +getBoundingBox(mContext,brain_num);
                String result = neuron_num + ";" +boundingbox;
//                String Check_Info = result + ";1;" + getUserAccount_Check(mContext) + ":ArborCheck.\n";
                String Check_Info = result + ";1;" + getUserAccount_Check(mContext) + ";" +
                        getArborNum(mContext,brain_num.split("/")[0] + "_" + neuron_num).split(":")[0].split(" ")[1] +":ArborCheck.\n";

                Log.e(TAG,Check_Info);
                sendMessage(Check_Info);
            }

        }else {
            Toast_in_Thread("Can't Connect Server, Try Again Later !");
        }

    }

    private float getRatio(String res_new, String res_old){

        String y_new = res_new.split("x")[1];
        String y_old  = res_old.split("x")[1];

        int y_new_int = Integer.parseInt(y_new);
        int y_old_int = Integer.parseInt(y_old);

        float ratio = ((float) y_new_int)/ ((float) y_old_int);

        return ratio;

    }


    public int getRatio_SWC(){

        Vector<String> res_temp = getRES(mContext, BrainNumber_Selected);
        String filename = getFilename_Remote(mContext);
        String res_cur  = filename.split("/")[1];

        String res_new = res_temp.lastElement();
        String res_old = res_cur;

        int ratio = (int) (getRatio(res_new, res_old));

        return ratio;

    }

    private String[] getNewOffset(String offset, float ratio){

        String offset_x = offset.split("_")[0];
        String offset_y = offset.split("_")[1];
        String offset_z = offset.split("_")[2];
        String size     = offset.split("_")[3];

        float offset_x_float = Float.parseFloat(offset_x) * ratio;
        float offset_y_float = Float.parseFloat(offset_y) * ratio;
        float offset_z_float = Float.parseFloat(offset_z) * ratio;

        return new String[]{
                Integer.toString((int) offset_x_float),
                Integer.toString((int) offset_y_float),
                Integer.toString((int) offset_z_float),
                size};

    }

    private String getNewBoundingBox(String boundingbox, float ratio){

        String[] offset = boundingbox.split(";");
        int[] offset_f = new int[offset.length];

        for (int i=0; i<offset.length; i++){
            offset_f[i] = (int) ( Float.parseFloat(offset[i]) * ratio );
            offset[i] = Integer.toString(offset_f[i]);
        }

        String result = offset[0];
        for (int i=1; i<offset.length; i++){
            result += ";" + offset[i];
        }
        return result;

    }

    private String[] transform_offset(){
        String offset = Pos_Selected.split(":")[1];

        Vector<String> res_temp = getRES(mContext, BrainNumber_Selected);
        String filename = getFilename_Remote(mContext);
        String res_cur  = filename.split("/")[1];
        int res_index = res_temp.indexOf(res_cur);

        System.out.println("res_cur: " + res_cur + "---");
        System.out.println("res_highest: " + res_temp.lastElement() + "---");

        if (res_index == res_temp.size()-1){
            return new String[]{Integer.toString((int) Float.parseFloat(offset.split(";")[0])),
                                Integer.toString((int) Float.parseFloat(offset.split(";")[1])),
                                Integer.toString((int) Float.parseFloat(offset.split(";")[2])),
                                "128"};
        }

        float ratio = getRatio(res_cur, res_temp.lastElement());
        String[] offset_result = getNewOffset(offset.replaceAll(";","_") + "_128", ratio);
        return offset_result;

    }


    // ----------------------------------------------------------------------------------------------------------

    private void sendMessage(String message) {
        makeConnect();

        if (checkConnection()){
            socket_send.Send_Message(ManageSocket, message);
        }else {
            Toast_in_Thread("Can't Connect Server, Try Again Later !");
        }

    }

    private void sendFile(String filename, InputStream is, long length_content){
        makeConnect();

        if (checkConnection()){
            socket_send.Send_File(ManageSocket, filename, is, length_content);
        }else {
            Toast_in_Thread("Can't Connect Server, Try Again Later !");
        }
    }


    private String getMessage(){

        if (ManageSocket == null || ManageSocket.isClosed()){
            Log.d("Get_Message","ManageSocket.isClosed()");
            return SOCKET_CLOSED;
        }

        String msg = socket_receive.Get_Message(ManageSocket);

        if (msg != null)
            return msg;
        else
            return EMPTY_MSG;
    }


    private String getFile(String file_path, boolean Need_Waited){

        return socket_receive.Get_File(ManageSocket, file_path, Need_Waited);

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void getImg(String file_path, boolean Need_Waited){

        socket_receive.Get_Block(ManageSocket, file_path, Need_Waited);

    }


    private void makeConnect(){

        if (ManageSocket == null || !checkConnection()){
            Log.e("Make_Connect","Connect Again");
            connectServer(ip);
        }

    }

    private boolean checkConnection(){

        return ManageSocket!= null && ManageSocket.isConnected() && !ManageSocket.isClosed();

    }


    // ----------------------------------------------------------------------------------------------------------


    /**
     * judge if the offset of the block is even
     * @param offset_x offset of x
     * @param offset_y offset of y
     * @param offset_z offset of z
     * @param size size of the block
     * @return the new offset
     */
    private String[] judgeEven(String offset_x, String offset_y, String offset_z, String size){

        float x_offset_f = Float.parseFloat(offset_x);
        float y_offset_f = Float.parseFloat(offset_y);
        float z_offset_f = Float.parseFloat(offset_z);
        float size_f     = Float.parseFloat(size);

        int[] offset_i = new int[]{(int) x_offset_f, (int) y_offset_f,
                                (int) z_offset_f, (int) size_f};


        for(int i = 0; i < offset_i.length; i++){
            if (offset_i[i] % 2 == 1)
                offset_i[i] += 1;
        }

        String[] result = new String[4];
        for (int i = 0; i < result.length; i++){
            result[i] = Integer.toString(offset_i[i]);
        }

        Log.e("JudgeEven", Arrays.toString(result));

        return result;
    }



    /**
     * judge the if the bounding box out of range
     * @param offsets
     * @return true : the bounding box
     */
    private boolean judgeBounding(String[] offsets){

        int[] offset_i = new int[4];
        for (int i = 0; i < 4; i++){
            offset_i[i] = Integer.parseInt(offsets[i]);
        }

        String filename_root = getFilename_Remote(mContext);
        String filename = filename_root.replace(")","").replace("(","");
        String size = filename.split("RES")[1];

        System.out.println("hhh-------" + size + "--------hhh");

        // current filename mouse18864_teraconvert/RES(35001x27299x10392)   : y x z
        int[] img_size = new int[3];
        img_size[0] = Integer.parseInt(size.split("x")[1]);
        img_size[1] = Integer.parseInt(size.split("x")[0]);
        img_size[2] = Integer.parseInt(size.split("x")[2]);

        for (int i=0; i<3; i++){
            if ((offset_i[i] < offset_i[3]/2 ) && (offset_i[i] > img_size[i] -1 - offset_i[3]/2))
                return false;

            if (offset_i[i] <= offset_i[3]/2){
                offset_i[i] = offset_i[3]/2 + 1;
                offsets[i] = Integer.toString(offset_i[i]);
            }

            if (offset_i[i] > img_size[i] -1 - offset_i[3]/2){
                offset_i[i] = img_size[i] -1 - offset_i[3]/2;
                offsets[i] = Integer.toString(offset_i[i]);
            }

        }

        String offset = offsets[0] + "_" + offsets[1] + "_" + offsets[2] + "_" +offsets[3];
        setoffset_Remote(offset, filename_root, mContext);

        Log.e(TAG, offset);

        return true;
    }


    /**
     * disconnect socket connection
     */
    public void disConnectFromHost(){

        System.out.println("---- Disconnect from Host ----");
        try {

            if (ManageSocket != null){
                ManageSocket.close();
            }

            if (ImgReader != null){
                ImgReader.close();
            }

            if (ImgPWriter != null){
                ImgPWriter.close();
            }

        } catch (IOException e) {

            e.printStackTrace();
        }

    }



    public void analyzeTXT(String File_Path){
        ArrayList<String> arraylist = new ArrayList<String>();
        File file = new File(File_Path);

        if (!file.exists()){
            Toast_in_Thread("Fail to Open TXT File !");
            return;
        }

        try {
            FileInputStream fid = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fid);
            BufferedReader br = new BufferedReader(isr);
            String str;
            while ((str = br.readLine()) != null) {
                arraylist.add(str.trim());
            }

            Vector<String> RES_List_temp = new Vector<>();
            Vector<String> Neuron_Number_List_temp = new Vector<>();
            HashMap<String, Vector<String>> Neuron_Info_temp = new HashMap<>();

            for (int i = 0; i < arraylist.size(); i++){
                String line = arraylist.get(i);

                if (line.startsWith("#RES")){
                    String[] split = line.split(":");
                    int num = Integer.parseInt(split[1]);
                    for (int j = 0; j < num; j++){
                        i++;
                        String RES = arraylist.get(i);
                        RES_List_temp.add(RES.split(":")[1]);
                    }
                }

                if (line.startsWith("#Neuron_number")){
                    String[] split = line.split(":");
                    int num = Integer.parseInt(split[1]);
                    for (int j = 0; j < num; j++){
                        i++;
                        String Number = arraylist.get(i);
                        Neuron_Number_List_temp.add(Number.split(":")[1]);
                    }
                }

                if (line.startsWith("##")){
                    String neuron_number = line.substring(2);
                    Vector<String> point_list = new Vector<>();
                    point_list.add(arraylist.get(++i));

                    String[] marker = arraylist.get(i).split(":")[1].split(";");
                    marker_List.add(Integer.parseInt(marker[0]));
                    marker_List.add(Integer.parseInt(marker[1]));
                    marker_List.add(Integer.parseInt(marker[2]));

                    String[] split = arraylist.get(++i).split(":");
                    int num = Integer.parseInt(split[1]);
                    for (int j = 0; j < num; j++){
                        i++;
                        String offset = arraylist.get(i).split(":")[1];
                        point_list.add("arbor " + (j+1) + ":" + offset);
                        Log.e("Neuron_Info: ", " " + Neuron_Info_temp.size());

                    }

                    Neuron_Info_temp.put(neuron_number, point_list);

                }

//                Log.e(TAG,Arrays.toString(Transform(RES_List,0,RES_List.size())));
//                Log.e(TAG,Arrays.toString(Transform(Neuron_Number_List,0,Neuron_Number_List.size())));

                RES_List = RES_List_temp;
                Neuron_Number_List = Neuron_Number_List_temp;
                Neuron_Info = Neuron_Info_temp;

                setRES(RES_List.toArray(new String[RES_List.size()]), BrainNumber_Selected, mContext);

            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast_in_Thread("Fail to Read TXT File !");
        }


    }



    public void loadNeuronTxt(boolean isDrawMode){

        this.isDrawMode = isDrawMode;
        Store_path = mContext.getExternalFilesDir(null).toString();

        String Store_path_txt = Store_path + "/BrainInfo/";
        String filename_root = getFilename_Remote(mContext);
        String[] filename_break = filename_root.split("/");

        String info_txt = "";
        if (isDrawMode){
            info_txt = Store_path_txt + filename_break[0] + ".txt";
        }else {
            info_txt = Store_path_txt + "check" + filename_break[0] + ".txt";
        }

        analyzeTXT(info_txt);

        BrainNumber_Selected = filename_break[0];
        RES_Selected = filename_break[1];
        Neuron_Number_Selected = getNeuronNumber_Remote(mContext,filename_root);

    }

    public void loadArborTxt(){

        Arbor_Check_List = getArbor_List__Check(mContext);

    }



    public ArrayList<ArrayList<Integer>> getMarker(int[] index){

        ArrayList<ArrayList<Integer>> marker_list = new ArrayList<ArrayList<Integer>>();

        float ratio = getRatio_SWC();
        Log.e("getMarker", "ratio: " + ratio);

        String[] soma_index = Neuron_Info.get(Neuron_Number_Selected).get(0).split(":")[1].split(";");

        int x = (int) ( Integer.parseInt(soma_index[0]) / ratio );
        int y = (int) ( Integer.parseInt(soma_index[1]) / ratio );
        int z = (int) ( Integer.parseInt(soma_index[2]) / ratio );

        ArrayList<Integer> marker = new ArrayList<Integer>();
        marker.add(x - index[0]);
        marker.add(y - index[1]);
        marker.add(z - index[2]);
        marker_list.add(marker);

//        for (int i = 0; i < Marker_List.size(); i = i + 3){
//            int x = (int) ( Marker_List.get(i)   / (float) ratio);
//            int y = (int) ( Marker_List.get(i+1) / (float) ratio);
//            int z = (int) ( Marker_List.get(i+2) / (float) ratio);
//
//            Log.e("Remote_Socket", Neuron_Info.get(Neuron_Number_Selected).get(0));
//
//
//
////            Log.e("getMarker","(" + x + ", " + y + ", " + z + ")");
//
//            if (x >= index[0] && y >= index[1] && z>= index[2] &&
//                x < index[3] && y < index[4] && z < index[5]){
//                ArrayList<Integer> marker = new ArrayList<Integer>();
//                marker.add(x - index[0]);
//                marker.add(y - index[1]);
//                marker.add(z - index[2]);
//                marker_list.add(marker);
//
//                Log.e("getMarker","(" + x + ", " + y + ", " + z + ")");
//
//            }
//        }

        Log.e("getMarker",marker_list.size() + " !");
        Log.e("getMarker", marker_List.size() + " !");
        return marker_list;

    }


    public void backup(){
        String fileName = getFilename_Remote(mContext);
        String neuronNum = getNeuronNumber_Remote(mContext,fileName);
        String offset = getoffset_Remote(mContext,fileName);

        socket_receive.setFileName_Backup(fileName);
        socket_receive.setNeuronNum_Backup(neuronNum);
        socket_receive.setOffset_Backup(offset);
    }


    public void switchRES(){

        Vector<String> res_temp = getRES(mContext, BrainNumber_Selected);
        String filename = getFilename_Remote(mContext);   // BrainNumber: 18465/RES250x250x250
        String res_cur  = filename.split("/")[1];   // RES: RES250x250x250
        int res_index = res_temp.indexOf(res_cur);
        res_temp.set(res_index, res_temp.get(res_index) + "   √");

        new XPopup.Builder(mContext)
        .maxWidth(850)
//        .maxHeight(1350)
                .asCenterList("Select a RES", Transform(res_temp,0, res_temp.size()),
                        new OnSelectListener() {
                            @RequiresApi(api = Build.VERSION_CODES.N)
                            @Override
                            public void onSelect(int position, String text) {
                                switchRES(res_cur, text.replace("   √",""));
                            }
                        })
                .show();

    }


    private String offsetSwitch(String filename, String offset_old){

        String offset_neuron = filename.split("RES")[1];
        System.out.println("offset_neuron" + offset_neuron);
        int offset_start_x = Integer.parseInt(offset_neuron.split("__")[1]);
        int offset_start_y = Integer.parseInt(offset_neuron.split("__")[3]);
        int offset_start_z = Integer.parseInt(offset_neuron.split("__")[5]);

        String[] offset = offset_old.split(";");
        String offset_result = "";

        for (int i=0; i<6; i++){

            int bais = 0;
            switch (i+1){
                case 1:
                case 2:
                    bais = offset_start_x;
                    break;
                case 3:
                case 4:
                    bais = offset_start_y;
                    break;
                case 5:
                case 6:
                    bais = offset_start_z;
                    break;
            }

            offset_result = offset_result + ";" + Integer.toString(Integer.parseInt(offset[i]) + bais - 1);

        }

        return offset_result;

    }



    public String getIp(){
        makeConnect();

        if (checkConnection()){
            return this.ip;
        }

        return "Can't connect server";

    }

    String[] Transform(Vector<String> strings, int start, int end){

        String[] string_list = new String[end - start];
        int j = 0;
        for (int i = start; i < end; i++) {
                string_list[j++] = strings.get(i);
        }
        return string_list;
    }

    private String[] ellipsize(String[] strings){

        String[] string_list = new String[strings.length];

        for (int i=0; i<strings.length; i++){
            String item = strings[i];
            if (item.length() > 22){
                string_list[i] = item.substring(0,10) + "..." + item.substring(item.length()-10);
            }else {
                string_list[i] = item;
            }
        }
        return string_list;
    }

    /**
     * toast info in the thread
     * @param message the message you wanna toast
     */
    public void Toast_in_Thread(String message){
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, message,Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * get the activity
     * @param context current context
     * @return current activity
     */
    @Nullable
    private static Activity getActivity(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        if (context instanceof ContextWrapper) {
            ContextWrapper wrapper = (ContextWrapper) context;
            return getActivity(wrapper.getBaseContext());
        } else {
            return null;
        }
    }

    public static int ordinalIndexOf(String str, String substr, int n) {
        int pos = str.indexOf(substr);
        while (--n > 0 && pos != -1)
            pos = str.indexOf(substr, pos + 1);
        return pos;
    }

}
