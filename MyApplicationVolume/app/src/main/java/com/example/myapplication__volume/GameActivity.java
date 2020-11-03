package com.example.myapplication__volume;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;

import com.example.datastore.SettingFileManager;
import com.example.server_communicator.Remote_Socket;
import com.example.game.GameCharacter;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.interfaces.OnCancelListener;
import com.lxj.xpopup.interfaces.OnConfirmListener;
import com.lxj.xpopup.interfaces.OnSelectListener;
import com.tracingfunc.gd.V_NeuronSWC;
import com.tracingfunc.gd.V_NeuronSWC_unit;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class GameActivity extends BaseActivity {

    public static final String NAME = "com.example.myapplication__volume.GameActivity";

    private static final int SHOW_PROGRESSBAR = 1;
    private static final int HIDE_PROGRESSBAR = 2;
    private static MyRenderer myrenderer;

    private static MyGLSurfaceView myGLSurfaceView;

    private String filepath;
    private static BasePopupView progressBar;

//    private float[] position;
//    private float[] dir;
//    private float[] head;
    private GameCharacter gameCharacter;

    private float [] moveDir;
    private float [] viewRotateDir;

    private static Context gameContext;

    private Timer timer;
    private TimerTask task;

    private V_NeuronSWC travelPath;

    private float [] lastPlace;

    private Remote_Socket remoteSocket;

    final private static String TAG = "GAMEACTIVITY";

    @SuppressLint("HandlerLeak")
    private static Handler puiHandler = new Handler(){
        // 覆写这个方法，接收并处理消息。
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case SHOW_PROGRESSBAR:
                    progressBar.show();
                    break;

                case HIDE_PROGRESSBAR:
                    progressBar.dismiss();
                    break;

                default:
                    Toast.makeText(gameContext,"Something Wrong in puiHandler !",Toast.LENGTH_SHORT).show();
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        filepath = extras.getString("FilePath");
        float [] position = extras.getFloatArray("Position");
        float [] dir = extras.getFloatArray("Dir");
        float [] head = MyRenderer.locateHead(dir[0], dir[1], dir[2]);

        progressBar = new XPopup.Builder(this).asLoading("Downloading...");

        gameContext = this;

        setContentView(R.layout.activity_game);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        myrenderer = new MyRenderer(this);
        myrenderer.setPath(filepath);
        myrenderer.resetContrast(preferenceSetting.getContrast());
//        myrenderer.setGamePosition(position);
//        myrenderer.setGameDir(dir);
//        myrenderer.setGameHead(head);

        gameCharacter = new GameCharacter(position, dir, head);
        myrenderer.setGameCharacter(gameCharacter);

        remoteSocket = new Remote_Socket(this);

//        myrenderer.addMarker(position);
        myrenderer.setIfGame(true);

        myGLSurfaceView = new GameActivity.MyGLSurfaceView(this);
        FrameLayout ll = (FrameLayout) findViewById(R.id.gameContainer);
        ll.addView(myGLSurfaceView);

        MyRockerView rockerView1 = (MyRockerView)findViewById(R.id.rockerView1);

        FrameLayout.LayoutParams lp_rocker1 = new FrameLayout.LayoutParams(300, 300);
        lp_rocker1.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        lp_rocker1.setMargins(0, 0, 20, 20);
        ll.removeView(rockerView1);
        this.addContentView(rockerView1, lp_rocker1);

        MyRockerView rockerView2 = (MyRockerView)findViewById(R.id.rockerView2);

        FrameLayout.LayoutParams lp_rocker2 = new FrameLayout.LayoutParams(300, 300);
        lp_rocker2.gravity = Gravity.BOTTOM | Gravity.LEFT;
        lp_rocker2.setMargins(20, 0, 0, 20);
        ll.removeView(rockerView2);
        this.addContentView(rockerView2, lp_rocker2);

//        Button Check_Yes = new Button(this);
//        Check_Yes.setText("Y");
//
//        FrameLayout.LayoutParams lp_check_yes = new FrameLayout.LayoutParams(120, 120);
//        lp_check_yes.gravity = Gravity.BOTTOM | Gravity.RIGHT;
//        lp_check_yes.setMargins(0, 0, 20, 500);
//        this.addContentView(Check_Yes, lp_check_yes);
//
//        Check_Yes.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                boolean [] b = {false};
//                timer = new Timer();
//                task = new TimerTask() {
//                    @Override
//                    public void run() {
//                        System.out.println("AAAAA");
//                        try {
//                            b[0] = true;
//                            System.out.print(moveDir[0]);
//                            System.out.print(' ');
//                            System.out.print(moveDir[1]);
//
//                            System.out.print(viewRotateDir[0]);
//                            System.out.print(' ');
//                            System.out.print(viewRotateDir[1]);
//                        } catch (Exception e){
//                            e.printStackTrace();
//                        }
//                    }
//                };
//                timer.schedule(task, 0, 1000);
//                if (b[0]){
//                    System.out.println("HHHHHHHHHHHH");
//                }
//            }
//        });

        moveDir = new float[]{0f, 0f, 0f};
        viewRotateDir = new float[]{0f, 0f, 0f};

        travelPath = new V_NeuronSWC();
        V_NeuronSWC_unit startPoint = new V_NeuronSWC_unit();
        startPoint.n = 0;
        startPoint.parent = -1;
        float [] startPlace = myrenderer.modeltoVolume(position);
        startPoint.x = startPlace[0];
        startPoint.y = startPlace[1];
        startPoint.z = startPlace[2];
        startPoint.type = 2;
        travelPath.append(startPoint);

        lastPlace = new float[]{position[0], position[1], position[2]};

        rockerView1.setRockerChangeListener(new MyRockerView.RockerChangeListener() {
            @Override
            public void report(float x, float y) {

                moveDir[0] = x;
                moveDir[1] = y;
//                System.out.println(x);
//                System.out.println(y);
            }
        });

        rockerView2.setRockerChangeListener(new MyRockerView.RockerChangeListener() {
            @Override
            public void report(float x, float y) {

                viewRotateDir[0] = x;
                viewRotateDir[1] = y;
            }
        });

//        final boolean[] b = {false};
//
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                try {
                    float angleH = viewRotateDir[0] / 100;
                    float angleV = viewRotateDir[1] / 100;
                    float x = moveDir[0] / 10000;
                    float y = moveDir[1] / 10000;
//                    System.out.println("SSSSSSSSSSSSSSSS");
//                    System.out.print(viewRotateDir[0]);
//                    System.out.print(' ');
//                    System.out.print(moveDir[0]);
//                    System.out.print(' ');
//                    System.out.println(moveDir[1]);
                    if (angleH != 0 || angleV != 0 || x != 0 || y != 0) {
                        gameCharacter.rotateDir(angleH, angleV);
//                        float[] dirE = new float[]{dir[0], dir[1], dir[2], 1};
//                        float[] headE = new float[]{head[0], head[1], head[2], 1};
//                        float[] axisV = new float[3];
//                        if (angleH != 0 && angleV != 0) {
////                        float[] head = MyRenderer.locateHead(dir[0], dir[1], dir[2]);
////                        float[] head = new float[]{0, 1, 0};
////                        float[] axisV = new float[]{dir[1] * head[2] - dir[2] * head[1], dir[2] * head[0] - dir[0] * head[2], dir[0] * head[1] - dir[1] * head[0]};
//
//                            float[] rotationHMatrix = new float[16];
//                            float[] rotationVMatrix = new float[16];
////                        float[] rotationMatrix = new float[16];
//
////                            if (angleH > 0) {
//                            Matrix.setRotateM(rotationHMatrix, 0, -angleH, head[0], head[1], head[2]);
////                            } else {
////                                Matrix.setRotateM(rotationHMatrix, 0, -angleH, -head[0], -head[1], -head[2]);
////                            }
//
////                        float[] dirE = new float[]{dir[0], dir[1], dir[2], 1};
////                        float[] headE = new float[]{head[0], head[1], head[2], 1};
////                        float[] dirF = new float[4];
//                            Matrix.multiplyMV(dirE, 0, rotationHMatrix, 0, dirE, 0);
//
//                            axisV = new float[]{dirE[1] * head[2] - dirE[2] * head[1], dirE[2] * head[0] - dirE[0] * head[2], dirE[0] * head[1] - dirE[1] * head[0]};
//
////                            if (angleV > 0) {
//                            Matrix.setRotateM(rotationVMatrix, 0, -angleV, axisV[0], axisV[1], axisV[2]);
////                            } else {
////                                Matrix.setRotateM(rotationVMatrix, 0, -angleV, -axisV[0], -axisV[1], -axisV[2]);
////                            }
//                            Matrix.multiplyMV(dirE, 0, rotationVMatrix, 0, dirE, 0);
//                            Matrix.multiplyMV(headE, 0, rotationVMatrix, 0, headE, 0);
//
//
//                            dir = new float[]{dirE[0], dirE[1], dirE[2]};
//                            head = new float[]{headE[0], headE[1], headE[2]};
//                        }

                        if (x != 0 && y != 0) {
                            gameCharacter.movePosition(x, y);
//                            axisV = new float[]{dirE[1] * head[2] - dirE[2] * head[1], dirE[2] * head[0] - dirE[0] * head[2], dirE[0] * head[1] - dirE[1] * head[0]};
//                            float XL = (float)Math.sqrt(axisV[0] * axisV[0] + axisV[1] * axisV[1] + axisV[2] * axisV[2]);
//                            float [] X = new float[]{axisV[0] / XL, axisV[1] / XL, axisV[2] / XL};
//                            float YL = (float)Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
//                            float [] Y = new float[]{dir[0] / YL, dir[1] / YL, dir[2] / YL};
//
//                            position[0] = position[0] + X[0] * x - Y[0] * y;
//                            position[1] = position[1] + X[1] * x - Y[1] * y;
//                            position[2] = position[2] + X[2] * x - Y[2] * y;

                            if (gameCharacter.closeToBoundary()){
                                remoteSocket.disConnectFromHost();
                                remoteSocket.connectServer(ip_SEU);
                                remoteSocket.PullImageBlock_Dir(context, gameCharacter.getDir());
//                                Thread.sleep(8000);
                                float [] volumnePosition = myrenderer.modeltoVolume(gameCharacter.getPosition());
                                float [] dis = new float[]{64 - volumnePosition[0], 64 - volumnePosition[1], 64 - volumnePosition[2]};
                                Log.v("DISSSSS", Arrays.toString(dis));
                                gameCharacter.setPosition(new float[]{0.5f, 0.5f, 0.5f});

                                travelPath.move(dis);

                                myrenderer.clearCurSwcList();
                                myrenderer.addSwc(travelPath);
//                                myrenderer.clearCurSwcList();
//                                myrenderer.addSwc(travelPath);

//                                myrenderer.clearMarkerList();
//                                myrenderer.addMarker(new float[]{0.5f, 0.5f, 0.5f});

                                lastPlace = new float[]{0.5f, 0.5f, 0.5f};
                            } else {

                                float[] position = gameCharacter.getPosition();

//                                myrenderer.clearMarkerList();
//                                myrenderer.addMarker(position);

//                                if (((position[0] - lastPlace[0]) * (position[0] - lastPlace[0])
//                                        + (position[1] - lastPlace[1]) * (position[1] - lastPlace[1])
//                                        + (position[2] - lastPlace[2]) * (position[2] - lastPlace[2])) > 0.001) {

                                V_NeuronSWC_unit newPoint = new V_NeuronSWC_unit();
                                newPoint.parent = travelPath.nrows() - 1;
                                newPoint.n = travelPath.nrows();
                                newPoint.type = 2;
                                float[] newPlace = myrenderer.modeltoVolume(position);
                                newPoint.x = newPlace[0];
                                newPoint.y = newPlace[1];
                                newPoint.z = newPlace[2];
                                travelPath.append(newPoint);

                                myrenderer.clearCurSwcList();
                                myrenderer.addSwc(travelPath);

                                lastPlace = new float[]{position[0], position[1], position[2]};
//                                }
                            }
                        }

//                        myrenderer.moveBlock(x, 0, -y);

//                        myrenderer.setGameDir(gameCharacter.getDir());
//                        myrenderer.setGameHead(gameCharacter.getHead());
//                        myrenderer.setGamePosition(gameCharacter.getPosition());
                        myrenderer.setGameCharacter(gameCharacter);

                        myGLSurfaceView.requestRender();
                    }

                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(task, 0, 100);
    }

    @Override
    protected void onPause() {
        super.onPause();
        myGLSurfaceView.onPause();
        Log.v("onPause", "start-----");
    }

    @Override
    protected void onResume() {
        super.onResume();
        myGLSurfaceView.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game_menu, menu);
        return true;
    }

    /**
     * call the corresponding function when button in top bar clicked
     * @param item
     * @return
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.back:
                timer.cancel();
                task.cancel();
                MainActivity.setIfGame(false);
                finish();
                return true;

            case R.id.save_game:
                archiveList(true);
//                if (saveGame(0))
//                    Toast_in_Thread("Saved Successfully");
//                else
//                    Toast_in_Thread("Failed To Save!!!");
                return true;

            default:
                return true;
        }
    }

    public static void showProgressBar(){
        puiHandler.sendEmptyMessage(SHOW_PROGRESSBAR);
    }

    public static void hideProgressBar(){
        puiHandler.sendEmptyMessage(HIDE_PROGRESSBAR);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void LoadBigFile_Remote(String filepath){

        myrenderer.setPath(filepath);
//        myGLSurfaceView.requestRender();

    }

    private void archiveList(boolean saving){
        String externalFileDir = context.getExternalFilesDir(null).toString();
        String [] fileList = {"[Empty Archive]", "[Empty Archive]", "[Empty Archive]", "[Empty Archive]", "[Empty Archive]", "[Empty Archive]", "[Empty Archive]", "[Empty Archive]", "[Empty Archive]", "[Empty Archive]"};
        File file = new File(externalFileDir + "/Game/Archives");
        if (file.exists()){
            try {
                for (int i = 0; i < 10; i++) {
                    File tempFile = new File(externalFileDir + "/Game/Archives/Archive_" + i);
                    if (!tempFile.exists()) {
                        tempFile.mkdir();
                    } else {
                        File [] archiveFiles = tempFile.listFiles();
                        if (archiveFiles.length > 0){
                            fileList[i] = archiveFiles[0].getName().split(".txt")[0];
                        }
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }

//            File[] tempList = file.listFiles();

//            for (int i = 0; i < tempList.length; i++){
//                if (tempList[i].isDirectory()){
//                    if (Pattern.matches("Archive_[0-9]", tempList[i].getName())){
//                        File [] archiveFile = tempList[i].listFiles();
//                        if (archiveFile.length != 0){
//                            fileList[i] = archiveFile[0].getName();
//                        }
//                    }
//                }
//            }
        } else {
            File parent = file.getParentFile();
            if (!parent.exists()){
                parent.mkdir();
            }
            file.mkdir();
            for (int i = 0; i < 10; i++){
                File tempFile = new File(externalFileDir + "/Game/Archives/Archive_" + i);
                tempFile.mkdir();
            }
        }

        new XPopup.Builder(this)
                .asCenterList("Archives", fileList,
                        new OnSelectListener() {
                            @Override
                            public void onSelect(int position, String text) {
                                if (text.equals("[Empty Archive]")){
                                    if (saving){
                                        if (saveGame(position))
                                            Toast_in_Thread("Saved Successfully");
                                        else
                                            Toast_in_Thread("Failed To Save!!!");
                                    }
                                } else {
                                    if (saving){
                                        new XPopup.Builder(gameContext)
                                                .dismissOnTouchOutside(false)
                                                .asConfirm("Archive", "Are you sure to overwrite this archive?", "Cancel", "Confirm",
                                                        new OnConfirmListener() {
                                                            @Override
                                                            public void onConfirm() {
                                                                if (saveGame(position))
                                                                    Toast_in_Thread("Saved Successfully");
                                                                else
                                                                    Toast_in_Thread("Failed To Save!!!");
                                                            }
                                                        },
                                                        new OnCancelListener() {
                                                            @Override
                                                            public void onCancel() {
                                                                archiveList(true);
                                                            }
                                                        },false).show();
                                    }
                                }
                            }
                        }).show();
    }

    public boolean saveGame(int num){
        String filename_root = SettingFileManager.getFilename_Remote(context);
        String offset = SettingFileManager.getoffset_Remote(context, filename_root);

        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");// a为am/pm的标记
        Date date = new Date();// 获取当前时间
        String date_str = sdf.format(date);

        float x_pos = gameCharacter.getPosition()[0];
        float y_pos = gameCharacter.getPosition()[1];
        float z_pos = gameCharacter.getPosition()[2];

        float x_dir = gameCharacter.getDir()[0];
        float y_dir = gameCharacter.getDir()[1];
        float z_dir = gameCharacter.getDir()[2];

        String pos_str = Float.toString(x_pos) + ' ' + Float.toString(y_pos) + ' ' + Float.toString(z_pos);
        String dir_str = Float.toString(x_dir) + ' ' + Float.toString(y_dir) + ' ' + Float.toString(z_dir);

        String externalFileDir = context.getExternalFilesDir(null).toString();
        String str = filename_root + '\n' + offset + '\n' + pos_str + '\n' + dir_str;
//        File file = new File(externalFileDir + "/Game/Archives/" + "Archive_" + num + "/" + date_str + ".txt");
        File file = new File(externalFileDir + "/Game/Archives/" + "Archive_" + num);
        if (!file.exists()){
            try {
//                File dir = new File(file.getParent());
//                dir.mkdirs();
//                file.createNewFile();
                file.mkdir();

//                String str = filename_root + '\n' + offset + '\n' + pos_str + '\n' + dir_str;

            }catch (Exception e){
                Log.v(TAG, "failed to create archive dir");
                e.printStackTrace();
                return false;
            }
        }

        File [] oldFiles = file.listFiles();
        for (int i = 0; i < oldFiles.length; i++){
            oldFiles[i].delete();
        }

        File archiveFile = new File(externalFileDir + "/Game/Archives/" + "Archive_" + num + "/" + date_str + ".txt");

        try {
            archiveFile.createNewFile();
        } catch (Exception e) {
            Log.v(TAG, "failed to create archive file");
            e.printStackTrace();
            return false;
        }

        try {
            FileOutputStream outStream = new FileOutputStream(archiveFile);
            outStream.write(str.getBytes());
            outStream.close();
        } catch (Exception e){
            Log.v(TAG, "failed to write archive");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    class MyGLSurfaceView extends GLSurfaceView {

        public MyGLSurfaceView(Context context) {
            super(context);

            ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
            ConfigurationInfo info = am.getDeviceConfigurationInfo();
            String v = info.getGlEsVersion(); //判断是否为3.0 ，一般4.4就开始支持3.0版本了。

            Log.v("GameActivity", "GLES-version: " + v);

            //设置一下opengl版本；
            setEGLContextClientVersion(3);

//            myrenderer.setLineDrawed(lineDrawed);

            setRenderer(myrenderer);


            //调用 onPause 的时候保存EGLContext
            setPreserveEGLContextOnPause(true);

            //当发生交互时重新执行渲染， 需要配合requestRender();
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        }

    }

    public void onDestroy() {
        super.onDestroy();

        timer.cancel();
        task.cancel();

        gameContext = null;
    }
}