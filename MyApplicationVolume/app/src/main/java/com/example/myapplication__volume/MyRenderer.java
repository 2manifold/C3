package com.example.myapplication__volume;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.GLES10;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.basic.ByteTranslate;
import com.example.basic.FastMarching_Linker;
import com.example.basic.FileManager;
import com.example.basic.Image4DSimple;
import com.example.basic.ImageMarker;
import com.example.basic.ImageUtil;
import com.example.basic.MarkerList;
import com.example.basic.MyAnimation;
import com.example.basic.NeuronTree;
import com.example.basic.XYZ;
import com.example.game.GameCharacter;
import com.example.myapplication__volume.FileReader.AnoReader;
import com.example.myapplication__volume.FileReader.ApoReader;
import com.example.myapplication__volume.Rendering.MyAxis;
import com.example.myapplication__volume.Rendering.MyDraw;
import com.example.myapplication__volume.Rendering.MyMarker;
import com.example.myapplication__volume.Rendering.MyNavLoc;
import com.example.myapplication__volume.Rendering.MyPattern;
import com.example.myapplication__volume.Rendering.MyPattern2D;
import com.tracingfunc.cornerDetection.HarrisCornerDetector;
import com.tracingfunc.gd.V_NeuronSWC;
import com.tracingfunc.gd.V_NeuronSWC_list;
import com.tracingfunc.gd.V_NeuronSWC_unit;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.example.basic.BitmapRotation.getBitmapDegree;
import static com.example.basic.BitmapRotation.rotateBitmapByDegree;
import static com.example.myapplication__volume.Myapplication.getContext;
import static javax.microedition.khronos.opengles.GL10.GL_ALPHA_TEST;
import static javax.microedition.khronos.opengles.GL10.GL_BLEND;
import static javax.microedition.khronos.opengles.GL10.GL_ONE_MINUS_SRC_ALPHA;
import static javax.microedition.khronos.opengles.GL10.GL_SRC_ALPHA;

//import android.graphics.Matrix;
//import org.apache.commons.io.IOUtils;
//import org.opencv.android.Utils;
//import org.opencv.core.CvType;
//import org.opencv.core.Mat;
//import org.opencv.core.MatOfPoint;
//import org.opencv.core.Point;
//import org.opencv.imgproc.Imgproc;


//@android.support.annotation.RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
public class MyRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "MyRenderer";
    private int UNDO_LIMIT = 20;
    private int curUndo = 0;
    private enum Operate {DRAWCURVE, DELETECURVE, DRAWMARKER, DELETEMARKER, CHANGELINETYPE, SPLIT};
    private Vector<Operate> process = new Vector<>();
    private Vector<V_NeuronSWC> undoDrawList = new Vector<>();
    private Vector<Vector<V_NeuronSWC>> undoDeleteList = new Vector<>();
    private Vector<ImageMarker> undoDrawMarkerList = new Vector<>();
    private Vector<ImageMarker> undoDeleteMarkerList = new Vector<>();
    private Vector<Vector<Integer>> undoChangeLineTypeIndex = new Vector<>();
    private Vector<Vector<Integer>> undoLineType = new Vector<>();
    private ArrayList<MarkerList> undoMarkerList = new ArrayList<>();
//    private ArrayList<ArrayList<ImageMarker>> undoMarkerList = new ArrayList<>();
    private ArrayList<V_NeuronSWC_list> undoCurveList = new ArrayList<>();

    public static final String OUT_OF_MEMORY = "OutOfMemory";
    public static final String FILE_SUPPORT_ERROR = "FileSupportError";
    public static final String FILE_PATH = "Myrender_FILEPATH";
    public static final String LOCAL_FILE_PATH = "LOCAL_FILEPATH";
    public static final String Time_out = "Myrender_Timeout";

    private MyPattern myPattern;
    private MyPattern2D myPattern2D;
    private MyAxis myAxis;
    private MyDraw myDraw;
    public  MyAnimation myAnimation;
    private MyNavLoc myNavLoc;
    private ByteTranslate byteTranslate;

    private Image4DSimple img = null;
    private ByteBuffer imageBuffer;
    private byte [] image2D;
    private Bitmap bitmap2D = null;

    private int mProgram;

    //    private boolean ispause = false;
    private float angle = 0f;
    private float angleX = 0.0f;
    private float angleY = 0.0f;
    private float angleZ = 0.0f;
    private int mTextureId;

    private int vol_w;
    private int vol_h;
    private int vol_d;
    private int[] sz = new int[3];
    private float[] mz = new float[3];
    private float[] mz_neuron = new float[3];
    private float[] mz_block = new float[6];


    private int[] texture = new int[1]; //生成纹理id

    // vPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] scratch = new float[16];
    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] rotationMatrix =new float[16];
    private final float[] rotationXMatrix = new float[16];
    private final float[] rotationYMatrix = new float[16];
    private final float[] rotationZMatrix = new float[16];
    private final float[] translateMatrix = new float[16];//平移矩阵
    private final float[] translateAfterMoveMatrix = new float[16];
    private final float[] translateAfterMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] TMMatrix = new float[16];
    private final float[] RTMatrix = new float[16];
    private final float[] ZRTMatrix = new float[16];
    private final float[] mMVP2DMatrix = new float[16];
    private final float[] translateSmallMapMatrix = new float[16];
    private final float[] zoomSmallMapMatrix = new float[16];
    private final float[] finalSmallMapMatrix = new float[16];
    private float[] ArotationMatrix = new float[16];


    private final float[] zoomMatrix = new float[16];//缩放矩阵
    private final float[] zoomAfterMatrix = new float[16];
    private final float[] finalMatrix = new float[16];//缩放矩阵
    private float[] linePoints = {

    };

    private ArrayList<ArrayList> curDrawed = new ArrayList<>();

    private ArrayList<Float> splitPoints = new ArrayList<Float>();
    private int splitType;

    private ArrayList<ArrayList<Float>> lineDrawed = new ArrayList<ArrayList<Float>>();

    private ArrayList<Float> markerDrawed = new ArrayList<Float>();

    private ArrayList<Float> eswcDrawed = new ArrayList<Float>();

    private ArrayList<Float> apoDrawed = new ArrayList<Float>();

    private ArrayList<Float> swcDrawed = new ArrayList<Float>();

//    private ArrayList<ImageMarker> MarkerList = new ArrayList<ImageMarker>();
    private MarkerList markerList = new MarkerList();

//    private ArrayList<ImageMarker> MarkerList_loaded = new ArrayList<ImageMarker>();

    private V_NeuronSWC_list newSwcList = new V_NeuronSWC_list();
    private V_NeuronSWC_list curSwcList = new V_NeuronSWC_list();

    private boolean isAddLine = false;
    private boolean isAddLine2 = false;

    private int lastLineType = 3;
    private int lastMarkerType = 3;
    private float contrast;


    private String filepath = ""; //文件路径
    private InputStream is;
    private long length;

    private boolean ifPainting = false;

    private boolean ifDownSampling = false;
    private boolean ifNeedDownSample = true;
    private boolean ifNavigationLococation = false;

    private int screen_w = -1;
    private int screen_h = -1;
    private float cur_scale = 1.0f;

    private byte[] grayscale;
    private int data_length;
    private boolean isBig;

    private FileType fileType;
    private ByteBuffer mCaptureBuffer;
    private Bitmap mBitmap;
    private boolean isTakePic = false;
    private String mCapturePath;


    private boolean ifFileSupport = false;
    private boolean ifFileLoaded = false;
    private boolean ifLoadSWC = false;

    private boolean ifShowSWC = true;

    private Context context_myrenderer;

    private int degree = 0;

//    private float [] gamePosition = new float[3];
//    private float [] gameDir = new float[3];
//    private float [] gameHead = new float[3];
//    private float [] thirdPosition = new float[3];
//    private float [] thirdDir = new float[3];
//    private float [] thirdHead = new float[3];

    private GameCharacter gameCharacter;

    private boolean ifGame = false;
    public static int threshold = 0;

    //初次渲染画面
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        //深蓝
        GLES30.glClearColor(0.098f, 0.098f, 0.439f, 1.0f);

        Log.v("onSurfaceCreated:","successfully");

        /*
        init shader program
         */
        MyPattern.initProgram();
        MyPattern2D.initProgram();

        /*
        init matrix
         */
        Matrix.setIdentityM(translateMatrix,0);//建立单位矩阵
        Matrix.setIdentityM(zoomMatrix,0);//建立单位矩阵
        Matrix.setIdentityM(zoomAfterMatrix, 0);
        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.setRotateM(rotationMatrix, 0, 0, -1.0f, -1.0f, 0.0f);
        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -2, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

    }


    //画面大小发生改变后
    public void onSurfaceChanged(GL10 gl,int width, int height){
        //设置视图窗口
        GLES30.glViewport(0, 0, width, height);

        boolean surfaceChanged = (screen_w != width || screen_h != height);
        screen_w = width;
        screen_h = height;

        Log.v(TAG,"---------------   onSurfaceChanged  -------------");
        System.out.println(screen_w);
        System.out.println(screen_h);

        if (surfaceChanged){
            Log.v(TAG,"---------------   init img when SurfaceChanged  -------------");
            if (fileType == FileType.V3draw || fileType == FileType.TIF || fileType == FileType.V3dPBD) {
                if (ifGame) {
                    myPattern = new MyPattern(width, height, img, mz, MyPattern.Mode.GAME);
                } else {
                    myPattern = new MyPattern(width, height, img, mz, MyPattern.Mode.NORMAL);
                }
//            myPattern.setIfGame(ifGame);
            }

            if (fileType == FileType.PNG || fileType == FileType.JPG)
                myPattern2D = new MyPattern2D(bitmap2D, sz[0], sz[1], mz);

            if (fileType == FileType.TIF || fileType == FileType.V3draw || fileType == FileType.V3dPBD
                    || fileType == FileType.SWC || fileType == FileType.APO || fileType == FileType.ANO) {
                myAxis = new MyAxis(mz);
            }
            myDraw = new MyDraw();
            myAnimation = new MyAnimation();
        }


//        if (ifFileSupport){
//            if (fileType == FileType.TIF || fileType == FileType.V3draw || fileType == FileType.V3dPBD
//                    || fileType == FileType.SWC || fileType == FileType.APO || fileType == FileType.ANO) {
//                myAxis = new MyAxis(mz);
//            }
//            myDraw = new MyDraw();
//            myAnimation = new MyAnimation();
//        }


        mCaptureBuffer = ByteBuffer.allocate(screen_h*screen_w*4);
        mBitmap = Bitmap.createBitmap(screen_w,screen_h, Bitmap.Config.ARGB_8888);



        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
//        Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

        float ratio = (float) width / height;
        if (fileType == FileType.PNG || fileType == FileType.JPG) {
            if (width > height) {
                Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1, 1, 1, 100);
            } else {
                Matrix.orthoM(projectionMatrix, 0, -1, 1, -1 / ratio, 1 / ratio, 1, 100);
            }
        }else {

            if (width > height) {
                Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 2f, 100);
            } else {
                Matrix.frustumM(projectionMatrix, 0, -1, 1, -1 / ratio, 1 / ratio, 2f, 100);
            }
        }

        if (ifGame) {
            Matrix.setIdentityM(translateMatrix, 0);//建立单位矩阵

            if (!ifNavigationLococation) {
                Matrix.translateM(translateMatrix, 0, -0.5f * mz[0], -0.5f * mz[1], -0.5f * mz[2]);
            } else {
                Matrix.translateM(translateMatrix, 0, -0.5f * mz_neuron[0], -0.5f * mz_neuron[1], -0.5f * mz_neuron[2]);
            }
        }

//        Matrix.setIdentityM(translateAfterMoveMatrix, 0);

//        if (ifGame){
//            setVisual(gamePosition, gameDir);
////            setVisual(gamePosition, gameDir);
//        }

//        onDrawFrame(gl);
//        Matrix.perspectiveM(projectionMatrix,0,45,1,0.1f,100f);

    }




    //绘制画面
    @Override
    public void onDrawFrame(GL10 gl){

        /*
         the color of background
         */
//        GLES30.glClearColor(0.5f, 0.4f, 0.3f, 1.0f);
//        GLES30.glClearColor(1.0f, 0.5f, 0.0f, 1.0f);
        //淡黄
//        GLES30.glClearColor(1.0f, 0.89f, 0.51f, 1.0f);
        //深蓝
//        GLES30.glClearColor(0.098f, 0.098f, 0.439f, 1.0f);
        //西红柿
//        GLES30.glClearColor(1f, 1f, 1f, 1.0f);
        //紫色
//        GLES30.glClearColor(0.192f, 0.105f, 0.572f, 1.0f);
        //浅蓝
//        GLES30.glClearColor(0.623f, 0.658f, 0.854f, 1.0f);
        //中蓝
        GLES30.glClearColor(121f/255f, 134f/255f, 203f/255f, 1.0f);
        //浅紫
//        GLES30.glClearColor(0.929f, 0.906f, 0.965f, 1.0f);


        //把颜色缓冲区设置为我们预设的颜色
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
//        GLES30.glEnable(GL_BLEND);
//        GLES30.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GLES10.glEnable(GL_ALPHA_TEST);
//        glAlphaFunc(GL_GREATER, 0.05f);
//        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        GLES30.glEnable(GL_BLEND);
        GLES30.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);


        if (judgeImg()){


//            if (ifGame) {
//                myPattern = new MyPattern(screen_w, screen_h, img, mz, MyPattern.Mode.GAME);
//            } else {
//                myPattern = new MyPattern(screen_w, screen_h, img, mz, MyPattern.Mode.NORMAL);
//            }

            /*
            init the {MyPattern, MyPattern2D, MyAxis, MyDraw, MyAnimation}
            */
            if (myPattern == null || myPattern2D == null){
                if (ifFileSupport){
                    if (fileType == FileType.V3draw || fileType == FileType.TIF || fileType == FileType.V3dPBD) {
                        if (ifGame) {
                            Log.v("MyRenderer","ifGame  ondrawframe");
                            myPattern = new MyPattern(screen_w, screen_h, img, mz, MyPattern.Mode.GAME);
                        } else {
                            myPattern = new MyPattern(screen_w, screen_h, img, mz, MyPattern.Mode.NORMAL);
                        }
                    }
                    if (fileType == FileType.PNG || fileType == FileType.JPG)
                        myPattern2D = new MyPattern2D(bitmap2D, sz[0], sz[1], mz);

                    if (fileType == FileType.TIF || fileType == FileType.V3draw || fileType == FileType.V3dPBD ||
                            fileType == FileType.SWC || fileType == FileType.APO || fileType == FileType.ANO){
                        if (myAxis == null)
                            myAxis = new MyAxis(mz);
                    }
                    if (myDraw == null)
                        myDraw = new MyDraw();
                    if (myAnimation == null)
                        myAnimation = new MyAnimation();

                    ifFileSupport = false;
                }
            }

            if (ifGame){
                setVisual(gameCharacter);

                setSmallMapMatrix();
//            setVisual(gamePosition, gameDir);
            }

            setMatrix();


            if (myAnimation != null){
                if (myAnimation.status){
                    AnimationRotation();
                }
            }


            /*
            if not loacation mode
            */
            if (!ifNavigationLococation){
                if (fileType == FileType.V3draw || fileType == FileType.TIF || fileType == FileType.V3dPBD) {
                    //draw the volume img
                    myPattern.drawVolume_3d(finalMatrix, translateAfterMatrix, ifDownSampling, contrast);
                }
                if (fileType == FileType.JPG || fileType == FileType.PNG){
                    //draw the 2D img
                    myPattern2D.draw(finalMatrix);
                }

                /*
                if show the swc & marker
                */
                if (ifShowSWC) {

                    if (curSwcList.nsegs() > 0) {
//                  System.out.println("------------draw curswclist------------------------");
                        ArrayList<Float> lines = new ArrayList<Float>();
                        for (int i = 0; i < curSwcList.seg.size(); i++) {
//                      System.out.println("i: "+i);
                            V_NeuronSWC seg = curSwcList.seg.get(i);
//                      ArrayList<Float> currentLine = swc.get(i);
                            Map<Integer, V_NeuronSWC_unit> swcUnitMap = new HashMap<Integer, V_NeuronSWC_unit>();
                            lines.clear();
                            for (int j = 0; j < seg.row.size(); j++) {
                                if (seg.row.get(j).parent != -1 && seg.getIndexofParent(j) != -1) {
                                    V_NeuronSWC_unit parent = seg.row.get(seg.getIndexofParent(j));
                                    swcUnitMap.put(j, parent);
                                }
                            }
//                      System.out.println("---------------end map-----------------------");
                            for (int j = 0; j < seg.row.size(); j++) {
//                          System.out.println("in row: "+j+"-------------------");
                                V_NeuronSWC_unit child = seg.row.get(j);
                                int parentid = (int) child.parent;
                                if (parentid == -1 || seg.getIndexofParent(j) == -1) {
//                              System.out.println("parent -1");
                                    float x = (int) child.x;
                                    float y = (int) child.y;
                                    float z = (int) child.z;
                                    float[] position = VolumetoModel(new float[]{x, y, z});
                                    myDraw.drawSplitPoints(finalMatrix, position[0], position[1], position[2], (int) child.type);
                                    continue;
                                }
                                V_NeuronSWC_unit parent = swcUnitMap.get(j);
                                lines.add((float) ((sz[0] - parent.x) / sz[0] * mz[0]));
                                lines.add((float) ((sz[1] - parent.y) / sz[1] * mz[1]));
                                lines.add((float) ((parent.z) / sz[2] * mz[2]));
                                lines.add((float) ((sz[0] - child.x) / sz[0] * mz[0]));
                                lines.add((float) ((sz[1] - child.y) / sz[1] * mz[1]));
                                lines.add((float) ((child.z) / sz[2] * mz[2]));
//                          System.out.println("in draw line--------------"+j);
//                          System.out.println("type: "+parent.type);
                                myDraw.drawLine(finalMatrix, lines, (int) parent.type);
                                if (ifGame) {
                                    float x = lines.get(0) / mz[0] - 0.5f;
                                    float y = lines.get(1) / mz[1] - 0.5f;
                                    float z = lines.get(2) / mz[2] - 0.5f;
                                    if (Math.sqrt((double)(x * x + y * y + z * z)) < 1) {
                                        myDraw.drawLine(finalSmallMapMatrix, lines, (int) parent.type);
                                    }
                                }
                                lines.clear();
                            }
                        }

                    }

                    if (newSwcList.nsegs() > 0) {
//                  System.out.println("------------draw curswclist------------------------");
                        ArrayList<Float> lines = new ArrayList<Float>();
                        for (int i = 0; i < newSwcList.seg.size(); i++) {
//                      System.out.println("i: "+i);
                            V_NeuronSWC seg = newSwcList.seg.get(i);
//                      ArrayList<Float> currentLine = swc.get(i);
                            Map<Integer, V_NeuronSWC_unit> swcUnitMap = new HashMap<Integer, V_NeuronSWC_unit>();
                            lines.clear();
                            for (int j = 0; j < seg.row.size(); j++) {
                                if (seg.row.get(j).parent != -1 && seg.getIndexofParent(j) != -1) {
                                    V_NeuronSWC_unit parent = seg.row.get(seg.getIndexofParent(j));
                                    swcUnitMap.put(j, parent);
                                }
                            }
//                      System.out.println("---------------end map-----------------------");
                            for (int j = 0; j < seg.row.size(); j++) {
//                      System.out.println("in row: "+j+"-------------------");
                                V_NeuronSWC_unit child = seg.row.get(j);
                                int parentid = (int) child.parent;
                                if (parentid == -1 || seg.getIndexofParent(j) == -1) {
//                              System.out.println("parent -1");
                                    float x = (int) child.x;
                                    float y = (int) child.y;
                                    float z = (int) child.z;
                                    float[] position = VolumetoModel(new float[]{x, y, z});
                                    myDraw.drawSplitPoints(finalMatrix, position[0], position[1], position[2], (int) child.type);
                                    continue;
                                }
                                V_NeuronSWC_unit parent = swcUnitMap.get(j);
                                lines.add((float) ((sz[0] - parent.x) / sz[0] * mz[0]));
                                lines.add((float) ((sz[1] - parent.y) / sz[1] * mz[1]));
                                lines.add((float) ((parent.z) / sz[2] * mz[2]));
                                lines.add((float) ((sz[0] - child.x) / sz[0] * mz[0]));
                                lines.add((float) ((sz[1] - child.y) / sz[1] * mz[1]));
                                lines.add((float) ((child.z) / sz[2] * mz[2]));
//                          System.out.println("in draw line--------------"+j);
//                          System.out.println("type: "+parent.type);
                                myDraw.drawLine(finalMatrix, lines, (int) parent.type);
                                lines.clear();
                            }
                        }

                    }


                    /*
                    draw the marker
                    */
                    if (markerList.size() > 0) {
                        float radius = 0.02f;
                        if (fileType == FileType.JPG || fileType == FileType.PNG)
                            radius = 0.01f;
                        for (int i = 0; i < markerList.size(); i++) {
//                      System.out.println("start draw marker---------------------");
                            ImageMarker imageMarker = markerList.get(i);
                            float[] markerModel = VolumetoModel(new float[]{imageMarker.x, imageMarker.y, imageMarker.z});
                            if (imageMarker.radius == 5) {
                                myDraw.drawMarker(finalMatrix, modelMatrix, markerModel[0], markerModel[1], markerModel[2], imageMarker.type, 0.01f);
                            } else {
                                myDraw.drawMarker(finalMatrix, modelMatrix, markerModel[0], markerModel[1], markerModel[2], imageMarker.type, radius);
                            }
//                      Log.v("onDrawFrame: ", "(" + markerDrawed.get(i) + ", " + markerDrawed.get(i+1) + ", " + markerDrawed.get(i+2) + ")");
                            if (ifGame){
                                if (imageMarker.radius == 5) {
                                    myDraw.drawMarker(finalSmallMapMatrix, modelMatrix, markerModel[0], markerModel[1], markerModel[2], imageMarker.type, 0.01f);
                                } else {
                                    myDraw.drawMarker(finalSmallMapMatrix, modelMatrix, markerModel[0], markerModel[1], markerModel[2], imageMarker.type, radius);
                                }
                            }
                        }
                    }

                }


                /*
                draw white trace of line that the user paint
                */
                if (ifPainting) {
                    if(linePoints.length > 0){
                        int num = linePoints.length / 3;
                        myDraw.drawPoints(linePoints, num);
                    }
                }


                /*
                draw the axis
                */
                if (fileType == FileType.V3draw || fileType == FileType.TIF || fileType == FileType.SWC || fileType == FileType.APO || fileType == FileType.ANO || fileType == FileType.V3dPBD)
                    if (myAxis != null)
                    {
                        if (!ifGame) {
                            myAxis.draw(finalMatrix);
                        } else {
                            GLES30.glDisable(GLES30.GL_DEPTH_TEST);
                            myAxis.draw(finalSmallMapMatrix);
                            GLES30.glEnable(GLES30.GL_DEPTH_TEST);
                        }
                    }

            }else {

                /*
                draw the location map in big data mode
                */
                if (myNavLoc == null){
                    myNavLoc = new MyNavLoc(mz_neuron, mz_block);
                }
                myNavLoc.draw(finalMatrix);
            }


            /*
            Screenshot and share
            */
            if(isTakePic){
                mCaptureBuffer.rewind();
                GLES30.glReadPixels(0,0,screen_w,screen_h,GLES30.GL_RGBA,GLES30.GL_UNSIGNED_BYTE,mCaptureBuffer);
                isTakePic = false;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mCaptureBuffer.rewind();
                        mBitmap.copyPixelsFromBuffer(mCaptureBuffer);
                        for(int i=0; i<screen_w; i++){
                            for(int j=0; j<screen_h/2; j++){
                                int jj = screen_h-1-j;
                                int pixelTmp = mBitmap.getPixel(i,jj);
                                mBitmap.setPixel(i,jj,mBitmap.getPixel(i,j));
                                mBitmap.setPixel(i,j,pixelTmp);
                            }
                        }

                        ImageUtil imageUtil = new ImageUtil();
                        Bitmap output_mBitmap = imageUtil.drawTextToRightBottom(getContext(), mBitmap, "C3", 20, Color.RED, 40, 30);
                        String mCaptureDir = "/storage/emulated/0/C3/screenCapture";
                        File dir = new File(mCaptureDir);
                        if (!dir.exists()){
                            dir.mkdirs();
                        }

                        mCapturePath = mCaptureDir + "/" + "Image_" + System.currentTimeMillis() +".jpg";
                        System.out.println(mCapturePath+"------------------------------");
                        try {
                            if (Looper.myLooper() == null)
                                Looper.prepare();

                            FileOutputStream fos = new FileOutputStream(mCapturePath);
                            output_mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                            String[] imgPath = new String[1];
                            imgPath[0] = mCapturePath;

                            if (imgPath[0] != null)
                            {
                                Log.v("Share","save screenshot to " + imgPath[0]);

                                Intent shareIntent = new Intent();
                                String imageUri = insertImageToSystem(context_myrenderer, imgPath[0]);
                                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                shareIntent.setAction(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(imageUri));
                                shareIntent.setType("image/jpeg");
                                context_myrenderer.startActivity(Intent.createChooser(shareIntent, "Share from C3"));

                            }
                            else{
                                Toast.makeText(getContext(), "Fail to screenshot", Toast.LENGTH_SHORT).show();
                                Looper.loop();
                            }

                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }).start();
            }


        }else {
            Toast.makeText(context_myrenderer,"Something Wrong When Renderer, reload File Please !",Toast.LENGTH_SHORT).show();
        }


        GLES30.glDisable(GL_BLEND);
        GLES30.glDisable(GL_ALPHA_TEST);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

    }


    private static String insertImageToSystem(Context context, String imagePath) {
        String url = "";
        String filename = imagePath.substring(imagePath.lastIndexOf("/") + 1 );
        try {
            url = MediaStore.Images.Media.insertImage(context.getContentResolver(), imagePath, filename, "ScreenShot from C3");
        } catch (FileNotFoundException e) {
            System.out.println("SSSSSSSSSSSS");
            e.printStackTrace();
        }
        System.out.println("Filename: " + filename);
        System.out.println("Url: " + url);
        return url;
    }


    private void setMatrix(){

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        Matrix.multiplyMM(mMVP2DMatrix, 0, vPMatrix, 0, zoomMatrix, 0);
        // Set the Rotation matrix
//        Matrix.setRotateM(rotationMatrix, 0, angle, 0.0f, 1.0f, 0.0f);
//        Matrix.setRotateM(rotationXMatrix, 0, angleX, 1.0f, 0.0f, 0.0f);
//        Matrix.setRotateM(rotationYMatrix, 0, angleY, 0.0f, 1.0f, 0.0f);

//        Log.v("roatation",Arrays.toString(rotationMatrix));
        if (!ifGame) {
            Matrix.setIdentityM(translateMatrix, 0);//建立单位矩阵
//
//
            if (!ifNavigationLococation) {
                Matrix.translateM(translateMatrix, 0, -0.5f * mz[0], -0.5f * mz[1], -0.5f * mz[2]);
            } else {
                Matrix.translateM(translateMatrix, 0, -0.5f * mz_neuron[0], -0.5f * mz_neuron[1], -0.5f * mz_neuron[2]);
            }
        }
//        Matrix.multiplyMM(translateMatrix, 0, zoomMatrix, 0, translateMatrix, 0);
        Matrix.setIdentityM(translateAfterMatrix, 0);

        Matrix.translateM(translateAfterMatrix, 0, 0, 0, cur_scale);
//        Matrix.translateM(translateAfterMatrix, 0, 0, 0, -cur_scale);

        // Combine the rotation matrix with the projection and camera view
        // Note that the vPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
//        Matrix.multiplyMM(rotationMatrix, 0, rotationYMatrix, 0, rotationXMatrix, 0);
//        Matrix.multiplyMM(rotationMatrix, 0, zoomMatrix, 0, rotationMatrix, 0);
        Matrix.multiplyMM(modelMatrix, 0, rotationMatrix, 0, translateMatrix, 0);
        if (ifGame){
//            Matrix.multiplyMM(modelMatrix, 0, translateAfterMoveMatrix, 0, modelMatrix, 0);

            Matrix.multiplyMM(RTMatrix, 0, zoomMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(TMMatrix, 0, translateAfterMoveMatrix, 0, RTMatrix, 0);  //translateAfterMoveMatrix会随面朝方向的改变而改变 从而达到绕图像中心，即控制角色所在位置旋转的效果

            Matrix.multiplyMM(ZRTMatrix, 0, translateAfterMatrix, 0, TMMatrix, 0);

            Matrix.multiplyMM(finalMatrix, 0, vPMatrix, 0, ZRTMatrix, 0);
        } else {
//        Matrix.multiplyMM(modelMatrix, 0, rotationMatrix, 0, translateMatrix, 0);


            Matrix.multiplyMM(RTMatrix, 0, zoomMatrix, 0, modelMatrix, 0);

            Matrix.multiplyMM(ZRTMatrix, 0, translateAfterMatrix, 0, RTMatrix, 0);

            Matrix.multiplyMM(finalMatrix, 0, vPMatrix, 0, ZRTMatrix, 0);      //ZRTMatrix代表modelMatrix
        }

//        Matrix.multiplyMM(finalMatrix, 0, zoomMatrix, 0, scratch, 0);

//        Matrix.setIdentityM(translateAfterMatrix, 0);
//        Matrix.translateM(translateAfterMatrix, 0, 0.0f, 0.0f, -0.1f);
//        Matrix.multiplyMM(translateAfterMatrix, 0, zoomAfterMatrix, 0, translateAfterMatrix, 0);
    }

    private void setSmallMapMatrix(){
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        Matrix.multiplyMM(mMVP2DMatrix, 0, vPMatrix, 0, zoomMatrix, 0);
        // Set the Rotation matrix
//        Matrix.setRotateM(rotationMatrix, 0, angle, 0.0f, 1.0f, 0.0f);
//        Matrix.setRotateM(rotationXMatrix, 0, angleX, 1.0f, 0.0f, 0.0f);
//        Matrix.setRotateM(rotationYMatrix, 0, angleY, 0.0f, 1.0f, 0.0f);

//        Log.v("roatation",Arrays.toString(rotationMatrix));
//        if (!ifGame) {
        Matrix.setIdentityM(translateSmallMapMatrix, 0);//建立单位矩阵
//
//
        if (!ifNavigationLococation) {
            Matrix.translateM(translateSmallMapMatrix, 0, -0.5f * mz[0], -0.5f * mz[1], -0.5f * mz[2]);
        } else {
            Matrix.translateM(translateSmallMapMatrix, 0, -0.5f * mz_neuron[0], -0.5f * mz_neuron[1], -0.5f * mz_neuron[2]);
        }

//        Matrix.translateM(translateSmallMapMatrix, 0, 1f, -0.5f, -1.5f);
//        }
//        Matrix.multiplyMM(translateMatrix, 0, zoomMatrix, 0, translateMatrix, 0);
        Matrix.setIdentityM(translateAfterMatrix, 0);

        Matrix.translateM(translateAfterMatrix, 0, 2.3f, 0.5f, 0.5f);

        Matrix.setIdentityM(zoomSmallMapMatrix, 0);
        Matrix.scaleM(zoomSmallMapMatrix, 0, 0.5f, 0.5f, 0.5f);
//        Matrix.translateM(translateAfterMatrix, 0, 0, 0, -cur_scale);

        // Combine the rotation matrix with the projection and camera view
        // Note that the vPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
//        Matrix.multiplyMM(rotationMatrix, 0, rotationYMatrix, 0, rotationXMatrix, 0);
//        Matrix.multiplyMM(rotationMatrix, 0, zoomMatrix, 0, rotationMatrix, 0);
        Matrix.multiplyMM(modelMatrix, 0, rotationMatrix, 0, translateSmallMapMatrix, 0);

        Matrix.multiplyMM(RTMatrix, 0, zoomSmallMapMatrix, 0, modelMatrix, 0);

        Matrix.multiplyMM(ZRTMatrix, 0, translateAfterMatrix, 0, RTMatrix, 0);

        Matrix.multiplyMM(finalSmallMapMatrix, 0, vPMatrix, 0, ZRTMatrix, 0);      //ZRTMatrix代表modelMatrix
    }



    //int转byte
    private static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }


    //设置文件路径
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void SetPath(String message){

        filepath = message;
        SetFileType();

        myAxis = null;
        cur_scale = 1.0f;

        curSwcList.clear();
        markerList.clear();
//        MarkerList_loaded.clear();

        if (fileType == FileType.V3draw || fileType == FileType.TIF || fileType == FileType.V3dPBD){
            Log.v(TAG,"Before setImage()");
            setImage();
            ifFileLoaded = true;
            ifFileSupport = true;
        }

        else if (fileType == FileType.SWC){
            bitmap2D = null;
            myPattern2D = null;
            img = null;
            setSWC();
            ifFileLoaded = true;
            ifFileSupport = true;
        }

        else if (fileType == FileType.PNG || fileType == FileType.JPG){
            loadImage2D();
            ifFileLoaded = true;
            ifFileSupport = true;
        }

        else if (fileType == FileType.APO){
            bitmap2D = null;
            myPattern2D = null;
            img = null;
            setAPO();
            ifFileLoaded = true;
            ifFileSupport = true;
        }

        else if (fileType == FileType.ANO){
            bitmap2D = null;
            myPattern2D = null;
            img = null;
            setANO();
            ifFileLoaded = true;
            ifFileSupport = true;
        }

        else {
            return;
        }


        Log.v("SetPath", Arrays.toString(mz));

        Matrix.setIdentityM(translateMatrix,0);//建立单位矩阵
        Matrix.setIdentityM(zoomMatrix,0);//建立单位矩阵
        Matrix.setIdentityM(zoomAfterMatrix, 0);
        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.setRotateM(rotationMatrix, 0, 0, -1.0f, -1.0f, 0.0f);
//        Matrix.setIdentityM(translateAfterMatrix, 0);
        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -2, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

    }

    public void SetSWCPath(String message){
        filepath = message;
        SetFileType();

        myAxis = null;
        cur_scale = 1.0f;

        curSwcList.clear();
        markerList.clear();

        if (fileType == FileType.SWC){
            bitmap2D = null;
            myPattern2D = null;
            setSWC();
            ifFileLoaded = true;
            ifFileSupport = true;
        }

        else {
            Toast.makeText(getContext(), "Do not support this file", Toast.LENGTH_LONG).show();
            return;
        }

        Matrix.setIdentityM(translateMatrix,0);//建立单位矩阵

        Matrix.setIdentityM(zoomMatrix,0);//建立单位矩阵
        Matrix.setIdentityM(zoomAfterMatrix, 0);
        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.setRotateM(rotationMatrix, 0, 0, -1.0f, -1.0f, 0.0f);

        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -2, 0f, 0f, 0f, 0f, 1.0f, 0.0f);


    }


    //设置文件路径
    public void SetPath_Bigdata(String message, int[] index){

        filepath = message;
        fileType = FileType.V3draw;

        myAxis = null;
        cur_scale = 1.0f;

        curSwcList.clear();
        markerList.clear();

        SetImage_Bigdata(index);
//        setImage();
        ifFileLoaded = true;
        ifFileSupport = true;

        Log.v("SetPath", Arrays.toString(mz));

        Matrix.setIdentityM(translateMatrix,0);//建立单位矩阵

        Matrix.setIdentityM(zoomMatrix,0);//建立单位矩阵
        Matrix.setIdentityM(zoomAfterMatrix, 0);
        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.setRotateM(rotationMatrix, 0, 0, -1.0f, -1.0f, 0.0f);
//        Matrix.setIdentityM(translateAfterMatrix, 0);
        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -2, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

    }


//    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadImage2D(){
        File file = new File(filepath);
        long length = 0;
        InputStream is = null;
        if (file.exists()){
            try {
                length = file.length();
                is = new FileInputStream(file);
//                grayscale =  rr.run(length, is);


                Log.v("getIntensity_3d", filepath);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        else {
            Uri uri = Uri.parse(filepath);

            try {
                ParcelFileDescriptor parcelFileDescriptor =
                        getContext().getContentResolver().openFileDescriptor(uri, "r");

                is = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);

                length = (int)parcelFileDescriptor.getStatSize();

                Log.v("MyPattern","Successfully load intensity");

            }catch (Exception e){
                Log.v("MyPattern","Some problems in the MyPattern when load intensity");
            }


        }
//        setOrientation(filepath);
//        int degree = 0;
        degree = getBitmapDegree(filepath);
        bitmap2D = BitmapFactory.decodeStream(is);
//        ByteArrayOutputStream st = new ByteArrayOutputStream();
        if (bitmap2D != null){

//            int degree = 0;
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//                degree = getBitmapDegree(is);
//            }
//            else {
//                degree = getBitmapDegree(filepath);
//            }
            System.out.println(degree);

            bitmap2D = rotateBitmapByDegree(bitmap2D, degree);
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bitmap.getByteCount());
            sz[0] = bitmap2D.getWidth();
            sz[1] = bitmap2D.getHeight();
            sz[2] = Math.max(sz[0], sz[1]);

            Integer[] num = {sz[0], sz[1]};
            float max_dim = (float) Collections.max(Arrays.asList(num));
            Log.v("MyRenderer", Float.toString(max_dim));

            mz[0] = (float) sz[0]/max_dim;
            mz[1] = (float) sz[1]/max_dim;
            mz[2] = Math.max(mz[0], mz[1]);

        }

    }





    private void SetFileType(){

        String filetype;
        File file = new File(filepath);
        if (file.exists()){
            filetype = filepath.substring(filepath.lastIndexOf(".")).toUpperCase();
        }else {
            Uri uri = Uri.parse(filepath);
            FileManager fileManager = new FileManager();
            filetype = fileManager.getFileType(uri);
        }

        System.out.println(filepath);
        System.out.println(filetype);

        switch (filetype){
            case ".V3DRAW":
                fileType = FileType.V3draw;
                break;

            case ".V3DPBD":
                fileType = FileType.V3dPBD;
                break;

            case ".SWC":
            case ".ESWC":
                fileType = FileType.SWC;
                break;

            case ".TIF":
            case ".TIFF":
                fileType = FileType.TIF;
                break;

            case ".JPEG":
            case ".JPG":
                fileType = FileType.JPG;
                break;

            case ".PNG":
                fileType = FileType.PNG;
                break;

            case ".APO":
                fileType = FileType.APO;
                break;

            case ".ANO":
                fileType = FileType.ANO;
                break;

            case "fail to read file":
                fileType = FileType.NotSupport;
                Toast.makeText(getContext(), "Fail to read file!",Toast.LENGTH_SHORT).show();
                break;

            default:
                fileType = FileType.NotSupport;
                Toast.makeText(getContext(),"Don't support this file!",Toast.LENGTH_SHORT).show();
        }

    }

    public void deleteAllTracing() {

        try {
            saveUndo();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        for (int i = curSwcList.seg.size(); i >= 0; i--){
            curSwcList.deleteSeg(i);
        }
    }

    private void JumptoFileActivity(String errormsg){
        Context context = getContext();
        Intent intent = new Intent(context, FileActivity.class);
        intent.putExtra(MyRenderer.FILE_SUPPORT_ERROR, errormsg);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void setInputStream(InputStream Is){

        is = Is;
    }

    public void setLength(long Length){

        length = Length;
    }


    /**
     * set rotationMatrix when the animation run
     */
    public void AnimationRotation(){

        float[] arotationMatrix = new float[16];
        arotationMatrix = myAnimation.Rotation();
        Matrix.multiplyMM(rotationMatrix,0, rotationMatrix, 0, arotationMatrix,0);

    }

//    public void setAnimation(boolean status, float speed, String type){
//        myAnimation.status = status;
//        myAnimation.speed = speed/60f;
//        myAnimation.setRotationType(type);
//        myAnimation.ResetAnimation();
//        if (status == false){
//            myAnimation.setRotationType("X");
//        }
//    }

    /**
     * Reset rotation matrix for big data mode : button 3d rotation
     */
    public void resetRotation(){
        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.setRotateM(rotationMatrix, 0, 0, -1.0f, -1.0f, 0.0f);
    }

    public void rotate(float dx, float dy, float dis){
//        Log.v("wwww", "66666666666666666");
//        Log.v("dddddxxxxx", Float.toString(dx));
//        Log.v("ddddddyyyy", Float.toString(dy));
//        Log.v("ddddddiiiissss", Float.toString(dis));
//
//        float [] currentRotateM = new float[16];
//        Matrix.setIdentityM(currentRotateM, 0);
//        Matrix.setRotateM(currentRotateM, 0, (float)(dis / Math.PI * 180.0f), dy, -dx, 0);
//        float [] templateMatrix = rotationMatrix;
//        Matrix.multiplyMM(rotationMatrix, 0, currentRotateM, 0, templateMatrix, 0);
////        float [] currentRotateM = rotateM((float)(dis / Math.PI * 180.0f), dy, -dx, 0);
////        finalRotateMatrix = multiplyMatrix(currentRotateM, finalRotateMatrix);
//        Log.v("rotation", Arrays.toString(currentRotateM));
////        Matrix.multiplyMM(rotationMatrix, 0, currentRotateM, 0, rotationMatrix, 0);
        angleX = dy * 70;
        angleY = dx * 70;
        Matrix.setRotateM(rotationXMatrix, 0, angleX, 1.0f, 0.0f, 0.0f);
        Matrix.setRotateM(rotationYMatrix, 0, angleY, 0.0f, 1.0f, 0.0f);
        float [] curRotationMatrix = new float[16];
        Matrix.multiplyMM(curRotationMatrix, 0, rotationXMatrix, 0, rotationYMatrix, 0);
        Matrix.multiplyMM(rotationMatrix, 0, curRotationMatrix, 0, rotationMatrix, 0);

//        Log.v("angleX = ", Float.toString(angleX));
//        Log.v("angleY = ", Float.toString(angleY));
    }

    public void rotate2f(float x1, float x2, float y1, float y2){
        double value = (x1 * x2 + y1 * y2) / (Math.sqrt(x1 * x1 + y1 * y1) * Math.sqrt(x2 * x2 + y2 * y2));
        if (value > 1){
            value = 1;
        }
        System.out.println(value);
//        angleZ = (float)Math.toDegrees(Math.acos(value));
        angleZ = (float)(Math.acos(value) / Math.PI * 180.0);
        System.out.println(angleZ);
        float axis = x2 * y1 - x1 * y2;
        if (axis != 0) {
//        float [] rotationZMatrix = new float[16];
            Matrix.setRotateM(rotationZMatrix, 0, angleZ, 0.0f, 0.0f, axis);
            Matrix.multiplyMM(rotationMatrix, 0, rotationZMatrix, 0, rotationMatrix, 0);
        }
    }


    public void zoom(float f){

        if (cur_scale > 0.2 && cur_scale < 30) {
            Matrix.scaleM(zoomMatrix, 0, f, f, f);
            cur_scale *= f;
        }else if(cur_scale < 0.2 && f > 1){
            Matrix.scaleM(zoomMatrix, 0, f, f, f);
            cur_scale *= f;
        }else if (cur_scale > 30 && f < 1){
            Matrix.scaleM(zoomMatrix, 0, f, f, f);
            cur_scale *= f;
        }

//        Matrix.scaleM(zoomAfterMatrix, 0, f-1, f-1, f-1);
//        float d = (f - 1) * 0.5f;
//        Matrix.translateM(translateAfterMatrix, 0, 0.0f, 0.0f, d);
//        zoomMatrix = {f, 0.0f, 0.0f, 0.0f,
//                      0.0f, f, 0.0f, 0.0f,
//                      0.0f, 0.0f, f, 0.0f,
//                      0.0f, 0.0f, 0.0f, f};
    }


    public void zoom_in(){

        zoom(2f);

    }

    public void zoom_out(){

        zoom(0.6f);

    }

    public void resetContrast(float contrast){
        this.contrast = (contrast/100.f) + 1.0f;
    }


    //矩阵乘法
    private float [] multiplyMatrix(float [] m1, float [] m2){
        float [] m = new float[9];
        for (int i = 0; i < 9; i++){
            int r = i / 3;
            int c = i % 3;
            m[i] = 0;
            for (int j = 0; j < 3; j++){
                m[i] += m1[r * 3 + j] * m2[j * 3 + c];
            }
        }
        return m;
    }


    private void CreateBuffer(byte[] data){
        //分配内存空间,每个字节型占1字节空间
        imageBuffer = ByteBuffer.allocateDirect(data.length)
                .order(ByteOrder.nativeOrder());
        //传入指定的坐标数据
        imageBuffer.put(data);
        imageBuffer.position(0);
    }

    public void setLineDrawed(ArrayList<Float> lineDrawed){
//        Float [] linePoints = lineDrawed.toArray(new Float[lineDrawed.size()]);

        linePoints = new float[lineDrawed.size()];
        for (int i =0; i < lineDrawed.size(); i++){
            linePoints[i] = lineDrawed.get(i);
        }
    }

    public void setIfPainting(boolean b){

        ifPainting = b;
    }


    public float[] solve2DMarker(float x, float y){
        if (ifIn2DImage(x, y)){
            System.out.println("innnnn");
            float i;
            float [] result = new float[3];
            for (i = -1; i < 1; i += 0.005){
                float [] invertfinalMatrix = new float[16];

                Matrix.invertM(invertfinalMatrix, 0, finalMatrix, 0);

                float [] temp = new float[4];
                Matrix.multiplyMV(temp, 0, invertfinalMatrix, 0, new float[]{x, y, i, 1}, 0);
                devideByw(temp);
                float dis = Math.abs(temp[2] - mz[2] / 2);
                if (dis < 0.1) {
                    System.out.println(temp[0]);
                    System.out.println(temp[1]);
                    result = new float[]{temp[0], temp[1], mz[2] / 2};
                    break;
                }
            }
            result = ModeltoVolume(result);
            System.out.println(result[0]);
            System.out.println(result[1]);
            return result;
        }
        return null;
    }

    public void add2DMarker(float x, float y) throws CloneNotSupportedException {
        float [] new_marker = solve2DMarker(x, y);
        if (new_marker == null){
            System.out.println("outtttt");
            Toast.makeText(getContext(), "Please make sure the point is in the image", Toast.LENGTH_SHORT).show();
            return;
        }else {
            ImageMarker imageMarker_drawed = new ImageMarker(new_marker[0],
                    new_marker[1],
                    new_marker[2]);
            imageMarker_drawed.type = lastMarkerType;
            System.out.println("set type to 3");

            MarkerList tempMarkerList = markerList.clone();
            V_NeuronSWC_list tempCurveList = curSwcList.clone();

            if (curUndo < UNDO_LIMIT){
                curUndo += 1;
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            } else {
                undoMarkerList.remove(0);
                undoCurveList.remove(0);
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            }

            markerList.add(imageMarker_drawed);

//            if (process.size() < UNDO_LIMIT){
//                process.add(Operate.DRAWMARKER);
//                undoDrawMarkerList.add(imageMarker_drawed);
//            } else {
//                Operate first = process.firstElement();
//                process.remove(0);
//                process.add(Operate.DRAWMARKER);
//                removeFirstUndo(first);
//                undoDrawMarkerList.add(imageMarker_drawed);
//            }
        }
    }

    public void deleteMultiMarkerByStroke(ArrayList<Float> line) throws CloneNotSupportedException {
//        ArrayList<Integer> toBeDeleted = new ArrayList<>();
        boolean already = false;
        for (int i = markerList.size() - 1; i >= 0; i--){
            ImageMarker tobeDeleted = markerList.get(i);
            float[] markerModel = VolumetoModel(new float[]{tobeDeleted.x,tobeDeleted.y,tobeDeleted.z});
            float [] position = new float[4];
            position[0] = markerModel[0];
            position[1] = markerModel[1];
            position[2] = markerModel[2];
            position[3] = 1.0f;

            float [] positionVolumne = new float[4];
            Matrix.multiplyMV(positionVolumne, 0, finalMatrix, 0, position, 0);
            devideByw(positionVolumne);

            if (pnpoly(line, positionVolumne[0], positionVolumne[1])){

                if (!already){
                    MarkerList tempMarkerList = markerList.clone();
                    V_NeuronSWC_list tempCurveList = curSwcList.clone();

                    if (curUndo < UNDO_LIMIT){
                        curUndo += 1;
                        undoMarkerList.add(tempMarkerList);
                        undoCurveList.add(tempCurveList);
                    } else {
                        undoMarkerList.remove(0);
                        undoCurveList.remove(0);
                        undoMarkerList.add(tempMarkerList);
                        undoCurveList.add(tempCurveList);
                    }
                    already = true;
                }

                markerList.remove(tobeDeleted);
            }
        }
    }

    public boolean pnpoly(ArrayList<Float> line, float x, float y){
        int n = line.size() / 3;
        int i = 0;
        int j = n - 1;
        boolean result = false;
        for (;i < n; j = i++){
            float x1 = line.get(i * 3);
            float y1 = line.get(i * 3 + 1);
            float x2 = line.get(j * 3);
            float y2 = line.get(j * 3 + 1);
            if (((y1 > y) != (y2 > y)) && (x < ((x2 - x1) * (y - y1) / (y2 - y1) + x1))){
                result = !result;
            }
        }
        return result;
    }

    public void add2DCurve(ArrayList<Float> line) throws CloneNotSupportedException {
        ArrayList<Float> lineAdded = new ArrayList<>();
        for (int i = 0; i < line.size() / 3; i++){
            float x = line.get(i * 3);
            float y = line.get(i * 3 + 1);

            float [] cur_point = solve2DMarker(x, y);
            if (cur_point == null){
                if (i == 0){
                    Toast.makeText(getContext(), "Please make sure the point is in the image", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
            }
            else{
                lineAdded.add(cur_point[0]);
                lineAdded.add(cur_point[1]);
                lineAdded.add(cur_point[2]);
            }
        }
        if (lineAdded != null){
//            lineDrawed.add(lineAdded);
            int max_n = curSwcList.maxnoden();
            V_NeuronSWC seg = new  V_NeuronSWC();
            for(int i=0; i < lineAdded.size()/3; i++){
                V_NeuronSWC_unit u = new V_NeuronSWC_unit();
                u.n = max_n + i+ 1;
                if(i==0)
                    u.parent = -1;
                else
                    u.parent = max_n + i;
//                float[] xyz = ModeltoVolume(new float[]{lineAdded.get(i*3+0),lineAdded.get(i*3+1),lineAdded.get(i*3+2)});
                float[] xyz = new float[]{lineAdded.get(i*3+0),lineAdded.get(i*3+1),lineAdded.get(i*3+2)};
                u.x = xyz[0];
                u.y = xyz[1];
                u.z = xyz[2];
                u.type = lastLineType;
                seg.append(u);
//                System.out.println("u n p x y z: "+ u.n +" "+u.parent+" "+u.x +" "+u.y+ " "+u.z);
            }
            if(seg.row.size()<3){
                return;
            }
            float[] headXYZ = new float[]{(float) seg.row.get(0).x, (float) seg.row.get(0).y, (float) seg.row.get(0).z};
            float[] tailXYZ = new float[]{(float) seg.row.get(seg.row.size()-1).x,
                    (float) seg.row.get(seg.row.size()-1).y,
                    (float) seg.row.get(seg.row.size()-1).z};
            boolean linked = false;
            for(int i=0; i<curSwcList.seg.size(); i++){
                V_NeuronSWC s = curSwcList.seg.get(i);
                for(int j=0; j<s.row.size(); j++){
                    if(linked)
                        break;
                    V_NeuronSWC_unit node = s.row.get(j);
                    float[] nodeXYZ = new float[]{(float) node.x, (float) node.y, (float) node.z};
                    if(distance(headXYZ,nodeXYZ)<5){
                        V_NeuronSWC_unit head = seg.row.get(0);
                        V_NeuronSWC_unit child = seg.row.get(1);
                        head.x = node.x;
                        head.y = node.y;
                        head.z = node.z;
                        head.n = node.n;
                        head.parent = node.parent;
                        child.parent = head.n;
                        linked = true;
                        break;
                    }
                    if(distance(tailXYZ,nodeXYZ)<5){
                        seg.reverse();
                        V_NeuronSWC_unit tail = seg.row.get(seg.row.size()-1);
                        V_NeuronSWC_unit child = seg.row.get(seg.row.size()-2);
                        tail.x = node.x;
                        tail.y = node.y;
                        tail.z = node.z;
                        tail.n = node.n;
                        tail.parent = node.parent;
                        child.n = tail.n;
                        linked = true;
                        break;
                    }
                }
            }

            MarkerList tempMarkerList = markerList.clone();
            V_NeuronSWC_list tempCurveList = curSwcList.clone();

            if (curUndo < UNDO_LIMIT){
                curUndo += 1;
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            } else {
                undoMarkerList.remove(0);
                undoCurveList.remove(0);
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            }

            curSwcList.append(seg);
//            if (process.size() < UNDO_LIMIT){
//                process.add(Operate.DRAWCURVE);
//                undoDrawList.add(seg);
//            } else{
//                Operate first = process.firstElement();
//                process.remove(0);
//                process.add(Operate.DRAWCURVE);
//                removeFirstUndo(first);
//                undoDrawList.add(seg);
//            }
//            Log.v("addLineDrawed", Integer.toString(lineAdded.size()));
        }
        else
            Log.v("draw line:::::", "nulllllllllllllllllll");

    }

    public boolean ifIn2DImage(float x, float y){
        float [] x1 = new float[]{0 ,0, mz[2] / 2, 1};
        float [] x2 = new float[]{mz[0], 0, mz[2] / 2, 1};
        float [] x3 = new float[]{0, mz[1], mz[2] / 2, 1};
        float [] x4 = new float[]{mz[0], mz[1], mz[2] / 2, 1};
        float [] x1r = new float[4];
        float [] x2r = new float[4];
        float [] x3r = new float[4];
        float [] x4r = new float[4];

        Matrix.multiplyMV(x1r, 0, finalMatrix, 0, x1, 0);
        Matrix.multiplyMV(x2r, 0, finalMatrix, 0, x2, 0);
        Matrix.multiplyMV(x3r, 0, finalMatrix, 0, x3, 0);
        Matrix.multiplyMV(x4r, 0, finalMatrix, 0, x4, 0);

        devideByw(x1r);
        devideByw(x2r);
        devideByw(x3r);
        devideByw(x4r);

        float signOfTrig = (x2r[0] - x1r[0]) * (x3r[1] - x1r[1]) - (x2r[1] - x1r[1]) * (x3r[0] - x1r[0]);
        float signOfAB = (x2r[0] - x1r[0]) * (y - x1r[1]) - (x2r[1] - x1r[1]) * (x - x1r[0]);
        float signOfCA = (x1r[0] - x3r[0]) * (y - x3r[1]) - (x1r[1] - x3r[1]) * (x - x3r[0]);
        float signOfBC = (x3r[0] - x2r[0]) * (y - x3r[1]) - (x3r[1] - x2r[1]) * (x - x3r[0]);

        boolean d1 = (signOfAB * signOfTrig > 0);
        boolean d2 = (signOfCA * signOfTrig > 0);
        boolean d3 = (signOfBC * signOfTrig > 0);

        boolean b1 =  d1 && d2 && d3;

        float signOfTrig2 = (x3r[0] - x2r[0]) * (x4r[1] - x2r[1]) - (x3r[1] - x2r[1]) * (x4r[0] - x2r[0]);
        float signOfCB = (x3r[0] - x2r[0]) * (y - x2r[1]) - (x3r[1] - x2r[1]) * (x - x2r[0]);
        float signOfDB = (x2r[0] - x4r[0]) * (y - x4r[1]) - (x2r[1] - x4r[1]) * (x - x4r[0]);
        float signOfDC = (x4r[0] - x3r[0]) * (y - x4r[1]) - (x4r[1] - x3r[1]) * (x - x4r[0]);

        boolean d4 = (signOfCB * signOfTrig2 > 0);
        boolean d5 = (signOfDB * signOfTrig2 > 0);
        boolean d6 = (signOfDC * signOfTrig2 > 0);

        boolean b2 = d4 && d5 && d6;

        return b1 || b2;
    }

    // add the marker drawed into markerlist
    public void setMarkerDrawed(float x, float y) throws CloneNotSupportedException {

        if(solveMarkerCenter(x, y) != null) {

//            float [] dis = {-0.01f, 0.01f, 0, 0.01f, 0.01f, 0.01f, -0.01f, 0, 0.01f, 0, -0.01f, -0.01f, 0, -0.01f, 0.01f, -0.01f};
            float[] new_marker = solveMarkerCenter(x, y);
//            float intensity = Sample3d(new_marker[0], new_marker[1], new_marker[2]);
//
//            for (int i = 0; i < 8; i++){
//                float [] temp_marker = solveMarkerCenter(x + dis[i * 2], y + dis[i * 2 + 1]);
//                if (temp_marker == null){
//                    continue;
//                }
//                if (Sample3d(temp_marker[0], temp_marker[1], temp_marker[2]) > intensity){
//                    intensity = Sample3d(temp_marker[0], temp_marker[1], temp_marker[2]);
//                    new_marker[0] = temp_marker[0];
//                    new_marker[1] = temp_marker[1];
//                    new_marker[2] = temp_marker[2];
//                }
//            }

//            if (Sample3d(new_marker[0], new_marker[1], new_marker[2]) <= 45){
//                return;
//            }

            ImageMarker imageMarker_drawed = new ImageMarker(new_marker[0],
                    new_marker[1],
                    new_marker[2]);

            imageMarker_drawed.type = lastMarkerType;
            System.out.println("set type to 3");

            MarkerList tempMarkerList = markerList.clone();
            V_NeuronSWC_list tempCurveList = curSwcList.clone();

            if (curUndo < UNDO_LIMIT){
                curUndo += 1;
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            } else {
                undoMarkerList.remove(0);
                undoCurveList.remove(0);
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            }

            markerList.add(imageMarker_drawed);

//            if (process.size() < UNDO_LIMIT){
//                process.add(Operate.DRAWMARKER);
//                undoDrawMarkerList.add(imageMarker_drawed);
//            } else {
//                Operate first = process.firstElement();
//                process.remove(0);
//                process.add(Operate.DRAWMARKER);
//                removeFirstUndo(first);
//                undoDrawMarkerList.add(imageMarker_drawed);
//            }
        }
    }

    void removeFirstUndo(Operate first){
        if (first == Operate.DRAWMARKER) {
            undoDrawMarkerList.remove(0);
        } else if (first == Operate.DELETEMARKER){
            undoDeleteMarkerList.remove(0);
        } else if (first == Operate.DRAWCURVE){
            undoDrawList.remove(0);
        } else if (first == Operate.DELETECURVE){
            undoDeleteList.remove(0);
        } else if (first == Operate.CHANGELINETYPE) {
            undoLineType.remove(0);
            undoChangeLineTypeIndex.remove(0);
        }
    }

    // delete the marker drawed from the markerlist
    public void deleteMarkerDrawed(float x, float y) throws CloneNotSupportedException {
        for (int i = 0; i < markerList.size(); i++){
            ImageMarker tobeDeleted = markerList.get(i);
            float[] markerModel = VolumetoModel(new float[]{tobeDeleted.x,tobeDeleted.y,tobeDeleted.z});
            float [] position = new float[4];
            position[0] = markerModel[0];
            position[1] = markerModel[1];
            position[2] = markerModel[2];
            position[3] = 1.0f;

            float [] positionVolumne = new float[4];
            Matrix.multiplyMV(positionVolumne, 0, finalMatrix, 0, position, 0);
            devideByw(positionVolumne);

            float dx = Math.abs(positionVolumne[0] - x);
            float dy = Math.abs(positionVolumne[1] - y);

            if (dx < 0.08 && dy < 0.08){
                ImageMarker temp = markerList.get(i);

                MarkerList tempMarkerList = markerList.clone();
                V_NeuronSWC_list tempCurveList = curSwcList.clone();

                if (curUndo < UNDO_LIMIT){
                    curUndo += 1;
                    undoMarkerList.add(tempMarkerList);
                    undoCurveList.add(tempCurveList);
                } else {
                    undoMarkerList.remove(0);
                    undoCurveList.remove(0);
                    undoMarkerList.add(tempMarkerList);
                    undoCurveList.add(tempCurveList);
                }

                markerList.remove(i);
//                if (process.size() < UNDO_LIMIT){
//                    process.add(Operate.DELETEMARKER);
//                    undoDeleteMarkerList.add(temp);
//                } else{
//                    Operate first = process.firstElement();
//                    process.remove(0);
//                    process.add(Operate.DELETEMARKER);
//                    removeFirstUndo(first);
//                    undoDeleteMarkerList.add(temp);
//                }
                break;
            }
        }
    }

    public void changeMarkerType(float x, float y) throws CloneNotSupportedException {
        for (int i = 0; i < markerList.size(); i++){
            ImageMarker tobeDeleted = markerList.get(i);
            float[] markerModel = VolumetoModel(new float[]{tobeDeleted.x,tobeDeleted.y,tobeDeleted.z});
            float [] position = new float[4];
            position[0] = markerModel[0];
            position[1] = markerModel[1];
            position[2] = markerModel[2];
            position[3] = 1.0f;

            float [] positionVolumne = new float[4];
            Matrix.multiplyMV(positionVolumne, 0, finalMatrix, 0, position, 0);
            devideByw(positionVolumne);

            float dx = Math.abs(positionVolumne[0] - x);
            float dy = Math.abs(positionVolumne[1] - y);

            if (dx < 0.08 && dy < 0.08){

                MarkerList tempMarkerList = markerList.clone();
                V_NeuronSWC_list tempCurveList = curSwcList.clone();

                if (curUndo < UNDO_LIMIT){
                    curUndo += 1;
                    undoMarkerList.add(tempMarkerList);
                    undoCurveList.add(tempCurveList);
                } else {
                    undoMarkerList.remove(0);
                    undoCurveList.remove(0);
                    undoMarkerList.add(tempMarkerList);
                    undoCurveList.add(tempCurveList);
                }

                ImageMarker temp = markerList.get(i);
                temp.type = lastMarkerType;

                break;
            }
        }
    }

    public void changeAllMarkerType() throws CloneNotSupportedException {

        MarkerList tempMarkerList = markerList.clone();
        V_NeuronSWC_list tempCurveList = curSwcList.clone();

        if (curUndo < UNDO_LIMIT){
            curUndo += 1;
            undoMarkerList.add(tempMarkerList);
            undoCurveList.add(tempCurveList);
        } else {
            undoMarkerList.remove(0);
            undoCurveList.remove(0);
            undoMarkerList.add(tempMarkerList);
            undoCurveList.add(tempCurveList);
        }

        for (int i = 0; i < markerList.size(); i++){
            markerList.get(i).type = lastMarkerType;
        }
    }


    /**
     * set the img info for volume img
     */
    private void setImage(){

        if (fileType == FileType.V3draw){
            img = Image4DSimple.loadImage(filepath, ".V3DRAW");
            if (img == null){
                return;
            }
        }else if (fileType == FileType.TIF){
            img = Image4DSimple.loadImage(filepath, ".TIF");
            if (img == null){
                return;
            }
        }else if (fileType == FileType.V3dPBD){
            img = Image4DSimple.loadImage(filepath, ".V3DPBD");
            if (img == null){
                return;
            }
        }

        Log.v(TAG,"Before myPattern.free()");

        if (myPattern != null){
            Log.v(TAG,"myPattern.free()");
            myPattern.free();
        }
        myPattern = null;
        grayscale =  img.getData();

        data_length = img.getDatatype().ordinal();
        isBig = img.getIsBig();

        sz[0] = (int)img.getSz0();
        sz[1] = (int)img.getSz1();
        sz[2] = (int)img.getSz2();

        Integer[] num = {sz[0], sz[1], sz[2]};
        float max_dim = (float) Collections.max(Arrays.asList(num));
        Log.v("MyRenderer", Float.toString(max_dim));

        mz[0] = (float) sz[0]/max_dim;
        mz[1] = (float) sz[1]/max_dim;
        mz[2] = (float) sz[2]/max_dim;

        Log.v("MyRenderer", Arrays.toString(sz));
        Log.v("MyRenderer", Arrays.toString(mz));

    }

    private void SetImage_Bigdata(int[] index){
        img = Image4DSimple.loadImage_Bigdata(filepath, index);
        if (img == null)
            return;

        myPattern = null;

        grayscale =  img.getData();

        data_length = img.getDatatype().ordinal();
        isBig = img.getIsBig();

        sz[0] = (int)img.getSz0();
        sz[1] = (int)img.getSz1();
        sz[2] = (int)img.getSz2();

        Integer[] num = {sz[0], sz[1], sz[2]};
        float max_dim = (float) Collections.max(Arrays.asList(num));
        Log.v("MyRenderer", Float.toString(max_dim));

        mz[0] = (float) sz[0]/max_dim;
        mz[1] = (float) sz[1]/max_dim;
        mz[2] = (float) sz[2]/max_dim;

        Log.v("MyRenderer", Arrays.toString(sz));
        Log.v("MyRenderer", Arrays.toString(mz));
    }


    public void ResetImg(Image4DSimple new_img){

        img = new_img;
        myPattern = null;

        grayscale =  img.getData();

        data_length = img.getDatatype().ordinal();
        isBig = img.getIsBig();

        sz[0] = (int)img.getSz0();
        sz[1] = (int)img.getSz1();
        sz[2] = (int)img.getSz2();

        Integer[] num = {sz[0], sz[1], sz[2]};
        float max_dim = (float) Collections.max(Arrays.asList(num));
        Log.v("MyRenderer", Float.toString(max_dim));

        mz[0] = (float) sz[0]/max_dim;
        mz[1] = (float) sz[1]/max_dim;
        mz[2] = (float) sz[2]/max_dim;

        Log.v("MyRenderer", Arrays.toString(sz));
        Log.v("MyRenderer", Arrays.toString(mz));

        ifFileSupport = true;

    }


    private void setSWC(){

        Uri uri = Uri.parse(filepath);
        NeuronTree nt = NeuronTree.readSWC_file(uri);
        V_NeuronSWC seg_swc = nt.convertV_NeuronSWCFormat();
        curSwcList.append(seg_swc);


        sz[0] = 0;
        sz[1] = 0;
        sz[2] = 0;

        for(int i=0; i<curSwcList.seg.size(); i++){
            V_NeuronSWC seg = curSwcList.seg.get(i);

            for(int j=0; j<seg.row.size(); j++){

                V_NeuronSWC_unit node = seg.row.get(j);

                if (node.x > sz[0])
                    sz[0] = (int) node.x;
                if (node.y > sz[1])
                    sz[1] = (int) node.y;
                if (node.z > sz[2])
                    sz[2] = (int) node.z;

            }
        }

        sz[0] = (int) (1.2f * sz[0]);
        sz[1] = (int) (1.2f * sz[1]);
        sz[2] = (int) (1.2f * sz[2]);

        Integer[] num = {sz[0], sz[1], sz[2]};
        float max_dim = (float) Collections.max(Arrays.asList(num));
        Log.v("MyRenderer", Float.toString(max_dim));

        mz[0] = (float) sz[0]/max_dim;
        mz[1] = (float) sz[1]/max_dim;
        mz[2] = (float) sz[2]/max_dim;

    }


    private void setAPO(){
        ArrayList<ArrayList<Float>> apo = new ArrayList<ArrayList<Float>>();
        ApoReader apoReader = new ApoReader();

        Uri uri = Uri.parse(filepath);
        apo = apoReader.read(uri);
        ArrayList<ImageMarker> markerListLoaded = importApo(apo);

        sz[0] = 0;
        sz[1] = 0;
        sz[2] = 0;

        for(int i=0; i<markerListLoaded.size(); i++){
            ImageMarker marker = markerListLoaded.get(i);
            if (marker.x > sz[0])
                sz[0] = (int) marker.x;
            if (marker.y > sz[1])
                sz[1] = (int) marker.y;
            if (marker.z > sz[2])
                sz[2] = (int) marker.z;
        }

        sz[0] = (int) (1.2f * sz[0]);
        sz[1] = (int) (1.2f * sz[1]);
        sz[2] = (int) (1.2f * sz[2]);

        Integer[] num = {sz[0], sz[1], sz[2]};
        float max_dim = (float) Collections.max(Arrays.asList(num));
        Log.v("MyRenderer", Float.toString(max_dim));

        mz[0] = (float) sz[0]/max_dim;
        mz[1] = (float) sz[1]/max_dim;
        mz[2] = (float) sz[2]/max_dim;

    }

    private void setANO(){

        ArrayList<ArrayList<Float>> ano_apo = new ArrayList<ArrayList<Float>>();
        AnoReader anoReader = new AnoReader();
        ApoReader apoReader_1 = new ApoReader();

        ArrayList<ImageMarker> markerListLoaded = new ArrayList<>();

        Uri uri = Uri.parse(filepath);
        anoReader.read(uri);

        String swc_path = anoReader.getSwc_Path();
        String apo_path = anoReader.getApo_Path();

        Log.v("setANO","swc_path: " + swc_path);
        Log.v("setANO","apo_path: " + apo_path);

        try{

            NeuronTree nt2 = NeuronTree.readSWC_file(swc_path);
            ano_apo = apoReader_1.read(apo_path);
            importNeuronTree(nt2);
            markerListLoaded = importApo(ano_apo);

        }catch (Exception e){
            Toast.makeText(getContext(),"Fail to open File !",Toast.LENGTH_SHORT).show();
        }

        sz[0] = 0;
        sz[1] = 0;
        sz[2] = 0;

        for(int i=0; i<markerListLoaded.size(); i++){
            ImageMarker marker = markerListLoaded.get(i);
            if (marker.x > sz[0])
                sz[0] = (int) marker.x;
            if (marker.y > sz[1])
                sz[1] = (int) marker.y;
            if (marker.z > sz[2])
                sz[2] = (int) marker.z;
        }


        for(int i=0; i<curSwcList.seg.size(); i++){
            V_NeuronSWC seg = curSwcList.seg.get(i);

            for(int j=0; j<seg.row.size(); j++){

                V_NeuronSWC_unit node = seg.row.get(j);

                if (node.x > sz[0])
                    sz[0] = (int) node.x;
                if (node.y > sz[1])
                    sz[1] = (int) node.y;
                if (node.z > sz[2])
                    sz[2] = (int) node.z;

            }
        }

        sz[0] = (int) (1.2f * sz[0]);
        sz[1] = (int) (1.2f * sz[1]);
        sz[2] = (int) (1.2f * sz[2]);

        Integer[] num = {sz[0], sz[1], sz[2]};
        float max_dim = (float) Collections.max(Arrays.asList(num));
        Log.v("MyRenderer", Float.toString(max_dim));

        mz[0] = (float) sz[0]/max_dim;
        mz[1] = (float) sz[1]/max_dim;
        mz[2] = (float) sz[2]/max_dim;

    }



    public void setNav_location(float[] neuron, float[] block, float[] size){

        float sz_img[] = new float[3];
        sz_img[0] = neuron[0];
        sz_img[1] = neuron[1];
        sz_img[2] = neuron[2];

        Float[] num = {sz_img[0], sz_img[1], sz_img[2]};
        float max_dim = Collections.max(Arrays.asList(num));
        Log.v("MyRenderer", Float.toString(max_dim));

        float[] sz_block = new float[6];
        sz_block[0] = block[0] - size[0]/2;
        sz_block[1] = block[0] + size[0]/2;
        sz_block[2] = block[1] - size[1]/2;
        sz_block[3] = block[1] + size[1]/2;
        sz_block[4] = block[2] - size[2]/2;
        sz_block[5] = block[2] + size[2]/2;

        mz_neuron[0] = sz_img[0]/max_dim;
        mz_neuron[1] = sz_img[1]/max_dim;
        mz_neuron[2] = sz_img[2]/max_dim;

        mz_block[0] = sz_block[0]/max_dim;
        mz_block[1] = sz_block[1]/max_dim;
        mz_block[2] = sz_block[2]/max_dim;
        mz_block[3] = sz_block[3]/max_dim;
        mz_block[4] = sz_block[4]/max_dim;
        mz_block[5] = sz_block[5]/max_dim;

        Log.v("MyRenderer", Arrays.toString(mz_neuron));
        Log.v("MyRenderer", Arrays.toString(mz_block));

        ifNavigationLococation = true;
        myNavLoc = null;

    }


    public boolean getNav_location_Mode(){
        return ifNavigationLococation;
    }

    public void setNav_location_Mode(){
        ifNavigationLococation = !ifNavigationLococation;

        if (!ifNavigationLococation){
            myNavLoc = null;
        }
    }

    public void quitNav_location_Mode(){
        ifNavigationLococation = false;
        myNavLoc = null;
    }


    //寻找marker点的位置~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public float[] solveMarkerCenter(float x, float y){

//        float [] result = new float[3];
        float [] loc1 = new float[3];
        float [] loc2 = new float[3];

//        get_NearFar_Marker(x, y, loc1, loc2);
        get_NearFar_Marker_2(x, y, loc1, loc2);

        Log.v("loc1",Arrays.toString(loc1));
        Log.v("loc2",Arrays.toString(loc2));

        float steps = 512;
        float [] step = devide(minus(loc1, loc2), steps);
        Log.v("step",Arrays.toString(step));


        if(make_Point_near(loc1, loc2)){
//            Log.v("loc1",Arrays.toString(loc1));
//            Log.v("loc2",Arrays.toString(loc2));

            float [] Marker = getCenterOfLineProfile(loc1, loc2);
//            float [] Marker = {60.1f, 63.2f, 63.6f};
            if (Marker == null){
                return null;
            }

            Log.v("Marker",Arrays.toString(Marker));

//            float intensity = Sample3d(Marker[0], Marker[1], Marker[2]);
//            Log.v("intensity",Float.toString(intensity));

            return Marker;
        }else {
            Log.v("solveMarkerCenter","please make sure the point inside the bounding box");
//            Looper.prepare();
            Toast.makeText(getContext(), "please make sure the point inside the bounding box", Toast.LENGTH_SHORT).show();
            return null;
        }


    }




    //类似于光线投射，找直线上强度最大的一点
    // in Image space (model space)
    private float[] getCenterOfLineProfile(float[] loc1, float[] loc2){

        float[] result = new float[3];
        float[] loc1_index = new float[3];
        float[] loc2_index = new float[3];
        boolean isInBoundingBox = false;

//        for(int i=0; i<3; i++){
//            loc1_index[i] = loc1[i] * sz[i];
//            loc2_index[i] = loc2[i] * sz[i];
//        }

        loc1_index = ModeltoVolume(loc1);
        loc2_index = ModeltoVolume(loc2);

//        float f = 0.8f;

        float[] d = minus(loc1_index, loc2_index);
        normalize(d);

        float[][] dim = new float[3][2];
        for(int i=0; i<3; i++){
            dim[i][0] = 0;
            dim[i][1] = sz[i] - 1;
        }



//        for(int i=0; i<2; i++){
//            loc1_index[i] = (1.0f - loc1[i]) * sz[2 - i];
//            loc2_index[i] = (1.0f - loc2[i]) * sz[2 - i];
//        }
//
//
//        loc1_index[2] = loc1[2] * sz[0];
//        loc2_index[2] = loc2[2] * sz[0];


        result = devide(plus(loc1_index, loc2_index), 2);

        float max_value = 0f;

        //单位向量
//        float[] d = minus(loc1_index, loc2_index);
//        normalize(d);

        Log.v("getCenterOfLineProfile:", "step: " + Arrays.toString(d));

        //判断是不是一个像素
        float length = distance(loc1_index, loc2_index);
        if(length < 0.5)
            return result;

        int nstep = (int)(length+0.5);
        float one_step = length/nstep;

        Log.v("getCenterOfLineProfile", Float.toString(one_step));

//            float[][] dim = new float[3][2];
//            for(int i=0; i<3; i++){
//                dim[i][0] = 0;
//                dim[i][1] = sz[i] - 1;
//            }


//            float[] sum_loc = {0, 0, 0};
//            float sum = 0;
        float[] poc;
        for (int i = 0; i <= nstep; i++) {

            float value;

            poc = minus(loc1_index, multiply(d, one_step * i));
//            poc = multiply(d, one_step);

//            Log.v("getCenterOfLineProfile:", "update the max");

//            Log.v("getCenterOfLineProfile", "(" + poc[0] + "," + poc[1] + "," + poc[2] + ")");


            if (IsInBoundingBox(poc, dim)) {

                value = Sample3d(poc[0], poc[1], poc[2]);
//                    sum_loc[0] += poc[0] * value;
//                    sum_loc[1] += poc[1] * value;
//                    sum_loc[2] += poc[2] * value;
//                    sum += value;
                isInBoundingBox = true;
                if(value > max_value){
//                    Log.v("getCenterOfLineProfile", "(" + poc[0] + "," + poc[1] + "," + poc[2] + "): " +value);
//                    Log.v("getCenterOfLineProfile:", "update the max");
                    max_value = value;
                    for (int j = 0; j < 3; j++){
                        result[j] = poc[j];
                    }
                    isInBoundingBox = true;
                }
            }
        }

//            if (sum != 0) {
//                result[0] = sum_loc[0] / sum;
//                result[1] = sum_loc[1] / sum;
//                result[2] = sum_loc[2] / sum;
//            }else{
//                break;
//            }

//            for (int k = 0; k < 3; k++){
//                loc1_index[k] = result[k] + d[k] * (length * f / 2);
//                loc2_index[k] = result[k] - d[k] * (length * f / 2);
//            }


        if(!isInBoundingBox){
            Toast.makeText(getContext(), "please make sure the point inside the bounding box", Toast.LENGTH_SHORT).show();
            return null;
        }

        return result;
    }




    //
    private void get_NearFar_Marker(float x, float y, float [] res1, float [] res2){

        //mvp矩阵的逆矩阵
        float [] invertfinalMatrix = new float[16];

        Matrix.invertM(invertfinalMatrix, 0, finalMatrix, 0);
        Log.v("invert_rotation",Arrays.toString(invertfinalMatrix));

        float [] near = new float[4];
        float [] far = new float[4];

        Matrix.multiplyMV(near, 0, invertfinalMatrix, 0, new float [] {x, y, -1, 1}, 0);
        Matrix.multiplyMV(far, 0, invertfinalMatrix, 0, new float [] {x, y, 0, 1}, 0);

        Log.v("near",Arrays.toString(near));
        Log.v("far",Arrays.toString(far));

        for(int i=0; i<3; i++){
            res1[i] = near[i];
            res2[i] = far[i];
        }

    }



    //用于透视投影中获取近平面和远平面的焦点
    private void get_NearFar_Marker_2(float x, float y, float [] res1, float [] res2){

        //mvp矩阵的逆矩阵
        float [] invertfinalMatrix = new float[16];

        Matrix.invertM(invertfinalMatrix, 0, finalMatrix, 0);
//        Log.v("invert_rotation",Arrays.toString(invertfinalMatrix));

        float [] near = new float[4];
        float [] far = new float[4];

        Matrix.multiplyMV(near, 0, invertfinalMatrix, 0, new float [] {x, y, -1, 1}, 0);
        Matrix.multiplyMV(far, 0, invertfinalMatrix, 0, new float [] {x, y, 1, 1}, 0);

        devideByw(near);
        devideByw(far);

//        Log.v("near",Arrays.toString(near));
//        Log.v("far",Arrays.toString(far));

        for(int i=0; i<3; i++){
            res1[i] = near[i];
            res2[i] = far[i];
        }

    }





    //找到靠近boundingbox的两处端点
    private boolean make_Point_near(float[] loc1, float[] loc2){

        float steps = 512;
        float [] near = loc1;
        float [] far = loc2;
        float [] step = devide(minus(near, far), steps);

        float[][] dim = new float[3][2];
        for(int i=0; i<3; i++){
            dim[i][0]= 0;
            dim[i][1]= mz[i];
        }

        int num = 0;
        while(num<steps && !IsInBoundingBox(near, dim)){
            near = minus(near, step);
            num++;
        }
        if(num == steps)
            return false;


        while(!IsInBoundingBox(far, dim)){
            far = plus(far, step);
        }

        near = plus(near, step);
        far = minus(far, step);

        for(int i=0; i<3; i++){
            loc1[i] = near[i];
            loc2[i] = far[i];
        }

//        Log.v("make_point_near","here we are");
        return true;

    }



    //找到靠近boundingbox的两处端点
    private boolean make_Point_near_2(float[] loc1, float[] loc2){

        float steps = 512;
        float [] near = loc1;
        float [] far = loc2;
        float [] step = devide(minus(near, far), steps);

        float[][] dim = new float[3][2];
        for(int i=0; i<3; i++){
            dim[i][0]= 0;
            dim[i][1]= mz[i];
        }

        int num = 0;
        while(num<steps && !IsInBoundingBox(near, dim)){
            near = minus(near, step);
            num++;
        }
        if(num == steps)
            return false;


        while(!IsInBoundingBox(far, dim)){
            far = plus(far, step);
        }


        for(int i=0; i<3; i++){
            loc1[i] = near[i];
            loc2[i] = far[i];
        }

//        Log.v("make_point_near","here we are");
        return true;

    }


    //判断是否在图像内部了
    private boolean IsInBoundingBox(float[] x, float[][] dim){
        int length = x.length;

        for(int i=0; i<length; i++){
//            Log.v("IsInBoundingBox", Float.toString(x[i]));
            if(x[i]>=dim[i][1] || x[i]<=dim[i][0])
                return false;
        }
//        Log.v("IsInBoundingBox", Arrays.toString(x));
//        Log.v("IsInBoundingBox", Arrays.toString(dim));
        return true;
    }



    float Sample3d(float x, float y, float z){
        int x0, x1, y0, y1, z0, z1;
        x0 = (int) Math.floor(x);         x1 = (int) Math.ceil(x);
        y0 = (int) Math.floor(y);         y1 = (int) Math.ceil(y);
        z0 = (int) Math.floor(z);         z1 = (int) Math.ceil(z);

        float xf, yf, zf;
        xf = x-x0;
        yf = y-y0;
        zf = z-z0;

        float [][][] is = new float[2][2][2];
        is[0][0][0] = grayData(x0, y0, z0);
        is[0][0][1] = grayData(x0, y0, z1);
        is[0][1][0] = grayData(x0, y1, z0);
        is[0][1][1] = grayData(x0, y1, z1);
        is[1][0][0] = grayData(x1, y0, z0);
        is[1][0][1] = grayData(x1, y0, z1);
        is[1][1][0] = grayData(x1, y1, z0);
        is[1][1][1] = grayData(x1, y1, z1);

        float [][][] sf = new float[2][2][2];
        sf[0][0][0] = (1-xf)*(1-yf)*(1-zf);
        sf[0][0][1] = (1-xf)*(1-yf)*(  zf);
        sf[0][1][0] = (1-xf)*(  yf)*(1-zf);
        sf[0][1][1] = (1-xf)*(  yf)*(  zf);
        sf[1][0][0] = (  xf)*(1-yf)*(1-zf);
        sf[1][0][1] = (  xf)*(1-yf)*(  zf);
        sf[1][1][0] = (  xf)*(  yf)*(1-zf);
        sf[1][1][1] = (  xf)*(  yf)*(  zf);

        float result = 0f;

        for(int i=0; i<2; i++)
            for(int j=0; j<2; j++)
                for(int k=0; k<2; k++)
                    result +=  is[i][j][k] * sf[i][j][k];

//        for(int i=0; i<2; i++)
//            for(int j=0; j<2; j++)
//                for(int k=0; k<2; k++)
//                    Log.v("Sample3d", Float.toString(is[i][j][k]));

        return result;
    }

    private int grayData(int x, int y, int z){
        int result = 0;
        if (data_length == 1){
            byte b = grayscale[z * sz[0] * sz[1] + y * sz[0] + x];
            result = ByteTranslate.byte1ToInt(b);
        }else if (data_length == 2){
            byte [] b = new byte[2];
            b[0] = grayscale[(z * sz[0] * sz[1] + y * sz[0] + x) * 2];
            b[1] = grayscale[(z * sz[0] * sz[1] + y * sz[0] + x) * 2 + 1];
            result = ByteTranslate.byte2ToInt(b, isBig);
        }else if (data_length == 4){
            byte [] b = new byte[4];
            b[0] = grayscale[(z * sz[0] * sz[1] + y * sz[0] + x) * 4];
            b[1] = grayscale[(z * sz[0] * sz[1] + y * sz[0] + x) * 4 + 1];
            b[2] = grayscale[(z * sz[0] * sz[1] + y * sz[0] + x) * 4 + 2];
            b[3] = grayscale[(z * sz[0] * sz[1] + y * sz[0] + x) * 4 + 3];
            result = ByteTranslate.byte2ToInt(b, isBig);
        }
        return result;
    }


    private float distance(float[] x, float[] y){
        int length = x.length;
        float sum = 0;

        for(int i=0; i<length; i++){
            sum += Math.pow(x[i]-y[i], 2);
        }
        return (float)Math.sqrt(sum);
    }

    private void normalize(float[] x){
        int length = x.length;
        float sum = 0;

        for(int i=0; i<length; i++)
            sum += Math.pow(x[i], 2);

        for(int i=0; i<length; i++)
            x[i] = x[i] / (float)Math.sqrt(sum);
    }



    public float[] ModeltoVolume(float[] input){
        if (input == null)
            return null;

        float[] result = new float[3];
        result[0] = (1.0f - input[0] / mz[0]) * sz[0];
        result[1] = (1.0f - input[1] / mz[1]) * sz[1];
        result[2] = input[2] / mz[2] * sz[2];

        return result;
    }

    public float[] VolumetoModel(float[] input){
        if (input == null)
            return null;

        float[] result = new float[3];
        result[0] = (sz[0] - input[0]) / sz[0] * mz[0];
        result[1] = (sz[1] - input[1]) / sz[1] * mz[1];
        result[2] = input[2] / sz[2] * mz[2];

        return result;
    }


    //减法运算
    private float [] minus(float[] x, float[] y){
        if(x.length != y.length){
            Log.v("minus","length is not the same!");
            return null;
        }

        int length = x.length;
        float [] result = new float[length];

        for (int i=0; i<length; i++)
            result[i] = x[i] - y[i];
        return result;
    }

    //加法运算
    private float [] plus(float[] x, float[] y){
        if(x.length != y.length){
            Log.v("plus","length is not the same!");
            return null;
        }

        int length = x.length;
        float [] result = new float[length];

        for (int i=0; i<length; i++)
            result[i] = x[i] + y[i];
        return result;
    }

    //除法运算
    private float [] devide(float[] x, float num){
        if(num == 0){
            Log.v("devide","can not be devided by 0");
        }

        int length = x.length;
        float [] result = new float[length];

        for(int i=0; i<length; i++)
            result[i] = x[i]/num;

        return result;
    }


    //除法运算
    private void devideByw(float[] x){
        if(Math.abs(x[3]) < 0.000001f){
            Log.v("devideByw","can not be devided by 0");
            return;
        }

        for(int i=0; i<3; i++)
            x[i] = x[i]/x[3];

    }

    //除法运算
    private float [] multiply(float[] x, float num){
        if(num == 0){
            Log.v("multiply","can not be multiply by 0");
        }

        int length = x.length;
        float [] result = new float[length];

        for(int i=0; i<length; i++)
            result[i] = x[i] * num;

        return result;
    }



    private ArrayList<Float> getLineDrawed(float [] line){
        float head_x = line[0];
        float head_y = line[1];
//        float [] result = new float[line.length];
        ArrayList<Float> result = new ArrayList<Float>();
        float [] head_result = solveMarkerCenter(head_x, head_y);
        if (head_result == null){
            return null;
        }
        for (int i = 0; i < 3; i++){
//            result[i] = head_result[i];
            result.add(head_result[i]);
        }//计算第一个点在物体坐标系的位置并保存

        float [] ex_head_result  = {head_result[0], head_result[1], head_result[2], 1.0f};
        float [] head_point = new float[4];
        Matrix.multiplyMV(head_point, 0, finalMatrix, 0,ex_head_result, 0);
        float current_z = head_point[2];

        for (int i = 1; i < line.length/3; i++){
            float x = line[i * 3];
            float y = line[i * 3 + 1];
            float [] mid_point = {x, y, current_z, 1.0f};
            float [] front_point = {x, y, -1.0f, 1.0f};

            float [] invertfinalMatrix = new float[16];
            Matrix.invertM(invertfinalMatrix, 0, finalMatrix, 0);

            float [] temp1 = new float[4];
            float [] temp2 = new float[4];
            Matrix.multiplyMV(temp1, 0, invertfinalMatrix, 0, mid_point, 0);
            Matrix.multiplyMV(temp2, 0, invertfinalMatrix, 0, front_point, 0);

            float [] mid_point_pixel = new float[3];
            float [] front_point_pixel = new float[3];
            mid_point_pixel = ModeltoVolume(temp1);
            front_point_pixel = ModeltoVolume(temp2);
//            for (int j = 0; j < 3; j++){
//                mid_point_pixel[j] = temp1[j] * sz[j];
//                front_point_pixel[j] = temp2[j] * sz[j];
//            }

            float [] dir = minus(front_point_pixel, mid_point_pixel);
            normalize(dir);

            float[][] dim = new float[3][2];
            for(int j = 0; j < 3; j++){
                dim[j][0] = 0;
                dim[j][1] = sz[j] - 1;
            }

            float value = 0;
            float [] result_pos = new float[3];
            for (int j = 0; j < 128; j++){
//                Log.v("getLineDrawed","~~~~~~~~~~~~~~");
                float [] pos = minus(mid_point_pixel, multiply(dir, (float)(j)));

                if (IsInBoundingBox(pos, dim)){
                    float current_value = Sample3d(pos[0], pos[1], pos[2]);
                    if (current_value > value){
                        value = current_value;
                        result_pos[0] = pos[0];
                        result_pos[1] = pos[1];
                        result_pos[2] = pos[2];
                    }
                }else{
                    break;
                }
            }
            for (int j = 0; j < 128; j++){
                float [] pos = plus(mid_point_pixel, multiply(dir, (float)(j)));

                if (IsInBoundingBox(pos, dim)){
                    float current_value = Sample3d(pos[0], pos[1], pos[2]);
                    if (current_value > value){
                        value = current_value;
                        result_pos[0] = pos[0];
                        result_pos[1] = pos[1];
                        result_pos[2] = pos[2];
                    }
                }else{
                    break;
                }
            }
            if (value == 0){
                break;
            }

            result_pos = VolumetoModel(result_pos);

            for (int j = 0; j < 3; j++){
                result.add(result_pos[j]);
            }

            float [] ex_result_pos = {result_pos[0], result_pos[1], result_pos[2], 1.0f};
            float [] current_pos = new float[4];
            Matrix.multiplyMV(current_pos, 0, finalMatrix, 0, ex_result_pos, 0);
            current_z = current_pos[2];
//            for (int j = 0; j < 3; j++){
////                result[i * 3 + j] = result_pos[j];
//                result.add(result_pos[j] / sz[j]);
//            }
//            current_z = result_pos[2];
        }
        return result;
    }





    private ArrayList<Float> getLineDrawed_2(float [] line){
        float head_x = line[0];
        float head_y = line[1];
//        float [] result = new float[line.length];
        ArrayList<Float> result = new ArrayList<Float>();
        float [] head_result = VolumetoModel(solveMarkerCenter(head_x, head_y));
        if (head_result == null){
            return null;
        }
//        System.out.println("getLineDrawed2");
        for (int i = 0; i < 3; i++){
//            result[i] = head_result[i];
            result.add(head_result[i]);
        }//计算第一个点在物体坐标系的位置并保存

        float [] ex_head_result  = {head_result[0], head_result[1], head_result[2], 1.0f};
        float [] head_point = new float[4];
        Matrix.multiplyMV(head_point, 0, finalMatrix, 0,ex_head_result, 0);
        float current_z = head_point[2]/head_point[3];

        for (int i = 1; i < line.length/3; i++){
            float x = line[i * 3];
            float y = line[i * 3 + 1];
            float [] mid_point = {x, y, current_z, 1.0f};
            float [] front_point = {x, y, -1.0f, 1.0f};

            float [] invertfinalMatrix = new float[16];
            Matrix.invertM(invertfinalMatrix, 0, finalMatrix, 0);

            float [] temp1 = new float[4];
            float [] temp2 = new float[4];
            Matrix.multiplyMV(temp1, 0, invertfinalMatrix, 0, mid_point, 0);
            Matrix.multiplyMV(temp2, 0, invertfinalMatrix, 0, front_point, 0);

            devideByw(temp1);
            devideByw(temp2);

            float [] mid_point_pixel = new float[3];
            float [] front_point_pixel = new float[3];
            mid_point_pixel = ModeltoVolume(temp1);
            front_point_pixel = ModeltoVolume(temp2);
//            for (int j = 0; j < 3; j++){
//                mid_point_pixel[j] = temp1[j] * sz[j];
//                front_point_pixel[j] = temp2[j] * sz[j];
//            }

            float [] dir = minus(front_point_pixel, mid_point_pixel);
            normalize(dir);

            float[][] dim = new float[3][2];
            for(int j = 0; j < 3; j++){
                dim[j][0] = 0;
                dim[j][1] = sz[j] - 1;
            }

            float value = 0;
            float [] result_pos = new float[3];
            for (int j = 1; j < 30; j++){
//                Log.v("getLineDrawed","~~~~~~~~~~~~~~");
                float [] pos = minus(mid_point_pixel, multiply(dir, (float)(j)));

                if (IsInBoundingBox(pos, dim)){
                    float current_value = Sample3d(pos[0], pos[1], pos[2]);
                    if (current_value > value){
                        value = current_value;
                        result_pos[0] = pos[0];
                        result_pos[1] = pos[1];
                        result_pos[2] = pos[2];
                    }
                }else{
                    break;
                }
            }
            for (int j = 1; j < 30; j++){
                float [] pos = plus(mid_point_pixel, multiply(dir, (float)(j)));

                if (IsInBoundingBox(pos, dim)){
                    float current_value = Sample3d(pos[0], pos[1], pos[2]);
                    if (current_value > value){
                        value = current_value;
                        result_pos[0] = pos[0];
                        result_pos[1] = pos[1];
                        result_pos[2] = pos[2];
                    }
                }else{
                    break;
                }
            }
            if (value == 0){
                break;
            }

            result_pos = VolumetoModel(result_pos);

            for (int j = 0; j < 3; j++){
                result.add(result_pos[j]);
            }

            float [] ex_result_pos = {result_pos[0], result_pos[1], result_pos[2], 1.0f};
            float [] current_pos = new float[4];
            Matrix.multiplyMV(current_pos, 0, finalMatrix, 0, ex_result_pos, 0);
            current_z = current_pos[2]/current_pos[3];
//            for (int j = 0; j < 3; j++){
////                result[i * 3 + j] = result_pos[j];
//                result.add(result_pos[j] / sz[j]);
//            }
//            current_z = result_pos[2];
        }
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Vector<MyMarker> solveCurveMarkerLists_fm(ArrayList<Float> listCurvePos){
        Vector<MyMarker> outswc = new Vector<MyMarker>();
        if (listCurvePos.isEmpty()) {
            System.out.println("You enter an empty curve for solveCurveMarkerLists_fm(). Check your code.\n");
            return null;
        }

        int szx = sz[0];
        int szy = sz[1];
        int szz = sz[2];

        XYZ sub_orig;
        double [] psubdata;
        int sub_szx, sub_szy, sub_szz;

        Vector<MyMarker> nearpos_vec = new Vector<MyMarker>();
        Vector<MyMarker> farpos_vec = new Vector<MyMarker>();
        nearpos_vec.clear();
        farpos_vec.clear();

        int N = listCurvePos.size() / 3;
        int firstPointIndex = 0;

        Vector<Integer> inds = new Vector<>();
        inds = resampleCurveStroke(listCurvePos);

        for (firstPointIndex = 0; firstPointIndex < N; firstPointIndex++){
            float [] loc_near = new float[3];
            float [] loc_far = new float[3];
            float [] cur_pos = {listCurvePos.get(firstPointIndex * 3), listCurvePos.get(firstPointIndex * 3 + 1), listCurvePos.get(firstPointIndex * 3 + 2)};
            get_NearFar_Marker_2(cur_pos[0], cur_pos[1], loc_near, loc_far);
            if (make_Point_near_2(loc_near, loc_far)){

                float[] loc_near_volume = ModeltoVolume(loc_near);
                float[] loc_far_volume = ModeltoVolume(loc_far);
                nearpos_vec.add(new MyMarker(loc_near_volume[0], loc_near_volume[1], loc_near_volume[2]));
                farpos_vec.add(new MyMarker(loc_far_volume[0], loc_far_volume[1], loc_far_volume[2]));

//                nearpos_vec.add(new MyMarker(loc_near[0], loc_near[1], loc_near[2]));
//                farpos_vec.add(new MyMarker(loc_far[0], loc_far[1], loc_far[2]));

                break;
            }else{
                continue;
            }
        }

        int last_i;
        for (int i = firstPointIndex; i < N; i++){
            boolean b_inds = false;

            if (inds.isEmpty()){
                b_inds = true;
            }else{
                if (inds.contains(i))
                    b_inds = true;
            }

            // only process resampled strokes
            if(i==1 || i==(N-1) || b_inds) { // make sure to include the last N-1 pos
                float[] cur_pos = {listCurvePos.get(i * 3), listCurvePos.get(i * 3 + 1), listCurvePos.get(i * 3 + 2)};
                float [] loc_near = new float[3];
                float [] loc_far = new float[3];
                get_NearFar_Marker_2(cur_pos[0], cur_pos[1], loc_near, loc_far);
                if (make_Point_near_2(loc_near, loc_far)){

                    float[] loc_near_volume = ModeltoVolume(loc_near);
                    float[] loc_far_volume = ModeltoVolume(loc_far);
                    nearpos_vec.add(new MyMarker(loc_near_volume[0], loc_near_volume[1], loc_near_volume[2]));
                    farpos_vec.add(new MyMarker(loc_far_volume[0], loc_far_volume[1], loc_far_volume[2]));

//                    nearpos_vec.add(new MyMarker(loc_near[0], loc_near[1], loc_near[2]));
//                    farpos_vec.add(new MyMarker(loc_far[0], loc_far[1], loc_far[2]));
                }
            }
        }
        outswc = FastMarching_Linker.fastmarching_drawing_serialboxes(nearpos_vec, farpos_vec, grayscale, outswc, szx, szy, szz, 1, 5, false, data_length, isBig);
        return outswc;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public synchronized void addLineDrawed2(ArrayList<Float> line) throws CloneNotSupportedException {
        if (img.getData() == null){
            return;
        }
        Vector<MyMarker> outswc = solveCurveMarkerLists_fm(line);

        if (outswc == null){

            Toast.makeText(getContext(), "Make sure the point is in boundingbox", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Float> lineAdded = new ArrayList<>();
        for (int i = 0; i < outswc.size(); i++){

            lineAdded.add((float)outswc.get(i).x);
            lineAdded.add((float)outswc.get(i).y);
            lineAdded.add((float)outswc.get(i).z);

//            System.out.println("( " + (int) outswc.get(i).x + "," + (int) outswc.get(i).y + "," + (int) outswc.get(i).z + " )");

//            float [] curswc = {(float)outswc.get(i).x, (float)outswc.get(i).y, (float)outswc.get(i).z};
//            VolumetoModel(curswc);
//
//            lineAdded.add(curswc[0]);
//            lineAdded.add(curswc[1]);
//            lineAdded.add(curswc[2]);

        }
        if (lineAdded != null){
//            lineDrawed.add(lineAdded);
            int max_n = curSwcList.maxnoden();
            V_NeuronSWC seg = new  V_NeuronSWC();
            for(int i=0; i < lineAdded.size()/3; i++){
                V_NeuronSWC_unit u = new V_NeuronSWC_unit();
                u.n = max_n + i+ 1;
                if(i==0)
                    u.parent = -1;
                else
                    u.parent = max_n + i;
//                float[] xyz = ModeltoVolume(new float[]{lineAdded.get(i*3+0),lineAdded.get(i*3+1),lineAdded.get(i*3+2)});
                float[] xyz = new float[]{lineAdded.get(i*3+0),lineAdded.get(i*3+1),lineAdded.get(i*3+2)};
                u.x = xyz[0];
                u.y = xyz[1];
                u.z = xyz[2];
                u.type = lastLineType;
                seg.append(u);
//                System.out.println("u n p x y z: "+ u.n +" "+u.parent+" "+u.x +" "+u.y+ " "+u.z);
            }
            if(seg.row.size()<3){
                return;
            }
            float[] headXYZ = new float[]{(float) seg.row.get(0).x, (float) seg.row.get(0).y, (float) seg.row.get(0).z};
            float[] tailXYZ = new float[]{(float) seg.row.get(seg.row.size()-1).x,
                    (float) seg.row.get(seg.row.size()-1).y,
                    (float) seg.row.get(seg.row.size()-1).z};
            boolean linked = false;
            for(int i=0; i<curSwcList.seg.size(); i++){
                V_NeuronSWC s = curSwcList.seg.get(i);
                for(int j=0; j<s.row.size(); j++){
                    if(linked)
                        break;
                    V_NeuronSWC_unit node = s.row.get(j);
                    float[] nodeXYZ = new float[]{(float) node.x, (float) node.y, (float) node.z};
                    if(distance(headXYZ,nodeXYZ)<5){
                        V_NeuronSWC_unit head = seg.row.get(0);
                        V_NeuronSWC_unit child = seg.row.get(1);
                        head.x = node.x;
                        head.y = node.y;
                        head.z = node.z;
                        head.n = node.n;
                        head.parent = node.parent;
                        child.parent = head.n;
                        linked = true;
                        break;
                    }
                    if(distance(tailXYZ,nodeXYZ)<5){
                        seg.reverse();
                        V_NeuronSWC_unit tail = seg.row.get(seg.row.size()-1);
                        V_NeuronSWC_unit child = seg.row.get(seg.row.size()-2);
                        tail.x = node.x;
                        tail.y = node.y;
                        tail.z = node.z;
                        tail.n = node.n;
                        tail.parent = node.parent;
                        child.n = tail.n;
                        linked = true;
                        break;
                    }
                }
            }

            MarkerList tempMarkerList = markerList.clone();
            V_NeuronSWC_list tempCurveList = curSwcList.clone();

            if (curUndo < UNDO_LIMIT){
                curUndo += 1;
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            } else {
                undoMarkerList.remove(0);
                undoCurveList.remove(0);
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            }

            curSwcList.append(seg);
//            if (process.size() < UNDO_LIMIT){
//                process.add(Operate.DRAWCURVE);
//                undoDrawList.add(seg);
//            } else{
//                Operate first = process.firstElement();
//                process.remove(0);
//                process.add(Operate.DRAWCURVE);
//                removeFirstUndo(first);
//                undoDrawList.add(seg);
//            }
//            Log.v("addLineDrawed", Integer.toString(lineAdded.size()));
        }
        else
            Log.v("draw line:::::", "nulllllllllllllllllll");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void addLineDrawed2(ArrayList<Float> line, V_NeuronSWC_list [] v_neuronSWC_lists, V_NeuronSWC background_seg) throws CloneNotSupportedException {
        if (img.getData() == null){
            return;
        }
        Vector<MyMarker> outswc = solveCurveMarkerLists_fm(line);

        if (outswc == null){

            Toast.makeText(getContext(), "Make sure the point is in boundingbox", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Float> lineAdded = new ArrayList<>();
        for (int i = 0; i < outswc.size(); i++){

            lineAdded.add((float)outswc.get(i).x);
            lineAdded.add((float)outswc.get(i).y);
            lineAdded.add((float)outswc.get(i).z);

//            System.out.println("( " + (int) outswc.get(i).x + "," + (int) outswc.get(i).y + "," + (int) outswc.get(i).z + " )");

//            float [] curswc = {(float)outswc.get(i).x, (float)outswc.get(i).y, (float)outswc.get(i).z};
//            VolumetoModel(curswc);
//
//            lineAdded.add(curswc[0]);
//            lineAdded.add(curswc[1]);
//            lineAdded.add(curswc[2]);

        }
        if (lineAdded != null){
//            lineDrawed.add(lineAdded);
            int max_n = curSwcList.maxnoden();
            V_NeuronSWC seg = new  V_NeuronSWC();
            for(int i=0; i < lineAdded.size()/3; i++){
                V_NeuronSWC_unit u = new V_NeuronSWC_unit();
                u.n = max_n + i+ 1;
                if(i==0){
                    u.parent = -1;
//                    System.out.println("--- u.parent = -1 ---");
                }
                else
                    u.parent = max_n + i;
//                float[] xyz = ModeltoVolume(new float[]{lineAdded.get(i*3+0),lineAdded.get(i*3+1),lineAdded.get(i*3+2)});
                float[] xyz = new float[]{lineAdded.get(i*3+0),lineAdded.get(i*3+1),lineAdded.get(i*3+2)};
                u.x = xyz[0];
                u.y = xyz[1];
                u.z = xyz[2];
                u.type = lastLineType;
                seg.append(u);
//                System.out.println("u n p x y z: "+ u.n +" "+u.parent+" "+u.x +" "+u.y+ " "+u.z);
            }
            if(seg.row.size()<3){
                return;
            }
            float[] headXYZ = new float[]{(float) seg.row.get(0).x, (float) seg.row.get(0).y, (float) seg.row.get(0).z};
            float[] tailXYZ = new float[]{(float) seg.row.get(seg.row.size()-1).x,
                    (float) seg.row.get(seg.row.size()-1).y,
                    (float) seg.row.get(seg.row.size()-1).z};
            boolean linked = false;
            for(int i=0; i<curSwcList.seg.size() - 1; i++){
                V_NeuronSWC s = curSwcList.seg.get(i);

                if (s == background_seg){
                    continue;
                }

                for(int j=0; j<s.row.size(); j++){
                    if(linked)
                        break;
                    V_NeuronSWC_unit node = s.row.get(j);
                    float[] nodeXYZ = new float[]{(float) node.x, (float) node.y, (float) node.z};
                    if(distance(headXYZ,nodeXYZ)<5){
                        V_NeuronSWC_unit head = seg.row.get(0);
                        V_NeuronSWC_unit child = seg.row.get(1);
                        head.x = node.x;
                        head.y = node.y;
                        head.z = node.z;
                        head.n = node.n;
                        head.parent = node.parent;
                        child.parent = head.n;
                        linked = true;
                        break;
                    }
                    if(distance(tailXYZ,nodeXYZ)<5){
                        seg.reverse();
                        V_NeuronSWC_unit tail = seg.row.get(seg.row.size()-1);
                        V_NeuronSWC_unit child = seg.row.get(seg.row.size()-2);
                        tail.x = node.x;
                        tail.y = node.y;
                        tail.z = node.z;
                        tail.n = node.n;
                        tail.parent = node.parent;
                        child.n = tail.n;
                        linked = true;
                        break;
                    }
                }
            }

            MarkerList tempMarkerList = markerList.clone();
            V_NeuronSWC_list tempCurveList = curSwcList.clone();

            if (curUndo < UNDO_LIMIT){
                curUndo += 1;
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            } else {
                undoMarkerList.remove(0);
                undoCurveList.remove(0);
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            }

            v_neuronSWC_lists[0] = tempCurveList;

            curSwcList.append(seg);
//            if (process.size() < UNDO_LIMIT){
//                process.add(Operate.DRAWCURVE);
//                undoDrawList.add(seg);
//            } else{
//                Operate first = process.firstElement();
//                process.remove(0);
//                process.add(Operate.DRAWCURVE);
//                removeFirstUndo(first);
//                undoDrawList.add(seg);
//            }
//            Log.v("addLineDrawed", Integer.toString(lineAdded.size()));
        }
        else
            Log.v("draw line:::::", "nulllllllllllllllllll");



//        for (int i = 0; i < curSwcList.nsegs(); i++){
//            V_NeuronSWC seg = curSwcList.seg.get(i);
//
//            System.out.println("seg " + i + ": ");
//            for (int j = 0; j < seg.row.size(); j++){
//                V_NeuronSWC_unit unit = seg.row.get(j);
//                System.out.println("node n: " + unit.n + ", parent: " + unit.parent);
//            }
//        }
    }

    private Vector<Integer> resampleCurveStroke(ArrayList<Float> listCurvePos){
        Vector<Integer> ids = new Vector<>();
        int N = listCurvePos.size() / 3;
        Vector<Double> maxval = new Vector<>();
        maxval.clear();

        for (int i = 0; i < N; i++){
            float [] curPos = {listCurvePos.get(i * 3), listCurvePos.get(i * 3 + 1), listCurvePos.get(i * 3 + 2)};
            float [] nearPos = new float[3];
            float [] farPos = new float[3];
            get_NearFar_Marker_2(curPos[0], curPos[1], nearPos, farPos);
            if (make_Point_near(nearPos, farPos)){
                float [] centerPos = getCenterOfLineProfile(nearPos, farPos);
                double value = Sample3d(centerPos[0], centerPos[1], centerPos[2]);
                maxval.add(value);
            }
        }

        Map<Double, Integer> max_score = new HashMap<>();
        for (int i = 0; i < maxval.size(); i++){
            max_score.put(maxval.get(i), i);
        }

        for (int val:max_score.values()){
            ids.add(val);
        }
        return ids;
    }



    public int addLineDrawed(ArrayList<Float> line){
        if (img.getData() == null){
            return -1;
        }
        ArrayList<Float> lineAdded;
        float [] lineCurrent = new float[line.size()];
        Log.v("addLineDrawed", Integer.toString(line.size()));
        for (int i = 0; i < line.size(); i++){
            lineCurrent[i] = line.get(i);
        }
//        lineAdded = getLineDrawed(lineCurrent);
        lineAdded = getLineDrawed_2(lineCurrent);

        if (lineAdded != null){
//            lineDrawed.add(lineAdded);
            int max_n = newSwcList.maxnoden();
            V_NeuronSWC seg = new  V_NeuronSWC();
            for(int i=0; i < lineAdded.size()/3; i++){
                V_NeuronSWC_unit u = new V_NeuronSWC_unit();
                u.n = max_n + i+ 1;
                if(i==0)
                    u.parent = -1;
                else
                    u.parent = max_n + i;
                float[] xyz = ModeltoVolume(new float[]{lineAdded.get(i*3+0),lineAdded.get(i*3+1),lineAdded.get(i*3+2)});
                u.x = xyz[0];
                u.y = xyz[1];
                u.z = xyz[2];
                u.type = lastLineType;
                seg.append(u);
//                System.out.println("u n p x y z: "+ u.n +" "+u.parent+" "+u.x +" "+u.y+ " "+u.z);
            }
            if(seg.row.size()<3){
                return -1;
            }
            float[] headXYZ = new float[]{(float) seg.row.get(0).x, (float) seg.row.get(0).y, (float) seg.row.get(0).z};
            float[] tailXYZ = new float[]{(float) seg.row.get(seg.row.size()-1).x,
                    (float) seg.row.get(seg.row.size()-1).y,
                    (float) seg.row.get(seg.row.size()-1).z};
            boolean linked = false;
            for(int i=0; i<newSwcList.seg.size(); i++){
                V_NeuronSWC s = newSwcList.seg.get(i);
                for(int j=0; j<s.row.size(); j++){
                    if(linked)
                        break;
                    V_NeuronSWC_unit node = s.row.get(j);
                    float[] nodeXYZ = new float[]{(float) node.x, (float) node.y, (float) node.z};
                    if(distance(headXYZ,nodeXYZ)<5){
                        V_NeuronSWC_unit head = seg.row.get(0);
                        V_NeuronSWC_unit child = seg.row.get(1);
                        head.x = node.x;
                        head.y = node.y;
                        head.z = node.z;
                        head.n = node.n;
                        head.parent = node.parent;
                        child.parent = head.n;
                        linked = true;
                        break;
                    }
                    if(distance(tailXYZ,nodeXYZ)<5){
                        seg.reverse();
                        V_NeuronSWC_unit tail = seg.row.get(seg.row.size()-1);
                        V_NeuronSWC_unit child = seg.row.get(seg.row.size()-2);
                        tail.x = node.x;
                        tail.y = node.y;
                        tail.z = node.z;
                        tail.n = node.n;
                        tail.parent = node.parent;
                        child.n = tail.n;
                        linked = true;
                        break;
                    }
                }
            }
            newSwcList.append(seg);
            return newSwcList.nsegs() - 1;

//            Log.v("addLineDrawed", Integer.toString(lineAdded.size()));
        }
        else {
            Log.v("draw line:::::", "nulllllllllllllllllll");
            return -1;
        }
    }

    public V_NeuronSWC addBackgroundLineDrawed(ArrayList<Float> line) throws CloneNotSupportedException {
        if (img.getData() == null){
            return null;
        }
        ArrayList<Float> lineAdded;
        float [] lineCurrent = new float[line.size()];
        Log.v("addLineDrawed", Integer.toString(line.size()));
        for (int i = 0; i < line.size(); i++){
            lineCurrent[i] = line.get(i);
        }
//        lineAdded = getLineDrawed(lineCurrent);
        lineAdded = getLineDrawed_2(lineCurrent);

        if (lineAdded != null){
//            lineDrawed.add(lineAdded);
//            float ave = 0;
//            for (int i = 0; i < lineAdded.size() / 3; i++){
//                float [] temp = {lineAdded.get(i * 3), lineAdded.get(i * 3 + 1), lineAdded.get(i * 3 + 2)};
//                temp = ModeltoVolume(temp);
//                ave += Sample3d(temp[0], temp[1], temp[2]);
//            }
//            ave /= lineAdded.size() / 3;
//            System.out.println("Average:::::::");
//            System.out.println(ave);
////            System.out.println(lineAdded.get(0));
//            if (ave < 41)
//                return null;
            int max_n = curSwcList.maxnoden();
            V_NeuronSWC seg = new  V_NeuronSWC();
            for(int i=0; i < lineAdded.size()/3; i++){
                V_NeuronSWC_unit u = new V_NeuronSWC_unit();
                u.n = max_n + i+ 1;
                if(i==0)
                    u.parent = -1;
                else
                    u.parent = max_n + i;
                float[] xyz = ModeltoVolume(new float[]{lineAdded.get(i*3+0),lineAdded.get(i*3+1),lineAdded.get(i*3+2)});
                u.x = xyz[0];
                u.y = xyz[1];
                u.z = xyz[2];
                u.type = lastLineType;
                seg.append(u);
//                System.out.println("u n p x y z: "+ u.n +" "+u.parent+" "+u.x +" "+u.y+ " "+u.z);
            }
            if(seg.row.size()<3){
                return null;
            }
            float[] headXYZ = new float[]{(float) seg.row.get(0).x, (float) seg.row.get(0).y, (float) seg.row.get(0).z};
            float[] tailXYZ = new float[]{(float) seg.row.get(seg.row.size()-1).x,
                    (float) seg.row.get(seg.row.size()-1).y,
                    (float) seg.row.get(seg.row.size()-1).z};
            boolean linked = false;
            for(int i=0; i<curSwcList.seg.size(); i++){
                V_NeuronSWC s = curSwcList.seg.get(i);
                for(int j=0; j<s.row.size(); j++){
                    if(linked)
                        break;
                    V_NeuronSWC_unit node = s.row.get(j);
                    float[] nodeXYZ = new float[]{(float) node.x, (float) node.y, (float) node.z};
                    if(distance(headXYZ,nodeXYZ)<5){
                        V_NeuronSWC_unit head = seg.row.get(0);
                        V_NeuronSWC_unit child = seg.row.get(1);
                        head.x = node.x;
                        head.y = node.y;
                        head.z = node.z;
                        head.n = node.n;
                        head.parent = node.parent;
                        child.parent = head.n;
                        linked = true;
                        break;
                    }
                    if(distance(tailXYZ,nodeXYZ)<5){
                        seg.reverse();
                        V_NeuronSWC_unit tail = seg.row.get(seg.row.size()-1);
                        V_NeuronSWC_unit child = seg.row.get(seg.row.size()-2);
                        tail.x = node.x;
                        tail.y = node.y;
                        tail.z = node.z;
                        tail.n = node.n;
                        tail.parent = node.parent;
                        child.n = tail.n;
                        linked = true;
                        break;
                    }
                }
            }

            MarkerList tempMarkerList = markerList.clone();
            V_NeuronSWC_list tempCurveList = curSwcList.clone();

            if (curUndo < UNDO_LIMIT){
                curUndo += 1;
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            } else {
                undoMarkerList.remove(0);
                undoCurveList.remove(0);
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            }

            curSwcList.append(seg);
//            if (process.size() < UNDO_LIMIT){
//                process.add(Operate.DRAWCURVE);
//                undoDrawList.add(seg);
//            } else{
//                Operate first = process.firstElement();
//                process.remove(0);
//                process.add(Operate.DRAWCURVE);
//                removeFirstUndo(first);
//                undoDrawList.add(seg);
//            }
            return seg;
//            return curSwcList.nsegs() - 1;

//            Log.v("addLineDrawed", Integer.toString(lineAdded.size()));
        }
        else {
            Log.v("draw line:::::", "nulllllllllllllllllll");
            return null;
        }
    }

    public V_NeuronSWC addBackgroundLineDrawed(ArrayList<Float> line, V_NeuronSWC_list [] v_neuronSWC_list) throws CloneNotSupportedException {
        if (img.getData() == null){
            return null;
        }
        ArrayList<Float> lineAdded;
        float [] lineCurrent = new float[line.size()];
        Log.v("addLineDrawed", Integer.toString(line.size()));
        for (int i = 0; i < line.size(); i++){
            lineCurrent[i] = line.get(i);
        }
//        lineAdded = getLineDrawed(lineCurrent);
        lineAdded = getLineDrawed_2(lineCurrent);

        if (lineAdded != null){
//            lineDrawed.add(lineAdded);
            int max_n = curSwcList.maxnoden();
            V_NeuronSWC seg = new  V_NeuronSWC();
            for(int i=0; i < lineAdded.size()/3; i++){
                V_NeuronSWC_unit u = new V_NeuronSWC_unit();
                u.n = max_n + i+ 1;
                if(i==0)
                    u.parent = -1;
                else
                    u.parent = max_n + i;
                float[] xyz = ModeltoVolume(new float[]{lineAdded.get(i*3+0),lineAdded.get(i*3+1),lineAdded.get(i*3+2)});
                u.x = xyz[0];
                u.y = xyz[1];
                u.z = xyz[2];
                u.type = lastLineType;
                seg.append(u);
//                System.out.println("u n p x y z: "+ u.n +" "+u.parent+" "+u.x +" "+u.y+ " "+u.z);
            }
            if(seg.row.size()<3){
                return null;
            }
            float[] headXYZ = new float[]{(float) seg.row.get(0).x, (float) seg.row.get(0).y, (float) seg.row.get(0).z};
            float[] tailXYZ = new float[]{(float) seg.row.get(seg.row.size()-1).x,
                    (float) seg.row.get(seg.row.size()-1).y,
                    (float) seg.row.get(seg.row.size()-1).z};
            boolean linked = false;
            for(int i=0; i<curSwcList.seg.size(); i++){
                V_NeuronSWC s = curSwcList.seg.get(i);
                for(int j=0; j<s.row.size(); j++){
                    if(linked)
                        break;
                    V_NeuronSWC_unit node = s.row.get(j);
                    float[] nodeXYZ = new float[]{(float) node.x, (float) node.y, (float) node.z};
                    if(distance(headXYZ,nodeXYZ)<5){
                        V_NeuronSWC_unit head = seg.row.get(0);
                        V_NeuronSWC_unit child = seg.row.get(1);
                        head.x = node.x;
                        head.y = node.y;
                        head.z = node.z;
                        head.n = node.n;
                        head.parent = node.parent;
                        child.parent = head.n;
                        linked = true;
                        break;
                    }
                    if(distance(tailXYZ,nodeXYZ)<5){
                        seg.reverse();
                        V_NeuronSWC_unit tail = seg.row.get(seg.row.size()-1);
                        V_NeuronSWC_unit child = seg.row.get(seg.row.size()-2);
                        tail.x = node.x;
                        tail.y = node.y;
                        tail.z = node.z;
                        tail.n = node.n;
                        tail.parent = node.parent;
                        child.n = tail.n;
                        linked = true;
                        break;
                    }
                }
            }

            MarkerList tempMarkerList = markerList.clone();
            V_NeuronSWC_list tempCurveList = curSwcList.clone();

            if (curUndo < UNDO_LIMIT){
                curUndo += 1;
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            } else {
                undoMarkerList.remove(0);
                undoCurveList.remove(0);
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            }

//            c[0] = undoMarkerList.size();

//            markerList = (ArrayList<ImageMarker>)MarkerList.clone();
            v_neuronSWC_list[0] = tempCurveList;


            curSwcList.append(seg);

//            v_neuronSWC_list[0] = curSwcList.clone();

//            if (v_neuronSWC_list[0].seg.get(0).equals(curSwcList.seg.get(0))){
//                System.out.println("Equallllllllll");
//                System.out.println("Equallllllllll");
//                System.out.println("Equallllllllll");
//                System.out.println("Equallllllllll");
//            }
//            if (process.size() < UNDO_LIMIT){
//                process.add(Operate.DRAWCURVE);
//                undoDrawList.add(seg);
//            } else{
//                Operate first = process.firstElement();
//                process.remove(0);
//                process.add(Operate.DRAWCURVE);
//                removeFirstUndo(first);
//                undoDrawList.add(seg);
//            }
            return seg;
//            return curSwcList.nsegs() - 1;

//            Log.v("addLineDrawed", Integer.toString(lineAdded.size()));
        }
        else {
            Log.v("draw line:::::", "nulllllllllllllllllll");
            return null;
        }
    }

    public boolean deleteFromNew(int segid){

        if (newSwcList.nsegs() < segid || segid < 0)
            return false;
        newSwcList.deleteSeg(segid);
        return true;
    }

    public boolean deleteFromCur(V_NeuronSWC seg, V_NeuronSWC_list v_neuronSWC_list){
        System.out.println("nnnnnn");
//        int index = undoDrawList.indexOf(seg);
//        if (index != -1){
//            undoDrawList.remove(seg);
//            process.remove(process.size() - 1);
//        }

//        if (curUndo > 0 && c >= 0 && c < curUndo){
//            curUndo -= 1;
//            undoCurveList.remove(c);
//            undoMarkerList.remove(c);
//        }

        if (curUndo > 0){
            int i = undoCurveList.lastIndexOf(v_neuronSWC_list);
            System.out.println("delete:::::");
            System.out.println(i);
            undoCurveList.remove(i);
            undoMarkerList.remove(i);
            curUndo -= 1;
        }

        return curSwcList.seg.remove(seg);

    }



    public  void deleteLine1(ArrayList<Float> line) throws CloneNotSupportedException {
//        curSwcList.deleteCurve(line, finalMatrix, sz, mz);
        System.out.println("deleteline1--------------------------");
        Vector<Integer> indexToBeDeleted = new Vector<>();
        for (int i = 0; i < line.size() / 3 - 1; i++){
            float x1 = line.get(i * 3);
            float y1 = line.get(i * 3 + 1);
            float x2 = line.get(i * 3 + 3);
            float y2 = line.get(i * 3 + 4);
            for(int j=0; j<curSwcList.nsegs(); j++){
                System.out.println("delete curswclist --"+j);
                V_NeuronSWC seg = curSwcList.seg.get(j);
                if(seg.to_be_deleted)
                    continue;
                Map<Integer, V_NeuronSWC_unit> swcUnitMap = new HashMap<Integer, V_NeuronSWC_unit>();
                for(int k=0; k<seg.row.size(); k++){
                    if(seg.row.get(k).parent != -1 && seg.getIndexofParent(k) != -1){
                        V_NeuronSWC_unit parent = seg.row.get(seg.getIndexofParent(k));
                        swcUnitMap.put(k,parent);
                    }
                }
                System.out.println("delete: end map");
                for(int k=0; k<seg.row.size(); k++){
                    System.out.println("j: "+j+" k: "+k);
                    V_NeuronSWC_unit child = seg.row.get(k);
                    int parentid = (int) child.parent;
                    if (parentid == -1 || seg.getIndexofParent(k) == -1){
                        System.out.println("parent -1");
                        continue;
                    }
                    V_NeuronSWC_unit parent = swcUnitMap.get(k);
                    float[] pchild = {(float) child.x, (float) child.y, (float) child.z};
                    float[] pparent = {(float) parent.x, (float) parent.y, (float) parent.z};
                    float[] pchildm = VolumetoModel(pchild);
                    float[] pparentm = VolumetoModel(pparent);
                    float[] p2 = {pchildm[0],pchildm[1],pchildm[2],1.0f};
                    float[] p1 = {pparentm[0],pparentm[1],pparentm[2],1.0f};

                    float [] p1Volumne = new float[4];
                    float [] p2Volumne = new float[4];
                    Matrix.multiplyMV(p1Volumne, 0, finalMatrix, 0, p1, 0);
                    Matrix.multiplyMV(p2Volumne, 0, finalMatrix, 0, p2, 0);
                    devideByw(p1Volumne);
                    devideByw(p2Volumne);
                    float x3 = p1Volumne[0];
                    float y3 = p1Volumne[1];
                    float x4 = p2Volumne[0];
                    float y4 = p2Volumne[1];

                    double m=(x2-x1)*(y3-y1)-(x3-x1)*(y2-y1);
                    double n=(x2-x1)*(y4-y1)-(x4-x1)*(y2-y1);
                    double p=(x4-x3)*(y1-y3)-(x1-x3)*(y4-y3);
                    double q=(x4-x3)*(y2-y3)-(x2-x3)*(y4-y3);

                    if( (Math.max(x1, x2) >= Math.min(x3, x4))
                            && (Math.max(x3, x4) >= Math.min(x1, x2))
                            && (Math.max(y1, y2) >= Math.min(y3, y4))
                            && (Math.max(y3, y4) >= Math.min(y1, y2))
                            && ((m * n) <= 0) && (p * q <= 0)){
                        System.out.println("------------------this is delete---------------");
                        seg.to_be_deleted = true;
                        indexToBeDeleted.add(j);
                        break;
                    }
                }
            }
        }
//        curSwcList.deleteMutiSeg(new Vector<Integer>());

        Vector<V_NeuronSWC> toBeDeleted = new Vector<>();
        for (int i = 0; i < indexToBeDeleted.size(); i++){
            int index = indexToBeDeleted.get(i);
            toBeDeleted.add(curSwcList.seg.get(index));
        }

        MarkerList tempMarkerList = markerList.clone();
        V_NeuronSWC_list tempCurveList = curSwcList.clone();

        if (curUndo < UNDO_LIMIT){
            curUndo += 1;
            undoMarkerList.add(tempMarkerList);
            undoCurveList.add(tempCurveList);
        } else {
            undoMarkerList.remove(0);
            undoCurveList.remove(0);
            undoMarkerList.add(tempMarkerList);
            undoCurveList.add(tempCurveList);
        }

        curSwcList.deleteMutiSeg(indexToBeDeleted);

//        if (process.size() < UNDO_LIMIT){
//            process.add(Operate.DELETECURVE);
//            undoDeleteList.add(toBeDeleted);
//        } else{
//            Operate first = process.firstElement();
//            process.remove(0);
//            process.add(Operate.DELETECURVE);
//            removeFirstUndo(first);
//            undoDeleteList.add(toBeDeleted);
//        }
    }

    public void splitCurve(ArrayList<Float> line) throws CloneNotSupportedException {
//        curSwcList.splitCurve(line, finalMatrix, sz, mz);
        System.out.println("split1--------------------------");
        boolean found = false;
        Vector<Integer> toSplit = new Vector<Integer>();
        for (int i = 0; i < line.size() / 3 - 1; i++){
            if (found == true){
                break;
            }
            float x1 = line.get(i * 3);
            float y1 = line.get(i * 3 + 1);
            float x2 = line.get(i * 3 + 3);
            float y2 = line.get(i * 3 + 4);
            for(int j=0; j<curSwcList.nsegs(); j++){
                if (found == true){
                    break;
                }
                System.out.println("delete curswclist --"+j);
                V_NeuronSWC seg = curSwcList.seg.get(j);
                if(seg.to_be_deleted)
                    continue;
                Map<Integer, V_NeuronSWC_unit> swcUnitMap = new HashMap<Integer, V_NeuronSWC_unit>();
                for(int k=0; k<seg.row.size(); k++){
                    if(seg.row.get(k).parent != -1 && seg.getIndexofParent(k) != -1){
                        V_NeuronSWC_unit parent = seg.row.get(seg.getIndexofParent(k));
                        swcUnitMap.put(k,parent);
                    }
                }
                System.out.println("delete: end map");
                for(int k=0; k<seg.row.size(); k++){
                    System.out.println("j: "+j+" k: "+k);
                    V_NeuronSWC_unit child = seg.row.get(k);
                    int parentid = (int) child.parent;
                    if (parentid == -1 || seg.getIndexofParent(k) == -1){
                        System.out.println("parent -1");
                        continue;
                    }
                    V_NeuronSWC_unit parent = swcUnitMap.get(k);
                    float[] pchild = {(float) child.x, (float) child.y, (float) child.z};
                    float[] pparent = {(float) parent.x, (float) parent.y, (float) parent.z};
                    float[] pchildm = VolumetoModel(pchild);
                    float[] pparentm = VolumetoModel(pparent);
                    float[] p2 = {pchildm[0],pchildm[1],pchildm[2],1.0f};
                    float[] p1 = {pparentm[0],pparentm[1],pparentm[2],1.0f};

                    float [] p1Volumne = new float[4];
                    float [] p2Volumne = new float[4];
                    Matrix.multiplyMV(p1Volumne, 0, finalMatrix, 0, p1, 0);
                    Matrix.multiplyMV(p2Volumne, 0, finalMatrix, 0, p2, 0);
                    devideByw(p1Volumne);
                    devideByw(p2Volumne);
                    float x3 = p1Volumne[0];
                    float y3 = p1Volumne[1];
                    float x4 = p2Volumne[0];
                    float y4 = p2Volumne[1];

                    double m=(x2-x1)*(y3-y1)-(x3-x1)*(y2-y1);
                    double n=(x2-x1)*(y4-y1)-(x4-x1)*(y2-y1);
                    double p=(x4-x3)*(y1-y3)-(x1-x3)*(y4-y3);
                    double q=(x4-x3)*(y2-y3)-(x2-x3)*(y4-y3);

                    if( (Math.max(x1, x2) >= Math.min(x3, x4))
                            && (Math.max(x3, x4) >= Math.min(x1, x2))
                            && (Math.max(y1, y2) >= Math.min(y3, y4))
                            && (Math.max(y3, y4) >= Math.min(y1, y2))
                            && ((m * n) <= 0) && (p * q <= 0)){
                        System.out.println("------------------this is split---------------");
//                        seg.to_be_deleted = true;
//                        break;
                        found = true;
//                        V_NeuronSWC newSeg = new V_NeuronSWC();
//                        V_NeuronSWC_unit first = seg.row.get(k);
//                        try {
//                            V_NeuronSWC_unit firstClone = first.clone();
//                            newSeg.append(firstClone);
//                        }catch (Exception e){
//                            System.out.println(e.getMessage());
//                        }
                        int cur = k;
//                        toSplit.add(k);
                        while (seg.getIndexofParent(cur) != -1){
                            cur = seg.getIndexofParent(cur);
                            toSplit.add(cur);
//                            V_NeuronSWC_unit nsu = swcUnitMap.get(cur);
//                            try{
//                                V_NeuronSWC_unit nsuClone = nsu.clone();
//                                newSeg.append(nsuClone);
//                            }catch (Exception e){
//                                System.out.println(e.getMessage());
//                            }
//                            seg.row.remove(cur);

                        }
                        V_NeuronSWC newSeg1 = new V_NeuronSWC();
                        V_NeuronSWC newSeg2 = new V_NeuronSWC();
                        int newSegid = curSwcList.nsegs();
                        V_NeuronSWC_unit first = seg.row.get(k);
                        try {
                            V_NeuronSWC_unit firstClone = first.clone();
                            V_NeuronSWC_unit firstClone2 = first.clone();
                            newSeg1.append(firstClone);
                            firstClone.parent = -1;
                            newSeg2.append(firstClone2);
                        }catch (Exception e){
                            System.out.println(e.getMessage());
                        }
                        for (int w = 0; w < seg.row.size(); w++){
                            try {
                                V_NeuronSWC_unit temp = seg.row.get(w);
                                if (!toSplit.contains(w)) {
                                    newSeg2.append(temp);
                                }else if(toSplit.contains(w) && (w != k)){
                                    temp.seg_id = newSegid;
                                    newSeg1.append(temp);
                                }
                            }catch (Exception e){
                                System.out.println(e.getMessage());
                            }
                        }

                        MarkerList tempMarkerList = markerList.clone();
                        V_NeuronSWC_list tempCurveList = curSwcList.clone();

                        if (curUndo < UNDO_LIMIT){
                            curUndo += 1;
                            undoMarkerList.add(tempMarkerList);
                            undoCurveList.add(tempCurveList);
                        } else {
                            undoMarkerList.remove(0);
                            undoCurveList.remove(0);
                            undoMarkerList.add(tempMarkerList);
                            undoCurveList.add(tempCurveList);
                        }

                        curSwcList.deleteSeg(j);
                        curSwcList.append(newSeg1);
                        curSwcList.append(newSeg2);
//                        splitPoints.add(pchildm[0]);
//                        splitPoints.add(pchildm[1]);
//                        splitPoints.add(pchildm[2]);
//                        splitType = (int)child.type;
                        break;
                    }
                }
            }
        }
        curSwcList.deleteMutiSeg(new Vector<Integer>());
    }

    public void deleteLine(ArrayList<Float> line){
        for (int i = 0; i < line.size() / 3 - 1; i++){
            float x1 = line.get(i * 3);
            float y1 = line.get(i * 3 + 1);
            float x2 = line.get(i * 3 + 3);
            float y2 = line.get(i * 3 + 4);
            for (int j = 0; j < lineDrawed.size(); j++){
                ArrayList<Float> curLine = lineDrawed.get(j);
                for (int k = 0; k < curLine.size() / 3 - 1; k++){
                    float [] p1 = {curLine.get(k * 3), curLine.get(k * 3 + 1), curLine.get(k * 3 + 2), 1.0f};
                    float [] p2 = {curLine.get(k * 3 + 3), curLine.get(k * 3 + 4), curLine.get(k * 3 + 5), 1.0f};
                    float [] p1Volumne = new float[4];
                    float [] p2Volumne = new float[4];
                    Matrix.multiplyMV(p1Volumne, 0, finalMatrix, 0, p1, 0);
                    Matrix.multiplyMV(p2Volumne, 0, finalMatrix, 0, p2, 0);
                    devideByw(p1Volumne);
                    devideByw(p2Volumne);
                    float x3 = p1Volumne[0];
                    float y3 = p1Volumne[1];
                    float x4 = p2Volumne[0];
                    float y4 = p2Volumne[1];
                    float temp = 0.000001f;
//                    if( Math.abs((x2-x1)*(y4-y3)-(x4-x3)*(y2-y1))<temp ){
//                        if( (x1==x3)&&((y3-y1)*(y3-y2)<=temp||(y4-y1)*(y4-y2)<=temp) ){
//                            lineDrawed.remove(j);
//                            break;
//                        }
//                    }
//                    else{
                    double m=(x2-x1)*(y3-y1)-(x3-x1)*(y2-y1);
                    double n=(x2-x1)*(y4-y1)-(x4-x1)*(y2-y1);
                    double p=(x4-x3)*(y1-y3)-(x1-x3)*(y4-y3);
                    double q=(x4-x3)*(y2-y3)-(x2-x3)*(y4-y3);

                    if( (Math.max(x1, x2) >= Math.min(x3, x4))
                            && (Math.max(x3, x4) >= Math.min(x1, x2))
                            && (Math.max(y1, y2) >= Math.min(y3, y4))
                            && (Math.max(y3, y4) >= Math.min(y1, y2))
                            && ((m * n) <= 0) && (p * q <= 0)){
                        lineDrawed.remove(j);
                        break;
                    }
//                    }
                }
            }
        }
    }

    public void changeLineType(ArrayList<Float> line, int type) throws CloneNotSupportedException {
        System.out.println("changeLineType--------------------------");
        Vector<Integer> indexToChangeLineType = new Vector<>();
        Vector<Integer> ChangeLineType = new Vector<>();
        for (int i = 0; i < line.size() / 3 - 1; i++){
            float x1 = line.get(i * 3);
            float y1 = line.get(i * 3 + 1);
            float x2 = line.get(i * 3 + 3);
            float y2 = line.get(i * 3 + 4);
            for(int j=0; j<curSwcList.nsegs(); j++){
//                System.out.println("delete curswclist --"+j);
                V_NeuronSWC seg = curSwcList.seg.get(j);
                if(seg.to_be_deleted)
                    continue;
                Map<Integer, V_NeuronSWC_unit> swcUnitMap = new HashMap<Integer, V_NeuronSWC_unit>();
                for(int k=0; k<seg.row.size(); k++){
                    if(seg.row.get(k).parent != -1 && seg.getIndexofParent(k) != -1){
                        V_NeuronSWC_unit parent = seg.row.get(seg.getIndexofParent(k));
                        swcUnitMap.put(k,parent);
                    }
                }
                System.out.println("changeLine: end map");
                for(int k=0; k<seg.row.size(); k++){
                    System.out.println("j: "+j+" k: "+k);
                    V_NeuronSWC_unit child = seg.row.get(k);
                    int parentid = (int) child.parent;
                    if (parentid == -1 || seg.getIndexofParent(k) == -1){
                        System.out.println("parent -1");
                        continue;
                    }
                    V_NeuronSWC_unit parent = swcUnitMap.get(k);
                    float[] pchild = {(float) child.x, (float) child.y, (float) child.z};
                    float[] pparent = {(float) parent.x, (float) parent.y, (float) parent.z};
                    float[] pchildm = VolumetoModel(pchild);
                    float[] pparentm = VolumetoModel(pparent);
                    float[] p2 = {pchildm[0],pchildm[1],pchildm[2],1.0f};
                    float[] p1 = {pparentm[0],pparentm[1],pparentm[2],1.0f};

                    float [] p1Volumne = new float[4];
                    float [] p2Volumne = new float[4];
                    Matrix.multiplyMV(p1Volumne, 0, finalMatrix, 0, p1, 0);
                    Matrix.multiplyMV(p2Volumne, 0, finalMatrix, 0, p2, 0);
                    devideByw(p1Volumne);
                    devideByw(p2Volumne);
                    float x3 = p1Volumne[0];
                    float y3 = p1Volumne[1];
                    float x4 = p2Volumne[0];
                    float y4 = p2Volumne[1];

                    double m=(x2-x1)*(y3-y1)-(x3-x1)*(y2-y1);
                    double n=(x2-x1)*(y4-y1)-(x4-x1)*(y2-y1);
                    double p=(x4-x3)*(y1-y3)-(x1-x3)*(y4-y3);
                    double q=(x4-x3)*(y2-y3)-(x2-x3)*(y4-y3);

                    if( (Math.max(x1, x2) >= Math.min(x3, x4))
                            && (Math.max(x3, x4) >= Math.min(x1, x2))
                            && (Math.max(y1, y2) >= Math.min(y3, y4))
                            && (Math.max(y3, y4) >= Math.min(y1, y2))
                            && ((m * n) <= 0) && (p * q <= 0)){
                        System.out.println("------------------this is delete---------------");
                        seg.to_be_deleted = true;
                        indexToChangeLineType.add(j);
                        ChangeLineType.add((int) seg.row.get(0).type);
                        break;
                    }
                }
            }
        }

        MarkerList tempMarkerList = markerList.clone();
        V_NeuronSWC_list tempCurveList = curSwcList.clone();

        if (curUndo < UNDO_LIMIT){
            curUndo += 1;
            undoMarkerList.add(tempMarkerList);
            undoCurveList.add(tempCurveList);
        } else {
            undoMarkerList.remove(0);
            undoCurveList.remove(0);
            undoMarkerList.add(tempMarkerList);
            undoCurveList.add(tempCurveList);
        }

        for(V_NeuronSWC seg : this.curSwcList.seg ){
            if (seg.to_be_deleted){
                for(int i = 0; i<seg.row.size(); i++){
                    seg.row.get(i).type = type;
                }
                seg.to_be_deleted = false;
            }
        }

//        if (process.size() < UNDO_LIMIT){
//            process.add(Operate.CHANGELINETYPE);
//            undoLineType.add(ChangeLineType);
//            undoChangeLineTypeIndex.add(indexToChangeLineType);
//        } else{
//            if (process.get(0) == Operate.CHANGELINETYPE){
//                undoLineType.remove(0);
//                undoChangeLineTypeIndex.remove(0);
//            }
//            process.remove(0);
//            process.add(Operate.CHANGELINETYPE);
//            undoLineType.add(ChangeLineType);
//            undoChangeLineTypeIndex.add(indexToChangeLineType);
//        }
    }

    public void changeAllType() throws CloneNotSupportedException {
        System.out.println("changeAllType--------------------------");

        MarkerList tempMarkerList = markerList.clone();
        V_NeuronSWC_list tempCurveList = curSwcList.clone();

        if (curUndo < UNDO_LIMIT){
            curUndo += 1;
            undoMarkerList.add(tempMarkerList);
            undoCurveList.add(tempCurveList);
        } else {
            undoMarkerList.remove(0);
            undoCurveList.remove(0);
            undoMarkerList.add(tempMarkerList);
            undoCurveList.add(tempCurveList);
        }

        Vector<Integer> indexToChangeLineType = new Vector<>();
        Vector<Integer> ChangeLineType = new Vector<>();
        for(int i=0; i<curSwcList.seg.size(); i++){
            V_NeuronSWC seg = curSwcList.seg.get(i);
            indexToChangeLineType.add(i);
            ChangeLineType.add((int) seg.row.get(0).type);
            for(V_NeuronSWC_unit u:seg.row){
                u.type = lastLineType;
            }
        }

//        if (process.size() < UNDO_LIMIT){
//            process.add(Operate.CHANGELINETYPE);
//            undoLineType.add(ChangeLineType);
//            undoChangeLineTypeIndex.add(indexToChangeLineType);
//        } else{
//            if (process.get(0) == Operate.CHANGELINETYPE){
//                undoLineType.remove(0);
//                undoChangeLineTypeIndex.remove(0);
//            }
//            process.remove(0);
//            process.add(Operate.CHANGELINETYPE);
//            undoLineType.add(ChangeLineType);
//            undoChangeLineTypeIndex.add(indexToChangeLineType);
//        }
    }



    public void importEswc(ArrayList<ArrayList<Float>> eswc){
        for (int i = 0; i < eswc.size(); i++){
            ArrayList<Float> currentLine = eswc.get(i);
            int parent = currentLine.get(6).intValue();
            if (parent == -1){
                continue;
            }
            ArrayList<Float> parentLine = eswc.get(parent - 1);
            eswcDrawed.add((sz[0] - parentLine.get(2)) / sz[0] * mz[0]);
            eswcDrawed.add((sz[1] - parentLine.get(3)) / sz[1] * mz[1]);
            eswcDrawed.add((parentLine.get(4)) / sz[2] * mz[2]);
            eswcDrawed.add((sz[0] - currentLine.get(2)) / sz[0] * mz[0]);
            eswcDrawed.add((sz[1] - currentLine.get(3)) / sz[1] * mz[1]);
            eswcDrawed.add((currentLine.get(4)) / sz[2] * mz[2]);
        }
    }

    public void importSwc(ArrayList<ArrayList<Float>> swc){
        for (int i = 0; i < swc.size(); i++){
            ArrayList<Float> currentLine = swc.get(i);
            int parent = currentLine.get(6).intValue();
            if (parent == -1){
                continue;
            }
            ArrayList<Float> parentLine = swc.get(parent - 1);
            swcDrawed.add((sz[0] - parentLine.get(2)) / sz[0] * mz[0]);
            swcDrawed.add((sz[1] - parentLine.get(3)) / sz[1] * mz[1]);
            swcDrawed.add((parentLine.get(4)) / sz[2] * mz[2]);
            swcDrawed.add((sz[0] - currentLine.get(2)) / sz[0] * mz[0]);
            swcDrawed.add((sz[1] - currentLine.get(3)) / sz[1] * mz[1]);
            swcDrawed.add((currentLine.get(4)) / sz[2] * mz[2]);
        }
    }
    public void importNeuronTree(NeuronTree nt){

        if (ifLoadSWC){
            deleteAllTracing();
            ifLoadSWC = false;
        }

        System.out.println("----------------importNeuronTree----------------");
        try{
            System.out.println("nt size: "+nt.listNeuron.size());
            Vector<V_NeuronSWC> segs = nt.devideByBranch();
            for (int i = 0; i < segs.size(); i++){
                curSwcList.append(segs.get(i));
            }

            System.out.println("curSwcList.nsegs() : ");curSwcList.nsegs();

        }catch (Exception e){
            e.printStackTrace();
        }

        ifLoadSWC = true;

    }


    public ArrayList<ImageMarker> importApo(ArrayList<ArrayList<Float>> apo){

        ArrayList<ImageMarker> markerListLoaded = new ArrayList<>();

        try{
            for (int i = 0; i < apo.size(); i++){
                ArrayList<Float> currentLine = apo.get(i);

//            apoDrawed.add((sz[0] - currentLine.get(5)) / sz[0] * mz[0]);
//            apoDrawed.add((sz[1] - currentLine.get(6)) / sz[1] * mz[1]);
//            apoDrawed.add((currentLine.get(4)) / sz[2] * mz[2]);

                ImageMarker imageMarker_drawed = new ImageMarker(currentLine.get(5),
                        currentLine.get(6),
                        currentLine.get(4));

                int r = currentLine.get(15).intValue();
                int g = currentLine.get(16).intValue();
                int b = currentLine.get(17).intValue();

                if (r == 0 && g == 0 && b == 0){
                    imageMarker_drawed.type = 0;

                }else if (r == 255 && g == 255 && b == 255){
                    imageMarker_drawed.type = 1;

                }else if (r == 255 && g == 0 && b == 0){
                    imageMarker_drawed.type = 2;

                }else if (r == 0 && g == 0 && b == 255){
                    imageMarker_drawed.type = 3;

                }else if (r == 0 && g == 255 && b == 0){
                    imageMarker_drawed.type = 4;

                }else if (r == 255 && g == 0 && b == 255){
                    imageMarker_drawed.type = 5;

                }else if (r == 255 && g == 255 && b == 0){
                    imageMarker_drawed.type = 6;

                }

                System.out.println("ImageType: " + imageMarker_drawed.type);
                markerList.add(imageMarker_drawed);
                markerListLoaded.add(imageMarker_drawed);
            }

            System.out.println("Size of : markerListLoaded: " + markerListLoaded.size());

        }catch (Exception e){
            markerListLoaded.clear();
            e.printStackTrace();
        }

        return markerListLoaded;

    }


    public void importMarker(ArrayList<ArrayList<Integer>> marker_list){

        for (int i = 0; i < marker_list.size(); i++){
            ArrayList<Integer> currentLine = marker_list.get(i);

            ImageMarker imageMarker_drawed = new ImageMarker(
                    currentLine.get(0).floatValue(),
                    currentLine.get(1).floatValue(),
                    currentLine.get(2).floatValue());

            imageMarker_drawed.type = 3;

            System.out.println("ImageType: " + imageMarker_drawed.type);
            markerList.add(imageMarker_drawed);
        }

    }


    public String saveCurrentSwc(String dir) throws Exception{
        String error = "";
        NeuronTree nt = this.getNeuronTree();

        //  save the swc file even if current swc is empty

//        if(nt.listNeuron.size()>0){
            String filePath = dir + "/" + nt.name + ".swc";
            System.out.println("filepath: "+filePath);
            boolean ifExits = nt.writeSWC_file(filePath);
            if (ifExits)
                error = "This file already exits";
            return error;
//        }else {
//            return error = "Current swc is empty!";
//        }

    }

    public String oversaveCurrentSwc(String dir) throws Exception{
        String error = "";
        NeuronTree nt = this.getNeuronTree();
        if(nt.listNeuron.size()>0){
            System.out.println("nt size: " + nt.listNeuron.size());
            String filePath = dir + "/" + nt.name + ".swc";
            System.out.println("filepath: "+filePath);
            boolean ifSucceed = nt.overwriteSWC_file(filePath);
            if (!ifSucceed)
                error = "Overwrite failed!";
            return error;
        }else {
            return error = "Current swc is empty!";
        }

    }

    public String saveCurrentApo(String filepath) throws Exception{
        String error = "";
        if(markerList.size()>0){
            markerList.saveAsApo(filepath);
        }else {
            error =  "Current apo is empty!";
        }
        return error;

    }

    public void reNameCurrentSwc(String name){
        curSwcList.name = name;
    }

    public Image4DSimple getImg() {
        return img;
    }

    public MarkerList getMarkerList() {
        return markerList;

//        ArrayList<ImageMarker> Marker_volume_List = new ArrayList<ImageMarker>();
//
//        for (int i = 0; i < MarkerList.size(); i++){
//            ImageMarker marker_model = MarkerList.get(i);
//            float[] model = {marker_model.x, marker_model.y, marker_model.z};
//            float[] volume = ModeltoVolume(model);
//            ImageMarker marker_volume = new ImageMarker(volume[0], volume[1], volume[2]);
//            Marker_volume_List.add(marker_volume);
//        }
//        return Marker_volume_List;
    }


    public NeuronTree getNeuronTree(){
        try {

//            for (int i = 0; i < curSwcList.nsegs(); i++){
//                V_NeuronSWC seg = curSwcList.seg.get(i);
//
//                System.out.println("getNeuronTree: ");
//                System.out.println("seg " + i + ": ");
//                for (int j = 0; j < seg.row.size(); j++){
//                    V_NeuronSWC_unit unit = seg.row.get(j);
//                    System.out.println("node n: " + unit.n + ", parent: " + unit.parent);
//                }
//            }

            V_NeuronSWC_list list = curSwcList.clone();

            System.out.println(list);
            return list.mergeSameNode();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return null;
        }
    }


    enum FileType
    {
        V3draw,
        SWC,
        ESWC,
        APO,
        ANO,
        TIF,
        JPG,
        PNG,
        V3dPBD,
        NotSupport
    }

    public void setTakePic(boolean takePic, Context contexts) {
        isTakePic = takePic;
        context_myrenderer = contexts;
    }

    public String getmCapturePath() {
        return mCapturePath;
    }

    public void resetCapturePath() {
        mCapturePath = null;
    }

    public  void pencolorchange(int color){
        lastLineType=color;


    }

    public void markercolorchange(int color){
        lastMarkerType = color;
    }

    public boolean undo() {
        if (process.size() == 0) {
            System.out.println("process is empty\n");
            return false;
        }

        Operate toUndo = process.lastElement();
        if (toUndo == Operate.DELETECURVE){
            if (undoDeleteList.size() <= 0){
                System.out.println("undoDeleteList is empty\n");
                return false;
            }

            Vector<V_NeuronSWC> lastDeleted = undoDeleteList.lastElement();

            if (lastDeleted.size() == 0){
                System.out.println("lastDeleted is empty\n");
                return false;
            }

            for (int i = 0; i < lastDeleted.size(); i++){
                V_NeuronSWC temp = lastDeleted.get(i);
                curSwcList.append(temp);
            }

            undoDeleteList.remove(undoDeleteList.size() - 1);
            process.remove(process.size() - 1);
        } else if (toUndo == Operate.DRAWCURVE){
            if (undoDrawList.size() <= 0){
                System.out.println("undoDrawedList is empty\n");
                return false;
            }

            V_NeuronSWC lastDrawed = undoDrawList.lastElement();
            boolean removeSuccess = curSwcList.seg.remove(lastDrawed);
            if (!removeSuccess){
                System.out.println("remove failed\n");
                return false;
            }

            undoDrawList.remove(undoDrawList.size() - 1);
            process.remove(process.size() - 1);
        } else if (toUndo == Operate.DRAWMARKER){
            if (undoDrawMarkerList.size() <= 0){
                System.out.println("undoDrawMarkerList is empty");
                return false;
            }

            ImageMarker temp = undoDrawMarkerList.lastElement();
            boolean removeSuccess = markerList.remove(temp);
            if (!removeSuccess){
                System.out.println("remove marker failed");
                return false;
            }

            undoDrawMarkerList.remove(undoDrawMarkerList.size() - 1);
            process.remove(process.size() - 1);
        } else if (toUndo == Operate.DELETEMARKER){
            if (undoDeleteMarkerList.size() <= 0){
                System.out.println("undoDeleteMarkerList is empty");
                return false;
            }

            ImageMarker temp = undoDeleteMarkerList.lastElement();
            markerList.add(temp);

            undoDeleteMarkerList.remove(undoDeleteMarkerList.size() - 1);
            process.remove(process.size() - 1);
        }else if(toUndo == Operate.CHANGELINETYPE){
            if(undoChangeLineTypeIndex.isEmpty() || undoLineType.isEmpty()){
                System.out.println("undoChangeLineTypeIndex is empty");
                return false;
            }

            Vector<Integer> lastUndoChangeLineTypeIndex = undoChangeLineTypeIndex.lastElement();

            for(int i=0; i<lastUndoChangeLineTypeIndex.size(); i++){
                V_NeuronSWC s = curSwcList.seg.get(lastUndoChangeLineTypeIndex.get(i));
                for(V_NeuronSWC_unit u:s.row){
                    u.type = undoLineType.lastElement().get(i);
                }
            }
            undoChangeLineTypeIndex.remove(undoChangeLineTypeIndex.size()-1);
            undoLineType.remove(undoLineType.size()-1);
            process.remove(process.size()-1);
        }

        System.out.println("undo succeed");
        return true;
    }

    public boolean undo2(){
        if (curUndo == 0)
            return false;

        curUndo -= 1;
        System.out.println("undosize");
        System.out.println(undoMarkerList.size());
        System.out.println("lastsize");
        System.out.println(undoMarkerList.get(undoMarkerList.size() - 1).size());
        markerList = undoMarkerList.get(undoMarkerList.size() - 1);
        curSwcList = undoCurveList.get(undoCurveList.size() - 1);
        undoMarkerList.remove(undoMarkerList.size() - 1);
        undoCurveList.remove(undoCurveList.size() - 1);
        System.out.println("cursize");
        System.out.println(markerList.size());

        return true;
    }

    public int getLastLineType() {
        return lastLineType;
    }




    public void corner_detection() {

//        if (bitmap2D == null)
//            return;
//
//        Toast.makeText(getContext(), "Please load a 2d image first", Toast.LENGTH_SHORT).show();

        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        // PC端一定要有这句话，但是android端一定不能有这句话，否则报错

//        Mat src = new Mat();
//
//        Mat temp = new Mat();
//
//        Mat dst = new Mat();


//        final int maxCorners = 40, blockSize = 3; //blockSize表示窗口大小，越大那么里面的像素点越多，选取梯度和方向变化最大的像素点作为角点，这样总的角点数肯定变少，而且也可能错过一些角点

//        final double qualityLevel = 0.05, minDistance = 23.0, k = 0.04;

        //qualityLevel：检测到的角点的质量等级，角点特征值小于qualityLevel*最大特征值的点将被舍弃；
        //minDistance：两个角点间最小间距，以像素为单位；

//        final boolean useHarrisDetector = false;

//        MatOfPoint corners = new MatOfPoint();


        File file = new File(filepath);
        System.out.println(filepath);
        long length = 0;
        InputStream is1 = null;
        if (file.exists()) {
            try {
                length = file.length();
                is1 = new FileInputStream(file);
//                grayscale =  rr.run(length, is);


                Log.v("getIntensity_3d", filepath);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            Uri uri = Uri.parse(filepath);

            try {
                ParcelFileDescriptor parcelFileDescriptor =
                        getContext().getContentResolver().openFileDescriptor(uri, "r");

                is1 = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);

                length = (int) parcelFileDescriptor.getStatSize();


            } catch (Exception e) {
                Log.v("MyPattern", "Successfully load intensity");

                Log.v("MyPattern", "Some problems in the MyPattern when load intensity");
            }


        }


        BitmapFactory.Options options1 = new BitmapFactory.Options();
        //设置inJustDecodeBounds为true表示只获取大小，不生成Btimap
        options1.inJustDecodeBounds = true;
        //解析图片大小
        //InputStream stream = getContentResolver().openInputStream(uri);
        BitmapFactory.decodeStream(is1, null, options1);
        if (is1 != null)
            System.out.println("isnnnnnn");
        IOUtils.closeQuietly(is1); // 关闭流
        // is.close();
        int width = options1.outWidth;
        int height = options1.outHeight;
        int ratio = 0;
        //如果宽度大于高度，交换宽度和高度
        if (width > height) {
            int temp2 = width;
            width = height;
            height = temp2;
        }
        //计算取样比例
        int sampleRatio = 1;
        if (width < 500 || height < 500)
            sampleRatio = 1;
        else{
            int s1 = 2;
            int s2 = 2;
            while ((width / s1) > 500){
                s1 *= 2;
            }
            while ((height / s2) > 900){
                s2 *= 2;
            }
            sampleRatio = Math.max(s1, s2);
        }
        System.out.println(width);
        System.out.println(height);
        //定义图片解码选项
        BitmapFactory.Options options2 = new BitmapFactory.Options();
        options2.inSampleSize = sampleRatio;


        //读取图片，并将图片缩放到指定的目标大小
        // InputStream stream = getContentResolver().openInputStream(uri);
        File file2 = new File(filepath);
        long length2 = 0;
        InputStream is2 = null;
        if (file2.exists()) {
            try {
                length2 = file2.length();
                is2 = new FileInputStream(file2);
//                grayscale =  rr.run(length, is);


                Log.v("getIntensity_3d", filepath);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            Uri uri = Uri.parse(filepath);

            try {
                ParcelFileDescriptor parcelFileDescriptor =
                        getContext().getContentResolver().openFileDescriptor(uri, "r");

                is2 = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);

                length2 = (int) parcelFileDescriptor.getStatSize();

                Log.v("MyPattern", "Successfully load intensity");

            } catch (Exception e) {
                Log.v("MyPattern", "Some problems in the MyPattern when load intensity");
            }


        }


        Bitmap image = BitmapFactory.decodeStream(is2, null, options2);
        if (image == null) {
            System.out.println("nnnnnn");
        }

        image = rotateBitmapByDegree(image, degree);

//        System.out.println(image.getWidth());
//        System.out.println(image.getHeight());

        System.out.println("ssssss");
        System.out.println(options2.inSampleSize);
        IOUtils.closeQuietly(is2);
        //is.close();


        // Bitmap image = bitmap2D;//从bitmap中加载进来的图像有时候有四个通道，所以有时候需要多加一个转化
        // Bitmap image = BitmapFactory.decodeResource(this.getResources(),R.drawable.cube);

//        Utils.bitmapToMat(image, src);//把image转化为Mat

//        dst = src.clone();

//        Imgproc.cvtColor(src, temp, Imgproc.COLOR_BGR2GRAY);//这里由于使用的是Imgproc这个模块所有这里要这么写
//
//        Log.i("CV", "image type:" + (temp.type() == CvType.CV_8UC3));
//        Imgproc.goodFeaturesToTrack(temp, corners, maxCorners, qualityLevel, minDistance,
//
//                new Mat(), blockSize, useHarrisDetector, k);
//        Point[] pCorners = corners.toArray();

        Bitmap destImage;
//        Bitmap sourceImage = bitmap2D;
        Bitmap sourceImage = image;
        HarrisCornerDetector filter = new HarrisCornerDetector();
        destImage = filter.filter(sourceImage, null);

        int[] corner_x_y = filter.corner_xy;
        int[] corner_x = new int[corner_x_y.length/2];
        int[] corner_y = new int[corner_x_y.length/2];

        System.out.println("LLLLLLLLLLLLLL");
        System.out.println(corner_x_y.length);
        System.out.println("xyxyxyxyxy");
        for (int n=0;n<corner_x_y.length/2;n++)
        {
            corner_x[n]=corner_x_y[2*n+1];
            corner_y[n]=corner_x_y[2*n];
            System.out.println(corner_x[n]);
            System.out.println(corner_y[n]);
        }


//        System.out.println(pCorners.length);


//        int power = (int) (Math.log((double) sampleRatio) / Math.log(2));
//        int actual_ratio = (int) Math.pow(2, power);
//        if (actual_ratio>2){
//            actual_ratio+=2;
//        }

        int actual_ratio = options2.inSampleSize;

        System.out.println("aaaaaaaaa");
        System.out.println(actual_ratio);
        for (int i = 0; i < corner_x_y.length/2; i++) {

            if (corner_x[i]==0&&corner_y[i]==0);
            else{
                ImageMarker imageMarker_drawed = new ImageMarker((float) corner_x[i] * actual_ratio,
                        (float) corner_y[i] * actual_ratio,
                        sz[2] / 2);
                imageMarker_drawed.type = lastMarkerType;
//            System.out.println("set type to 3");

                markerList.add(imageMarker_drawed);}
//            Imgproc.circle(dst, pCorners[i], (width+height)/(350*sampleRatio), new Scalar(255,255,0),2);

        }
//        System.out.println(pCorners.length);

        // Imgproc.cvtColor(temp,dst,Imgproc.COLOR_GRAY2BGR);

//        Utils.matToBitmap(dst,image);//把mat转化为bitmap
//        bitmap2D = image;

//        System.out.println(image.getWidth());
//        System.out.println(mz[0]);
//        myPattern2D = new MyPattern2D(bitmap2D, image.getWidth(), image.getHeight(), mz);

        //ImageView imageView = findViewById(R.id.text_view);

        //imageView.setImageBitmap(image);

        //release

//        src.release();

//        temp.release();

//        dst.release();

    }

    /*
    judge if there are something wrong in init img, avoid Splash screen
     */
    public boolean judgeImg(){

        if (myPattern == null || myPattern2D == null){
            if (ifFileSupport){
                if (fileType == FileType.V3draw || fileType == FileType.TIF || fileType == FileType.V3dPBD) {

                    //init MyPattern
                    if (screen_w<0 || screen_h<0 ||img == null || mz == null)
                        return false;
                }
                if (fileType == FileType.PNG || fileType == FileType.JPG){

                    //init MyPattern2D
                    if (bitmap2D == null || sz[0]<0 || sz[1]<0 || mz == null)
                        return false;
                }

                if (fileType == FileType.TIF || fileType == FileType.V3draw || fileType == FileType.V3dPBD ||
                        fileType == FileType.SWC || fileType == FileType.APO || fileType == FileType.ANO){

                    //init MyAxis
                    if (mz == null)
                        return false;
                }
            }
        }

        return true;
    }

    public void SetSwcLoaded(){
        ifLoadSWC = true;
    }

    public FileType getFileType() {
        return fileType;

    }

    public boolean getIfDownSampling(){
        return ifDownSampling;
    }

    public void setIfDownSampling(boolean b){
        if (ifNeedDownSample)
            ifDownSampling = b;
        else
            ifDownSampling = false;
    }

    public boolean getIfNeedDownSample(){
        return ifNeedDownSample;
    }

    public void setIfNeedDownSample(boolean b){
        ifNeedDownSample = b;
    }

    public boolean getIfFileSupport(){
        return ifFileSupport;
    }

    public boolean getIfFileLoaded(){
        return ifFileLoaded;
    }

    public boolean ifImageLoaded(){
        return !((img == null || !img.valid()) && bitmap2D == null);
    }

    public boolean if3dImageLoaded(){
        return !(img == null || !img.valid());
    }

    public boolean if2dImageLoaded() {
        return !(bitmap2D == null);
    }

    public void setIfShowSWC(boolean b){
        ifShowSWC = b;
    }

    public boolean getIfShowSWC(){
        return ifShowSWC;
    }

    public void saveUndo() throws CloneNotSupportedException {

        MarkerList tempMarkerList = markerList.clone();
        V_NeuronSWC_list tempCurveList = curSwcList.clone();

        if (curUndo < UNDO_LIMIT){
            curUndo += 1;
            undoMarkerList.add(tempMarkerList);
            undoCurveList.add(tempCurveList);
        } else {
            undoMarkerList.remove(0);
            undoCurveList.remove(0);
            undoMarkerList.add(tempMarkerList);
            undoCurveList.add(tempCurveList);
        }
    }

    public int[] getBinaryImg(){
        int [] newpixel = new int[1];
        return newpixel;
    }

    public byte[] binarization(byte[] data_image){
        //        int x=0;
//        for (int i=0;i<data_image.length;i++){
//            if (data_image[i] >100  & x<30){
//                Log.d("iiiii",i+ " " + String.valueOf((data_image[i])));
//            x++;}
//        }

        int[] data_gray = new int[(data_image.length + 1)/4];
//        float rate = 0.8f;
        int j = 0;
        for (int i = 0; i < data_image.length; i += 4){
            data_gray[j] = byteTranslate.byte1ToInt(data_image[i]);//(int)((float)data_image[i] * 0.3 + (float)data_image[i+1] * 0.59 + (float)data_image[i+2] * 0.11);
            j++;
        }
////        for ( int i =0; i < data_gray.length;i++){
////            if (data_gray[i] < 0)
////                Log.d("GRAYYY",String.valueOf(data_gray[i]));
////        }
//
////        int[] data_gray_copy = data_gray.clone();
//        float[] data_gray_average_cross = new float[data_gray.length];
//        float[] data_gray_average_rectangle = new float[data_gray.length];
////        int vol_hh = (int) Math.pow(((float)(vol_h + vol_w + vol_d))*vol_d/vol_h,1.0/3);
////        int vol_ww = (int)((float)vol_hh * vol_h / vol_w);
////        int vol_dd = (int)((float)vol_hh * vol_w / vol_d);
//        int vol_hh = 2;
//        int vol_ww = 2;
//        int vol_dd = 2;
////        Log.d("hh-ww-dd",String.valueOf(vol_hh)+"-"+String.valueOf(vol_ww)+"-"+String.valueOf(vol_dd));
//        int OOO = 0;
//        int OOO_i = 0;
//        int count_count = 0;
//
//        for (int h = 0; h < vol_h; h++)
//            for (int w = 0; w < vol_w; w++)
//                for (int d = 0; d < vol_d; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    int index_h1 = Math.max((h-2) * vol_w + w + d*vol_w*vol_h,0);
//                    int index_h2 = Math.max((h-1) * vol_w + w + d*vol_w*vol_h,0);
//                    int index_h3 = Math.min((h+1) * vol_w + w + d*vol_w*vol_h,vol_h);
//                    int index_h4 = Math.min((h+2) * vol_w + w + d*vol_w*vol_h,vol_h);
//                    int index_w1 = Math.max(h * vol_w + (w-2) + d*vol_w*vol_h,0);
//                    int index_w2 = Math.max(h * vol_w + (w-1) + d*vol_w*vol_h,0);
//                    int index_w3 = Math.min(h * vol_w + (w+1) + d*vol_w*vol_h,vol_w);
//                    int index_w4 = Math.min(h * vol_w + (w+2) + d*vol_w*vol_h,vol_w);
//                    int index_d1 = Math.max(h * vol_w + w + (d-2)*vol_w*vol_h,0);
//                    int index_d2 = Math.max(h * vol_w + w + (d-1)*vol_w*vol_h,0);
//                    int index_d3 = Math.min(h * vol_w + w + (d+1)*vol_w*vol_h,vol_d);
//                    int index_d4 = Math.min(h * vol_w + w + (d+2)*vol_w*vol_h,vol_d);
//
//                    int index_h5 = Math.max((h-4) * vol_w + w + d*vol_w*vol_h,0);
//                    int index_h6 = Math.max((h-3) * vol_w + w + d*vol_w*vol_h,0);
//                    int index_h7 = Math.min((h+3) * vol_w + w + d*vol_w*vol_h,vol_h);
//                    int index_h8 = Math.min((h+4) * vol_w + w + d*vol_w*vol_h,vol_h);
//                    int index_w5 = Math.max(h * vol_w + (w-4) + d*vol_w*vol_h,0);
//                    int index_w6 = Math.max(h * vol_w + (w-3) + d*vol_w*vol_h,0);
//                    int index_w7 = Math.min(h * vol_w + (w+3) + d*vol_w*vol_h,vol_w);
//                    int index_w8 = Math.min(h * vol_w + (w+4) + d*vol_w*vol_h,vol_w);
//                    int index_d5 = Math.max(h * vol_w + w + (d-4)*vol_w*vol_h,0);
//                    int index_d6 = Math.max(h * vol_w + w + (d-3)*vol_w*vol_h,0);
//                    int index_d7 = Math.min(h * vol_w + w + (d+3)*vol_w*vol_h,vol_d);
//                    int index_d8 = Math.min(h * vol_w + w + (d+4)*vol_w*vol_h,vol_d);
//
//                    int index_h9 = Math.max((h-6) * vol_w + w + d*vol_w*vol_h,0);
//                    int index_h10 = Math.max((h-5) * vol_w + w + d*vol_w*vol_h,0);
//                    int index_h11 = Math.min((h+5) * vol_w + w + d*vol_w*vol_h,vol_h);
//                    int index_h12 = Math.min((h+6) * vol_w + w + d*vol_w*vol_h,vol_h);
//                    int index_w9 = Math.max(h * vol_w + (w-6) + d*vol_w*vol_h,0);
//                    int index_w10 = Math.max(h * vol_w + (w-5) + d*vol_w*vol_h,0);
//                    int index_w11 = Math.min(h * vol_w + (w+5) + d*vol_w*vol_h,vol_w);
//                    int index_w12 = Math.min(h * vol_w + (w+6) + d*vol_w*vol_h,vol_w);
//                    int index_d9 = Math.max(h * vol_w + w + (d-6)*vol_w*vol_h,0);
//                    int index_d10 = Math.max(h * vol_w + w + (d-5)*vol_w*vol_h,0);
//                    int index_d11 = Math.min(h * vol_w + w + (d+5)*vol_w*vol_h,vol_d);
//                    int index_d12 = Math.min(h * vol_w + w + (d+6)*vol_w*vol_h,vol_d);
//
//
//
//                    data_gray_average_cross[index] = (data_gray[index]+data_gray[index_h1]+data_gray[index_h2]+data_gray[index_h3]+data_gray[index_h4]+data_gray[index_w1]+data_gray[index_w2]
//                            +data_gray[index_w3]+data_gray[index_w4]+data_gray[index_d1]+data_gray[index_d2]+data_gray[index_d3]+data_gray[index_d4]
//                            +data_gray[index_h5]+data_gray[index_h6]+data_gray[index_h7]+data_gray[index_h8]+data_gray[index_w5]+data_gray[index_w6]
//                            +data_gray[index_w7]+data_gray[index_w8]+data_gray[index_d5]+data_gray[index_d6]+data_gray[index_d7]+data_gray[index_d8]
//                            +data_gray[index_h9]+data_gray[index_h10]+data_gray[index_h11]+data_gray[index_h12]+data_gray[index_w9]+data_gray[index_w10]
//                            +data_gray[index_w11]+data_gray[index_w12]+data_gray[index_d9]+data_gray[index_d10]+data_gray[index_d11]+data_gray[index_d12]
//                            )/37;
//                }
//
//
//        for (int h = 0; h < vol_h; h++)
//            for (int w = 0; w < vol_w; w++)
//                for (int d = 0; d < vol_d; d++){
////                    int h1 = Math.max(1,h-vol_hh/2);
////                    int h2 = Math.min(vol_h,h+vol_hh/2);
////                    int w1 = Math.max(1,h-vol_ww/2);
////                    int w2 = Math.min(vol_w,h+vol_ww/2);
////                    int d1 = Math.max(1,h-vol_dd/2);
////                    int d2 = Math.min(vol_d,h+vol_hh/2);
//                    int h1 = h-vol_hh/2;
//                    int h2 = h+vol_hh/2+1;
//                    int w1 = w-vol_ww/2;
//                    int w2 = w+vol_ww/2+1;
//                    int d1 = d-vol_dd/2;
//                    int d2 = d+vol_hh/2+1;
//                      if (h1<0)
//                          h1 = 0;
//                      if (w1<0)
//                          w1 = 0;
//                      if (d1<0)
//                          d1=0;
//                      if(h2>vol_h)
//                          h2 = vol_h;
//                      if(w2>vol_w)
//                          w2 = vol_w;
//                      if(d2>vol_d)
//                          d2 = vol_d;
//
//
//                    float avg = 0;
//                    int count = 0;
////                    int[] mini_gray = new int[(h2-h1)*(w2-w1)*(d2-d1)];
//                    for (int ii = h1; ii < h2; ii++)
//                        for (int jj = w1; jj < w2; jj++)
//                            for ( int kk = d1; kk < d2; kk++){
//                                int central = (h1+h2)/2*vol_w + (w1+w2)/2 + (d1+d2)/2*vol_w*vol_h;
//                                int index = ii * vol_w + jj + kk*vol_w*vol_h;
////                                if (Math.abs(data_gray[central]-data_gray[index])<5){
//                                    avg += data_gray[index];
////                                    mini_gray[count] = data_gray[index];
//                                    count++;
////                                }
//
//                            }
//                    if (count_count<50){
//                        Log.d("COUNT",String.valueOf(count));
//                        count_count++;
//                    }
//
//                    if(avg == 0) OOO++;
////                    Log.d("ddddd",String.valueOf(d));
////                    Log.d("vol_dddd",String.valueOf(vol_dd));
//
//                    int index = h * vol_w + w + d*vol_w*vol_h;
////                    if (index == 2097151){
////                        Log.d("h-w-d",String.valueOf(h)+"-"+String.valueOf(w)+"-"+String.valueOf(d));
////                    }
////                    data_gray_average[index] =  (avg / ((h2-h1)*(w2-w1)*(d2-d1)));
//                    data_gray_average_rectangle[index] =  avg / count;
////                    data_gray_average_rectangle[index] =  threshold;
//                }
//
//        for (int i=0;i<data_image.length;i++)
//            if (data_image[i] == 0) OOO_i++;
//        Log.d("OOO",String.valueOf(OOO) + "-" + String.valueOf(OOO_i));
//        for (int i=0; i<20; i++){
//            Log.d("compare",i + "-" + String.valueOf(data_gray[i]) + "-" +String.valueOf(data_gray_average_rectangle[i]));
//        }
//        for (int i=0;i<data_gray.length;i++){
//            if ((data_gray[i]-data_gray_average_rectangle[i])>5 & data_gray[i]>50)
////              if (data_gray[i]<data_gray_average[i] & data_gray[i]>50)
//                Log.d("extreme",i + "-" + String.valueOf(data_gray[i]) + "-" +String.valueOf(data_gray_average_rectangle[i]));
//        }

// 下面是迭代法求二值化阈值
//                 求出最大灰度值和最小灰度值
        float Gmax=data_gray[0],Gmin=data_gray[0];
        for (int i=0;i<data_gray.length;i++){
            if (data_gray[i]>Gmax)
                Gmax = data_gray[i];
            if (data_gray[i]<Gmin)
                Gmin = data_gray[i];
        }
        Log.d("GGGmax",String.valueOf(Gmax));
        Log.d("GGGmin",String.valueOf(Gmin));
        //获取灰度直方图,其中histogram的下标表示灰度，下标对应的值表示有多少个像素对应的灰度这个灰度
//        int ii,jj,t,count1 = 0, count2 = 0, sum1 = 0, sum2 = 0;
//        int bp,fp;
        int[] histogram = new int[256];
        for (int t = (int) Gmin; t<=Gmax; t++){
            for (int index=0;index<data_gray.length;index++)
                if (data_gray[index] == t){
//                    Log.d("t",String.valueOf(t));
                    histogram[t]++;}
        }
        // 迭代法求最佳分割阈值
        int T = 0;
        int newT = (int) ((Gmax + Gmin) / 2); //初始的阈值
        // 求背景（黑色的）和前景（前面白色的神经元信号）的平均灰度值bp和fp
        while (T != newT){
            int sum1=0,sum2=0,count1=0,count2=0;
            int fp,bp;
            for (int ii = (int) Gmin; ii<newT; ii++){
                count1 += histogram[ii]; //背景像素点的个数
                sum1 += histogram[ii] * ii; //背景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
            }
            bp = (count1 == 0) ? 0: (sum1 / count1); //背景像素点的平均灰度值

            for (int jj = newT; jj<Gmax; jj++){
                count2 += histogram[jj]; //前景像素点的个数
                sum2 += histogram[jj] * jj; //前景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
            }
            fp = (count2 == 0) ? 0: (sum2 / count2); //前景像素点的平均灰度值
            T = newT;
            newT = (bp + fp) / 2;

        }
        int threshold = newT; //最佳阈值
        Log.d("threshold",String.valueOf(threshold));

//        for (int h = 0; h < vol_h; h++)
//            for (int w = 0; w < vol_w; w++)
//                for (int d = 0; d < vol_d; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    float average_rectangle = data_gray_average_rectangle[index];
//                    float average_cross = data_gray_average_cross[index];
////                    data_gray[index] = (data_gray[index] < ((average_rectangle*average_rectangle/(average_rectangle+average_cross)+average_cross*average_cross/(average_rectangle+average_cross))*rate)) ? 255:0;
////                    data_gray[index] = (data_gray[index] < (average_rectangle*rate)) ? 255:0;
//                    data_gray[index] = (data_gray[index] < average_rectangle) ? 255:0;
////                    data_gray[index] = ((average_rectangle*average_rectangle/(average_rectangle+average_cross) + average_cross*average_cross/(average_rectangle+average_cross)) < 42) ? 0:255;
////                    data_gray[index] = (average_rectangle <= threshold) ? 0:255;
//
//
////                    if (data_gray_average[index] < 50)
////                        data_gray[index] = 0;
////                    else
////                        data_gray[index] = 255;
//                }


//
//        float Gmax1=data_gray[vol_h/2*vol_w/2*vol_d/2],Gmin1=data_gray[vol_h/2*vol_w/2*vol_d/2];
//        for (int h = 0; h < vol_h/2; h++)
//            for (int w = 0; w < vol_w/2; w++)
//                for (int d = 0; d < vol_d/2; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > Gmax1)
//                        Gmax1 = data_gray[index];
//                    if (data_gray[index] < Gmin1)
//                        Gmin1 = data_gray[index];
//                }
//
//        int[] histogram1 = new int[256];
//        for (int t = (int) Gmin1; t<=Gmax1; t++){
//            for (int h = 0; h < vol_h/2; h++)
//                for (int w = 0; w < vol_w/2; w++)
//                    for (int d = 0; d < vol_d/2; d++){
//                        int index = h * vol_w + w + d*vol_w*vol_h;
//                        if (data_gray[index] == t){
//                            //                    Log.d("t",String.valueOf(t));
//                            histogram1[t]++;}
//                    }
//
//        }
//
//        int T1 = 0;
//        int newT1 = (int) ((Gmax1 + Gmin1) / 2); //初始的阈值
//        // 求背景（黑色的）和前景（前面白色的神经元信号）的平均灰度值bp和fp
//        while (T1 != newT1){
//            int sum1=0,sum2=0,count1=0,count2=0;
//            int fp,bp;
//            for (int ii = (int) Gmin1; ii<newT1; ii++){
//                count1 += histogram1[ii]; //背景像素点的个数
//                sum1 += histogram1[ii] * ii; //背景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            bp = (count1 == 0) ? 0: (sum1 / count1); //背景像素点的平均灰度值
//
//            for (int jj = newT1; jj<Gmax1; jj++){
//                count2 += histogram1[jj]; //前景像素点的个数
//                sum2 += histogram1[jj] * jj; //前景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            fp = (count2 == 0) ? 0: (sum2 / count2); //前景像素点的平均灰度值
//            T1 = newT1;
//            newT1 = (bp + fp) / 2;
//
//        }
//        int threshold1 = newT1; //最佳阈值
//        Log.d("threshold1",String.valueOf(threshold1));
//
//        for (int h = 0; h < vol_h/2; h++)
//            for (int w = 0; w < vol_w/2; w++)
//                for (int d = 0; d < vol_d/2; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > threshold1)
//                        data_gray[index] = 255;
//                    else
//                        data_gray[index] = 0;
//        }
//
//
//        float Gmax2=data_gray[vol_h/2*vol_w/2*vol_d/2],Gmin2=data_gray[vol_h/2*vol_w/2*vol_d/2];
//        for (int h = 0; h < vol_h/2; h++)
//            for (int w = 0; w < vol_w/2; w++)
//                for (int d = vol_d/2; d < vol_d; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > Gmax2)
//                        Gmax2 = data_gray[index];
//                    if (data_gray[index] < Gmin2)
//                        Gmin2 = data_gray[index];
//                }
//
//        int[] histogram2 = new int[256];
//        for (int t = (int) Gmin2; t<=Gmax2; t++){
//            for (int h = 0; h < vol_h/2; h++)
//                for (int w = 0; w < vol_w/2; w++)
//                    for (int d = vol_d/2; d < vol_d; d++){
//                        int index = h * vol_w + w + d*vol_w*vol_h;
//                        if (data_gray[index] == t){
//                            //                    Log.d("t",String.valueOf(t));
//                            histogram2[t]++;}
//                    }
//
//        }
//
//        int T2 = 0;
//        int newT2 = (int) ((Gmax2 + Gmin2) / 2); //初始的阈值
//        // 求背景（黑色的）和前景（前面白色的神经元信号）的平均灰度值bp和fp
//        while (T2 != newT2){
//            int sum1=0,sum2=0,count1=0,count2=0;
//            int fp,bp;
//            for (int ii = (int) Gmin2; ii<newT2; ii++){
//                count1 += histogram2[ii]; //背景像素点的个数
//                sum1 += histogram2[ii] * ii; //背景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            bp = (count1 == 0) ? 0: (sum1 / count1); //背景像素点的平均灰度值
//
//            for (int jj = newT2; jj<Gmax2; jj++){
//                count2 += histogram2[jj]; //前景像素点的个数
//                sum2 += histogram2[jj] * jj; //前景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            fp = (count2 == 0) ? 0: (sum2 / count2); //前景像素点的平均灰度值
//            T2 = newT2;
//            newT2 = (bp + fp) / 2;
//
//        }
//        int threshold2 = newT2; //最佳阈值
//        Log.d("threshold2",String.valueOf(threshold2));
//
//        for (int h = 0; h < vol_h/2; h++)
//            for (int w = 0; w < vol_w/2; w++)
//                for (int d = vol_d/2; d < vol_d; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > threshold2)
//                        data_gray[index] = 255;
//                    else
//                        data_gray[index] = 0;
//                }
//
//
//
//
//        float Gmax3=data_gray[vol_h/2*vol_w/2*vol_d/2],Gmin3=data_gray[vol_h/2*vol_w/2*vol_d/2];
//        for (int h = vol_h/2; h < vol_h; h++)
//            for (int w = 0; w < vol_w/2; w++)
//                for (int d = 0; d < vol_d/2; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > Gmax3)
//                        Gmax3 = data_gray[index];
//                    if (data_gray[index] < Gmin3)
//                        Gmin3 = data_gray[index];
//                }
//
//        int[] histogram3 = new int[256];
//        for (int t = (int) Gmin3; t<=Gmax3; t++){
//            for (int h = vol_h/2; h < vol_h; h++)
//                for (int w = 0; w < vol_w/2; w++)
//                    for (int d = 0; d < vol_d/2; d++){
//                        int index = h * vol_w + w + d*vol_w*vol_h;
//                        if (data_gray[index] == t){
//                            //                    Log.d("t",String.valueOf(t));
//                            histogram3[t]++;}
//                    }
//
//        }
//
//        int T3 = 0;
//        int newT3 = (int) ((Gmax3 + Gmin3) / 2); //初始的阈值
//        // 求背景（黑色的）和前景（前面白色的神经元信号）的平均灰度值bp和fp
//        while (T3 != newT3){
//            int sum1=0,sum2=0,count1=0,count2=0;
//            int fp,bp;
//            for (int ii = (int) Gmin3; ii<newT3; ii++){
//                count1 += histogram3[ii]; //背景像素点的个数
//                sum1 += histogram3[ii] * ii; //背景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            bp = (count1 == 0) ? 0: (sum1 / count1); //背景像素点的平均灰度值
//
//            for (int jj = newT3; jj<Gmax3; jj++){
//                count2 += histogram3[jj]; //前景像素点的个数
//                sum2 += histogram3[jj] * jj; //前景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            fp = (count2 == 0) ? 0: (sum2 / count2); //前景像素点的平均灰度值
//            T3 = newT3;
//            newT3 = (bp + fp) / 2;
//
//        }
//        int threshold3 = newT3; //最佳阈值
//        Log.d("threshold3",String.valueOf(threshold3));
//
//
//        for (int h = vol_h/2; h < vol_h; h++)
//            for (int w = 0; w < vol_w/2; w++)
//                for (int d = 0; d < vol_d/2; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > threshold3)
//                        data_gray[index] = 255;
//                    else
//                        data_gray[index] = 0;
//                }
//
//
//
//        float Gmax4=data_gray[vol_h/2*vol_w/2*vol_d/2],Gmin4=data_gray[vol_h/2*vol_w/2*vol_d/2];
//        for (int h = vol_h/2; h < vol_h; h++)
//            for (int w = 0; w < vol_w/2; w++)
//                for (int d = vol_d/2; d < vol_d; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > Gmax4)
//                        Gmax4 = data_gray[index];
//                    if (data_gray[index] < Gmin4)
//                        Gmin4 = data_gray[index];
//                }
//
//        int[] histogram4 = new int[256];
//        for (int t = (int) Gmin4; t<=Gmax4; t++){
//            for (int h = vol_h/2; h < vol_h; h++)
//                for (int w = 0; w < vol_w/2; w++)
//                    for (int d = vol_d/2; d < vol_d; d++){
//                        int index = h * vol_w + w + d*vol_w*vol_h;
//                        if (data_gray[index] == t){
//                            //                    Log.d("t",String.valueOf(t));
//                            histogram4[t]++;}
//                    }
//
//        }
//
//        int T4 = 0;
//        int newT4 = (int) ((Gmax4 + Gmin4) / 2); //初始的阈值
//        // 求背景（黑色的）和前景（前面白色的神经元信号）的平均灰度值bp和fp
//        while (T4 != newT4){
//            int sum1=0,sum2=0,count1=0,count2=0;
//            int fp,bp;
//            for (int ii = (int) Gmin4; ii<newT4; ii++){
//                count1 += histogram4[ii]; //背景像素点的个数
//                sum1 += histogram4[ii] * ii; //背景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            bp = (count1 == 0) ? 0: (sum1 / count1); //背景像素点的平均灰度值
//
//            for (int jj = newT4; jj<Gmax4; jj++){
//                count2 += histogram4[jj]; //前景像素点的个数
//                sum2 += histogram4[jj] * jj; //前景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            fp = (count2 == 0) ? 0: (sum2 / count2); //前景像素点的平均灰度值
//            T4 = newT4;
//            newT4 = (bp + fp) / 2;
//
//        }
//        int threshold4 = newT4; //最佳阈值
//        Log.d("threshold4",String.valueOf(threshold4));
//
//        for (int h = vol_h/2; h < vol_h; h++)
//            for (int w = 0; w < vol_w/2; w++)
//                for (int d = vol_d/2; d < vol_d; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > threshold4)
//                        data_gray[index] = 255;
//                    else
//                        data_gray[index] = 0;
//                }
//
//
//
//
//        float Gmax5=data_gray[vol_h/2*vol_w/2*vol_d/2],Gmin5=data_gray[vol_h/2*vol_w/2*vol_d/2];
//        for (int h = 0; h < vol_h/2; h++)
//            for (int w = vol_w/2; w < vol_w; w++)
//                for (int d = 0; d < vol_d/2; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > Gmax5)
//                        Gmax5 = data_gray[index];
//                    if (data_gray[index] < Gmin5)
//                        Gmin5 = data_gray[index];
//                }
//
//        int[] histogram5 = new int[256];
//        for (int t = (int) Gmin5; t<=Gmax5; t++){
//            for (int h = 0; h < vol_h/2; h++)
//                for (int w = vol_w/2; w < vol_w; w++)
//                    for (int d = 0; d < vol_d/2; d++){
//                        int index = h * vol_w + w + d*vol_w*vol_h;
//                        if (data_gray[index] == t){
//                            //                    Log.d("t",String.valueOf(t));
//                            histogram5[t]++;}
//                    }
//
//        }
//
//        int T5 = 0;
//        int newT5 = (int) ((Gmax5 + Gmin5) / 2); //初始的阈值
//        // 求背景（黑色的）和前景（前面白色的神经元信号）的平均灰度值bp和fp
//        while (T5 != newT5){
//            int sum1=0,sum2=0,count1=0,count2=0;
//            int fp,bp;
//            for (int ii = (int) Gmin5; ii<newT5; ii++){
//                count1 += histogram5[ii]; //背景像素点的个数
//                sum1 += histogram5[ii] * ii; //背景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            bp = (count1 == 0) ? 0: (sum1 / count1); //背景像素点的平均灰度值
//
//            for (int jj = newT5; jj<Gmax5; jj++){
//                count2 += histogram5[jj]; //前景像素点的个数
//                sum2 += histogram5[jj] * jj; //前景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            fp = (count2 == 0) ? 0: (sum2 / count2); //前景像素点的平均灰度值
//            T5 = newT5;
//            newT5 = (bp + fp) / 2;
//
//        }
//        int threshold5 = newT5; //最佳阈值
//        Log.d("threshold5",String.valueOf(threshold5));
//
//        for (int h = 0; h < vol_h/2; h++)
//            for (int w = vol_w/2; w < vol_w; w++)
//                for (int d = 0; d < vol_d/2; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > threshold5)
//                        data_gray[index] = 255;
//                    else
//                        data_gray[index] = 0;
//                }
//
//
//
//        float Gmax6=data_gray[vol_h/2*vol_w/2*vol_d/2],Gmin6=data_gray[vol_h/2*vol_w/2*vol_d/2];
//        for (int h = 0; h < vol_h/2; h++)
//            for (int w = vol_w/2; w < vol_w; w++)
//                for (int d = vol_d/2; d < vol_d; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > Gmax6)
//                        Gmax6 = data_gray[index];
//                    if (data_gray[index] < Gmin6)
//                        Gmin6 = data_gray[index];
//                }
//
//        int[] histogram6 = new int[256];
//        for (int t = (int) Gmin6; t<=Gmax6; t++){
//            for (int h = 0; h < vol_h/2; h++)
//                for (int w = vol_w/2; w < vol_w; w++)
//                    for (int d = vol_d/2; d < vol_d; d++){
//                        int index = h * vol_w + w + d*vol_w*vol_h;
//                        if (data_gray[index] == t){
//                            //                    Log.d("t",String.valueOf(t));
//                            histogram6[t]++;}
//                    }
//
//        }
//
//        int T6 = 0;
//        int newT6 = (int) ((Gmax6 + Gmin6) / 2); //初始的阈值
//        // 求背景（黑色的）和前景（前面白色的神经元信号）的平均灰度值bp和fp
//        while (T6 != newT6){
//            int sum1=0,sum2=0,count1=0,count2=0;
//            int fp,bp;
//            for (int ii = (int) Gmin6; ii<newT6; ii++){
//                count1 += histogram6[ii]; //背景像素点的个数
//                sum1 += histogram6[ii] * ii; //背景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            bp = (count1 == 0) ? 0: (sum1 / count1); //背景像素点的平均灰度值
//
//            for (int jj = newT6; jj<Gmax6; jj++){
//                count2 += histogram6[jj]; //前景像素点的个数
//                sum2 += histogram6[jj] * jj; //前景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            fp = (count2 == 0) ? 0: (sum2 / count2); //前景像素点的平均灰度值
//            T6 = newT6;
//            newT6 = (bp + fp) / 2;
//
//        }
//        int threshold6 = newT6; //最佳阈值
//        Log.d("threshold6",String.valueOf(threshold6));
//
//        for (int h = 0; h < vol_h/2; h++)
//            for (int w = vol_w/2; w < vol_w; w++)
//                for (int d = vol_d/2; d < vol_d; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > threshold6)
//                        data_gray[index] = 255;
//                    else
//                        data_gray[index] = 0;
//                }
//
//
//
//        float Gmax7=data_gray[vol_h/2*vol_w/2*vol_d/2],Gmin7=data_gray[vol_h/2*vol_w/2*vol_d/2];
//        for (int h = vol_h/2; h < vol_h; h++)
//            for (int w = vol_w/2; w < vol_w; w++)
//                for (int d = 0; d < vol_d/2; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > Gmax7)
//                        Gmax7 = data_gray[index];
//                    if (data_gray[index] < Gmin7)
//                        Gmin7 = data_gray[index];
//                }
//
//        int[] histogram7 = new int[256];
//        for (int t = (int) Gmin7; t<=Gmax7; t++){
//            for (int h = vol_h/2; h < vol_h; h++)
//                for (int w = vol_w/2; w < vol_w; w++)
//                    for (int d = 0; d < vol_d/2; d++){
//                        int index = h * vol_w + w + d*vol_w*vol_h;
//                        if (data_gray[index] == t){
//                            //                    Log.d("t",String.valueOf(t));
//                            histogram7[t]++;}
//                    }
//
//        }
//
//        int T7 = 0;
//        int newT7 = (int) ((Gmax7 + Gmin7) / 2); //初始的阈值
//        // 求背景（黑色的）和前景（前面白色的神经元信号）的平均灰度值bp和fp
//        while (T7 != newT7){
//            int sum1=0,sum2=0,count1=0,count2=0;
//            int fp,bp;
//            for (int ii = (int) Gmin7; ii<newT7; ii++){
//                count1 += histogram7[ii]; //背景像素点的个数
//                sum1 += histogram7[ii] * ii; //背景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            bp = (count1 == 0) ? 0: (sum1 / count1); //背景像素点的平均灰度值
//
//            for (int jj = newT7; jj<Gmax7; jj++){
//                count2 += histogram7[jj]; //前景像素点的个数
//                sum2 += histogram7[jj] * jj; //前景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            fp = (count2 == 0) ? 0: (sum2 / count2); //前景像素点的平均灰度值
//            T7 = newT7;
//            newT7 = (bp + fp) / 2;
//
//        }
//        int threshold7 = newT7; //最佳阈值
//        Log.d("threshold7",String.valueOf(threshold7));
//
//        for (int h = vol_h/2; h < vol_h; h++)
//            for (int w = vol_w/2; w < vol_w; w++)
//                for (int d = 0; d < vol_d/2; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > threshold7)
//                        data_gray[index] = 255;
//                    else
//                        data_gray[index] = 0;
//                }
//
//
//        float Gmax8=data_gray[vol_h/2*vol_w/2*vol_d/2],Gmin8=data_gray[vol_h/2*vol_w/2*vol_d/2];
//        for (int h = vol_h/2; h < vol_h; h++)
//            for (int w = vol_w/2; w < vol_w; w++)
//                for (int d = vol_d/2; d < vol_d; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > Gmax8)
//                        Gmax8 = data_gray[index];
//                    if (data_gray[index] < Gmin8)
//                        Gmin8 = data_gray[index];
//                }
//
//        int[] histogram8 = new int[256];
//        for (int t = (int) Gmin8; t<=Gmax8; t++){
//            for (int h = vol_h/2; h < vol_h; h++)
//                for (int w = vol_w/2; w < vol_w; w++)
//                    for (int d = vol_d/2; d < vol_d; d++){
//                        int index = h * vol_w + w + d*vol_w*vol_h;
//                        if (data_gray[index] == t){
//                            //                    Log.d("t",String.valueOf(t));
//                            histogram8[t]++;}
//                    }
//
//        }
//
//        int T8 = 0;
//        int newT8 = (int) ((Gmax8 + Gmin8) / 2); //初始的阈值
//        // 求背景（黑色的）和前景（前面白色的神经元信号）的平均灰度值bp和fp
//        while (T8 != newT8){
//            int sum1=0,sum2=0,count1=0,count2=0;
//            int fp,bp;
//            for (int ii = (int) Gmin8; ii<newT8; ii++){
//                count1 += histogram8[ii]; //背景像素点的个数
//                sum1 += histogram8[ii] * ii; //背景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            bp = (count1 == 0) ? 0: (sum1 / count1); //背景像素点的平均灰度值
//
//            for (int jj = newT8; jj<Gmax8; jj++){
//                count2 += histogram8[jj]; //前景像素点的个数
//                sum2 += histogram8[jj] * jj; //前景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            fp = (count2 == 0) ? 0: (sum2 / count2); //前景像素点的平均灰度值
//            T8 = newT8;
//            newT8 = (bp + fp) / 2;
//
//        }
//        int threshold8 = newT8; //最佳阈值
//        Log.d("threshold8",String.valueOf(threshold8));
//
//        for (int h = vol_h/2; h < vol_h; h++)
//            for (int w = vol_w/2; w < vol_w; w++)
//                for (int d = vol_d/2; d < vol_d; d++){
//                    int index = h * vol_w + w + d*vol_w*vol_h;
//                    if (data_gray[index] > threshold8)
//                        data_gray[index] = 255;
//                    else
//                        data_gray[index] = 0;
//                }

        if (threshold >= 35 & threshold <= 45 )
            threshold += 2;
        else if (threshold < 35) //防止threshold太小了。
            threshold = 35;
        Log.d("newthreshold",String.valueOf(threshold));
        for (int i=0;i<data_gray.length;i++){
            if (data_gray[i] > threshold)
                data_gray[i] = 255;
            else
                data_gray[i] = 0;
//            Log.d("newthreshold",String.valueOf(threshold));
        }
        for (int i = 0; i < data_image.length; i += 4){
            data_image[i] = (byte) data_gray[i/4];
            data_image[i+1] = (byte) data_gray[i/4];
            data_image[i+2] = (byte) data_gray[i/4];
        }


//        for (int i = 0; i < 20; i++)
//            Log.d("data_image_copy_gray",i + "-" +String.valueOf(data_image[i]) + "-" + String.valueOf(data_gray_copy[i]) + "-" + String.valueOf(data_gray[i]));

        return data_image;

    }

    public boolean driveMode(float [] vertexPoints, float [] direction, float [] head){
//        int size = vertexPoints.length;

        if (myPattern == null) {
            return false;
        }


        short [] drawlistTriangle = new short[]{
                0, 1, 2
        };

        short [] drawlistSquare = new short[]{
                0, 1, 2, 0, 2, 3
        };

        short [] drawlistHexagon = new short[]{
                0, 1, 2, 0, 2, 3,
                0, 3, 4, 0, 4, 5
        };

        short [] drawlistPentagon = new short[]{
                0, 1, 2, 0, 2, 3,
                0, 3, 4,
        };

        short [] drawlist6square = new short[] {
            0, 1, 2,      0, 2, 3,    // Front face
            4, 5, 6,      4, 6, 7,    // Back face
            8, 9, 10,     8, 10, 11,  // Top face
            12, 13, 14,   12, 14, 15, // Bottom face
            16, 17, 18,   16, 18, 19, // Right face
            20, 21, 22,   20, 22, 23  // Left face
        };

        short [] drawlist3square4triangle = new short[]{
                0, 1, 2,      0, 2, 3,    // Front face
                4, 5, 6,      4, 6, 7,    // Back face
                8, 9, 10,     8, 10, 11,  // Top face
                12, 13, 14,
                1, 12, 13,    2, 13, 14,    3, 12, 14
        };

        short [] drawlist6square1triangle = new short[]{
                0, 1, 2,      0, 2, 3,    // Front face
                4, 5, 6,      4, 6, 7,    // Back face
                8, 9, 10,     8, 10, 11,  // Top face
                12, 13, 14,   12, 14, 15, // Bottom face
                16, 17, 18,   16, 18, 19, // Right face
                20, 21, 22,   20, 22, 23,  // Left face
                24, 25, 26
        };

        float ratio = (float) screen_w / screen_h;

        if (screen_w > screen_h) {
            Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1, 1, 1, 100);
        } else {
            Matrix.orthoM(projectionMatrix, 0, -1, 1, -1 / ratio, 1 / ratio, 1, 100);
        }

        float [] rotate = new float[16];

        float [] aim = new float[]{
                0f, 0f, -1f
        };

        double angle = Math.acos(direction[2] / Math.sqrt(direction[0] * direction[0] + direction[1] * direction[1] + direction[2] * direction[2]));
        float [] axis = new float[]{
                direction[1], -direction[0], 0
        };

        if (axis[0] == 0 && axis[1] == 0){
            Matrix.setIdentityM(rotationMatrix,0);
        } else {
            float a = (float)(angle * 180 / Math.PI);
            Matrix.setRotateM(rotationMatrix, 0, a, axis[0], axis[1], axis[2]);
        }



//        Matrix.multiplyMM(rotationMatrix, 0, rotate, 0, rotationMatrix, 0);

        float [] headE = new float[]{head[0], head[1], head[2], 1};
        float [] headAfter = new float[4];
        Matrix.multiplyMV(headAfter, 0, rotationMatrix, 0, headE, 0);

        float [] rotation2Matrix = new float[16];
        if (headAfter[0] == 0){
            Matrix.setIdentityM(rotation2Matrix, 0);
        } else {
            double angle2 = Math.acos(headAfter[1] / Math.sqrt(headAfter[0] * headAfter[0] + headAfter[1] * headAfter[1] + headAfter[2] * headAfter[2]));
            float a2 = (float) (angle2 * 180 / Math.PI);


//            Matrix.setRotateM(rotation2Matrix, 0, a2, 0, 0, -headAfter[0]);
            Matrix.setRotateM(rotation2Matrix, 0, a2, -headAfter[2], 0, headAfter[0]);

            Matrix.multiplyMM(rotationMatrix, 0, rotation2Matrix, 0, rotationMatrix, 0);
        }

        Matrix.setIdentityM(zoomMatrix,0);
        Matrix.scaleM(zoomMatrix, 0, 4f, 4f, 4f);
        cur_scale = 4f;

//        byte[] color = img.getData();
//        byte[] gray = new byte[color.length];
//        for (int i=0;i<gray.length;i++){
////            int red = (gray[i] & 0x00FF0000) >> 16;
////            int green = (gray[i] & 0x0000FF00) >> 8;
////            int blue = gray[i] & 0x000000FF;
//            gray[i] = (byte) byteTranslate.byte1ToInt(color[i]);  //(byte)((int)((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11));
//        }
//        // 求出最大灰度值和最小灰度值
//        int Gmax=gray[0],Gmin=gray[0];
//        for (int i=0;i<gray.length;i++){
//            if (gray[i]>Gmax)
//                Gmax = gray[i];
//            if (gray[i]<Gmin)
//                Gmin = gray[i];
//        }
////        Log.d("GGGmax",String.valueOf(Gmax));
////        Log.d("GGGmin",String.valueOf(Gmin));
//        //获取灰度直方图,其中histogram的下标表示灰度，下标对应的值表示有多少个像素对应的灰度这个灰度
//        int i,j,t,count1 = 0, count2 = 0, sum1 = 0, sum2 = 0;
//        int bp,fp;
//        int[] histogram = new int[256];
//        for (t = Gmin;t<=Gmax;t++){
//            for (int index=0;index<gray.length;index++)
//                if (gray[index] == t){
////                    Log.d("t",String.valueOf(t));
//                    histogram[t]++;}
//        }
//        // 迭代法求最佳分割阈值
//        int T = 0;
//        int newT = (Gmax + Gmin) / 2; //初始的阈值
//        // 求背景（黑色的）和前景（前面白色的神经元信号）的平均灰度值bp和fp
//        while (T != newT){
//            for (i = 0; i<T; i++){
//                count1 += histogram[i]; //背景像素点的个数
//                sum1 += histogram[i] * i; //背景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            bp = (count1 == 0) ? 0: (sum1 / count1); //背景像素点的平均灰度值
//
//            for (j = i; j<histogram.length; j++){
//                count2 += histogram[j]; //前景像素点的个数
//                sum2 += histogram[j] * j; //前景像素的的灰度总值 i为灰度值，histogram[i]为对应的个数
//            }
//            fp = (count2 == 0) ? 0: (sum2 / count2); //前景像素点的平均灰度值
//            T = newT;
//            newT = (bp + fp) / 2;
//
//        }
//        threshold = newT; //最佳阈值
//        myPattern.deliver_threshold(0.1f);
//        Log.d("threshold",String.valueOf((float)threshold / 255));

        //二值化
//        for (int index =0; index<gray.length; index++){
//            if (gray[index] > 0)
//                gray[index] = (byte) 255;
//            else
//                gray[index] = (byte) 0;
//        }
//        img.resetData(gray);

//        byte[] gray = img.getData();
//        for (int i=0;i<gray.length;i++)
//            Log.d("gray",i + "-" + String.valueOf(gray[i]));


//        contrast = Float.parseFloat(getContrast(getContext()));
//        Log.d("contrast",String.valueOf(contrast));

//        resetContrast(30);

        Matrix.setIdentityM(translateMatrix,0);//建立单位矩阵

        if (!ifNavigationLococation){
            Matrix.translateM(translateMatrix,0,-0.5f * mz[0],-0.5f * mz[1],-0.5f * mz[2]);
        }else {
            Matrix.translateM(translateMatrix,0,-0.5f * mz_neuron[0],-0.5f * mz_neuron[1],-0.5f * mz_neuron[2]);
        }

//        float [] tm = new float[16];
        Matrix.setIdentityM(translateAfterMoveMatrix, 0);
        float [] gamePosition = gameCharacter.getPosition();
        float [] dis = new float[]{ gamePosition[0] - 0.5f, gamePosition[1] - 0.5f, gamePosition[2] - 0.5f };
        convertToPerspective(dis);
//        Matrix.translateM(translateAfterMoveMatrix, 0, 0.5f - gamePosition[0], 0.5f - gamePosition[1], 0.5f - gamePosition[2]);

//        Matrix.multiplyMM(translateAfterMatrix, 0, tm, 0, translateAfterMatrix, 0);

        int size = vertexPoints.length;
        if (size == 9){
            System.out.println("Triangle");
            myPattern.setDrawListBuffer(drawlistTriangle);
            myPattern.setDrawlistLength(drawlistTriangle.length);
        } else if (size == 12){
            System.out.println("Square");
            myPattern.setDrawListBuffer(drawlistSquare);
            myPattern.setDrawlistLength(drawlistSquare.length);
        } else if (size == 15){
            System.out.println("Pentagon");
            myPattern.setDrawListBuffer(drawlistPentagon);
            myPattern.setDrawlistLength(drawlistPentagon.length);
        } else if (size == 18){
            System.out.println("Hexagon");
            myPattern.setDrawListBuffer(drawlistHexagon);
            myPattern.setDrawlistLength(drawlistHexagon.length);
        } else {
            System.out.println("wrong vertex to draw");
            return false;
        }

        myPattern.setVertex(vertexPoints);

        return true;

    }

    public float[] head(float m, float n, float p){
        // (x0,y0,z0)是mark的坐标，（x,n,p）是mark前进方向的向量，Pix还是block的大小
        // 1.首先求防止头脚颠倒的向量。 先求一个与方向向量垂直，且与XOZ 平行的向量（则与XOZ法向量垂直（0,1,0））,然后可以直接利用向量积来求。
        // 然后防止头脚颠倒的向量是与前面两个向量(方向向量和与其垂直的向量)垂直的向量（也可以直接利用向量积就可以求出来）
        // 向量积：(a1,b1,c1)与(a2,b2,c2) 向量积 (b1c2-b2c1,a2c1-a1c2,a1b2-a2b1)
        // 方向向量(m,n,p)是 (a1,b1,c1)

        // float[] XOZ = {0,1,0}; // (a2,b2,c2)
//        float[] dir_ver = new float[3];

        float[] des = new float[3];
//        dir_ver[0] = p; //a2
//        dir_ver[1] = 0; //b2
//        dir_ver[2] = m; //c2

        des[0] = n*m;
        des[1] = p*p-m*m;
        des[2] = -p*n;

        return des;

    }
    public float[] thirdPersonAngle(float x0, float y0, float z0, float m, float n, float p, float h1, float h2, float h3){
        // (x0,y0,z0)是mark的坐标，（m,n,p）是mark前进方向的向量,(h1,h2,h3)是防止头脚颠倒的向量
        // 1.首先求防止头脚颠倒的向量。 先求一个与方向向量垂直，且与XOZ 平行的向量（则与XOZ法向量垂直（0,1,0））,然后可以直接利用向量积来求。
        // 然后防止头脚颠倒的向量是与前面两个向量(方向向量和与其垂直的向量)垂直的向量（也可以直接利用向量积就可以求出来）
        // 向量积：(a1,b1,c1)与(a2,b2,c2) 向量积 (b1c2-b2c1,a2c1-a1c2,a1b2-a2b1)
        // 方向向量(m,n,p)是 (a1,b1,c1)
        // 2.视角所在的点与前面的方向向量和求出来的防止颠倒的向量在同一平面，视角看进去的法向量应该也是在这个平面里的，可以先求法向量好求一点。
        // 如何求这个平面呢：方向向量和求出来的防止颠倒的向量做向量积得到平面的法向量，然后利用平面的点法式。
        // 到这里可以得到他和方向向量所在的平面方程：p(m^2-n^2-p^2)(X-x0)+m(p^2-m^2-n^2)(Z-z0)=0,没有Y，所以平面一定过Y轴
        // 最后没有用到这个平面. 视角在mark的斜上后方，那视角的后肯定是针对方向向量来说的后，上应该就和防止头脚颠倒的这个方向。
//        float[] XOZ = {0,1,0}; // (a2,b2,c2)
//        float[] dir_ver = new float[3];


        float[] head = {h1,h2,h3};
        float[] des = new float[6]; //数组的前三位存视角坐标，后三位用来存看进去的法向量

//        final float rad = (float) (45*(Math.PI/180)); //一般都要把角度转换成弧度。 前面的45就是我们常用的角度，是可以看情况定的
        final float behind = 0.1f; //用它来控制视角相对于mark向后移了多少
        final float up = 0.1f; //用它来控制视角相对于mark向后上抬了多少
        final float front = 0.3f; //它来控制mark在方向向量的方向上，视角可以看到的赛道长度,然后就可以确定法向量
//        dir_ver[0] = p; //a2
//        dir_ver[1] = 0; //b2
//        dir_ver[2] = m; //c2
        float newM = (float) (m/Math.sqrt(m*m+n*n+p*p));
        float newN = (float) (n/Math.sqrt(m*m+n*n+p*p));
        float newP = (float) (p/Math.sqrt(m*m+n*n+p*p)); //都先归一化一下，防止方向向量会超过1 （其实也没必要好像，但是考虑到block大小是1）

        float newH1 = (float) (head[0]/Math.sqrt(head[0]*head[0]+head[1]*head[1]+head[2]*head[2]));
        float newH2 = (float) (head[1]/Math.sqrt(head[0]*head[0]+head[1]*head[1]+head[2]*head[2]));
        float newH3 = (float) (head[2]/Math.sqrt(head[0]*head[0]+head[1]*head[1]+head[2]*head[2]));

        des[0] = x0-behind*newM+up*newH1;
        des[1] = y0-behind*newN+up*newH2;
        des[2] = z0-behind*newP+up*newH3;  // 求出来的这个视角的法向量，就用mark沿着方向向量前进一点的点，减去视角的坐标
//        des[0] = x0-behind*newM;
//        des[1] = y0-behind*newN;
//        des[2] = z0-behind*newP;  // 求出来的这个视角的法向量，就用mark沿着方向向量前进一点的点，减去视角的坐标

        float[] aid = {des[0]-x0,des[1]-y0,des[2]-z0}; // 只是一个辅助的中间变量
        if ((head[0]*aid[0]+head[1]*aid[1]+head[2]*aid[2])/(Math.sqrt(head[0]*head[0]+head[1]*head[1]+head[2]*head[2])*Math.sqrt(aid[0]*aid[0]+aid[1]*aid[1]+aid[2]*aid[2])) < 0){
            des[0] = x0-behind*newM-up*newH1;
            des[1] = y0-behind*newN-up*newH2;
            des[2] = z0-behind*newP-up*newH3;  // 为了使视角跟防止头脚颠倒的向量 保持一致： 同时在上或者同时在下
        }

        des[3] = (x0+front*newM)-des[0];
        des[4] = (y0+front*newN)-des[1];
        des[5] = (z0+front*newP)-des[2];

        return des;
    }

    public ArrayList<Float> tangentPlane(float x0,float y0,float z0,float m,float n, float p,float Pix){
// (x0,y0,z0)是视角的那个点，(m,n,p)是法向量，t是法线参数方程的参数t，Pix是block的像素
        ArrayList<Float> sec = new ArrayList<Float>();
        float x1 = x0; // + m*t;
        float y1 = y0; // + n*t;
        float z1 = z0; // + p*t;

        if (m!=0  & ((n*y1+p*z1)/m+x1) <= Pix & ((n*y1+p*z1)/m+x1)>=0){
            sec.add((n*y1+p*z1)/m+x1);
            sec.add((float) 0.0);
            sec.add((float) 0.0);
        }
        if (m!=0 & ((n*y1+p*(z1-Pix))/m+x1)<=Pix & ((n*y1+p*(z1-Pix))/m+x1)>=0){
            sec.add((n*y1+p*(z1-Pix))/m+x1);
            sec.add((float)0.0);
            sec.add(Pix);
        }
        if (m!=0 & ((n*(y1-Pix)+p*(z1-Pix))/m+x1)<=Pix & ((n*(y1-Pix)+p*(z1-Pix))/m+x1)>=0){
            sec.add((n*(y1-Pix)+p*(z1-Pix))/m+x1);
            sec.add(Pix);
            sec.add(Pix);
        }
        if (m!=0 & ((n*(y1-Pix)+p*z1)/m+x1)<=Pix & ((n*(y1-Pix)+p*z1)/m+x1)>=0){
            sec.add((n*(y1-Pix)+p*z1)/m+x1);
            sec.add(Pix);
            sec.add((float)0.0);
        }

        if (n!=0 & ((m*x1+p*z1)/n+y1)<Pix & ((m*x1+p*z1)/n+y1)>0){
            sec.add((float)0.0);
            sec.add((m*x1+p*z1)/n+y1);
            sec.add((float)0.0);
        }
        if (n!=0 & ((m*x1+p*(z1-Pix))/n+y1)<Pix & ((m*x1+p*(z1-Pix))/n+y1)>0){
            sec.add((float)0.0);
            sec.add((m*x1+p*(z1-Pix))/n+y1);
            sec.add(Pix);
        }
        if (n!=0 & ((m*(x1-Pix)+p*z1)/n+y1)<Pix & ((m*(x1-Pix)+p*z1)/n+y1)>0){
            sec.add(Pix);
            sec.add((m*(x1-Pix)+p*z1)/n+y1);
            sec.add((float)0.0);
        }
        if(n!=0 & ((m*(x1-Pix)+p*(z1-Pix))/n+y1)<Pix & ((m*(x1-Pix)+p*(z1-Pix))/n+y1)>0){
            sec.add(Pix);
            sec.add((m*(x1-Pix)+p*(z1-Pix))/n+y1);
            sec.add(Pix);
        }

        if (p!=0 & ((m*x1+n*y1)/p+z1)<Pix & ((m*x1+n*y1)/p+z1)>0){
            sec.add((float)0.0);
            sec.add((float)0.0);
            sec.add((m*x1+n*y1)/p+z1);
        }
        if (p!=0 & ((m*x1+n*(y1-Pix))/p+z1)<Pix & ((m*x1+n*(y1-Pix))/p+z1)>0){
            sec.add((float)0.0);
            sec.add(Pix);
            sec.add((m*x1+n*(y1-Pix))/p+z1);
        }
        if (p!=0 & ((m*(x1-Pix)+n*y1)/p+z1)<Pix & ((m*(x1-Pix)+n*y1)/p+z1)>0){
            sec.add(Pix);
            sec.add((float)0.0);
            sec.add((m*(x1-Pix)+n*y1)/p+z1);
        }
        if (p!=0 & ((m*(x1-Pix)+n*(y1-Pix))/p+z1)<Pix & ((m*(x1-Pix)+n*(y1-Pix))/p+z1)>0){
            sec.add(Pix);
            sec.add(Pix);
            sec.add((m*(x1-Pix)+n*(y1-Pix))/p+z1);
        }

//        float cx = (sec.get(0) + sec.get(3) + sec.get(6)) / 3;
//        float cy = (sec.get(1) + sec.get(4) + sec.get(7)) / 3;
//        float cz = (sec.get(2) + sec.get(5) + sec.get(8)) / 3;
//
//        float dx0 = sec.get(0) - cx;
//        float dy0 = sec.get(1) - cy;
//        float dz0 = sec.get(2) - cz;
//
//        for (int i = 1; i < sec.size() / 3; i++){
//            float x = sec.get(i * 3);
//            float y = sec.get(i * 3 + 1);
//            float z = sec.get(i * 3 + 2);
//
//            float dx = x - cx;
//            float dy = y - cy;
//            float dz = z - cz;
//
//
//        }
        return sec;
    }

    public String getFilePath(){
        return filepath;
    }


    public void updateVisual(){
        setVisual(gameCharacter);
    }

    public void setVisual(GameCharacter gameCharacter){
//        System.out.println("AAAAAAAAAAA");
//        System.out.print(position[0]);
//        System.out.print(' ');
//        System.out.print(position[1]);
//        System.out.print(' ');
//        System.out.println(position[2]);
//        System.out.print(head[0]);
//        System.out.print(' ');
//        System.out.print(head[1]);
//        System.out.print(' ');
//        System.out.println(head[2]);
//        System.out.print(dir[0]);
//        System.out.print(' ');
//        System.out.print(dir[1]);
//        System.out.print(' ');
//        System.out.println(dir[2]);
//

        gameCharacter.setThirdPersonal();

        float [] thirdHead = gameCharacter.getThirdHead();
        float [] thirdDir = gameCharacter.getThirdDir();
        float [] thirdPosition = gameCharacter.getThirdPosition();

        System.out.println("AAAAAAAAAAA");
//        System.out.println(head[0] * axis[0] + head[1] * axis[1] + head[2] * axis[2]);
//        System.out.print(axis[0]);
//        System.out.print(' ');
//        System.out.print(axis[1]);
//        System.out.print(' ');
//        System.out.println(axis[2]);
        System.out.print(gameCharacter.getThirdHead()[0]);
        System.out.print(' ');
        System.out.print(gameCharacter.getThirdHead()[1]);
        System.out.print(' ');
        System.out.println(gameCharacter.getThirdHead()[2]);
        System.out.print(gameCharacter.getThirdDir()[0]);
        System.out.print(' ');
        System.out.print(gameCharacter.getThirdDir()[1]);
        System.out.print(' ');
        System.out.println(gameCharacter.getThirdDir()[2]);
        System.out.print(gameCharacter.getThirdPosition()[0]);
        System.out.print(' ');
        System.out.print(gameCharacter.getThirdPosition()[1]);
        System.out.print(' ');
        System.out.println(gameCharacter.getThirdPosition()[2]);

        ArrayList<Float> tangent = gameCharacter.tangentPlane(thirdPosition, thirdDir, 1);

        ArrayList<Float> sec_anti = gameCharacter.sortVertex(tangent);

//        ArrayList<Integer> sec_proj1 = new ArrayList<Integer>();
//        ArrayList<Integer> sec_proj2 = new ArrayList<Integer>();
//        ArrayList<Integer> sec_proj3 = new ArrayList<Integer>();
//        ArrayList<Integer> sec_proj4 = new ArrayList<Integer>();
//        ArrayList<Float> sec_anti = new ArrayList<Float>();
//        ArrayList<Float> sec_copy = new ArrayList<Float>();
//        float gravity_X = 0;
//        float gravity_Y = 0;
//        float gravity_Z = 0;
//
////        ArrayList<Float> tangent = tangentPlane(position[0], position[1], position[2], dir[0], dir[1], dir[2],1);
//        sec_copy = (ArrayList<Float>) tangent.clone();
//
//        System.out.println("TangentPlane:::::");
//        System.out.println(tangent.size());
//
//        for (int i=0;i<tangent.size();i+=3) {
//            gravity_X += tangent.get(i);
//        }
//        for (int i=0;i<tangent.size();i+=3) {
//            gravity_Y += tangent.get(i+1);
//        }
//        for (int i=0;i<tangent.size();i+=3) {
//            gravity_Z += tangent.get(i+2);
//        }
//        gravity_X /= (tangent.size()/3);
//        gravity_Y /= (tangent.size()/3);
//        gravity_Z /= (tangent.size()/3);
//
//        for (int i=0;i<tangent.size();i+=3) {
//            tangent.set(i,tangent.get(i)-gravity_X);
//        }
//        for (int i=0;i<tangent.size();i+=3) {
//            tangent.set(i+1, tangent.get(i+1)-gravity_Y);
//        }
//        for (int i=0;i<tangent.size();i+=3) {
//            tangent.set(i+2, tangent.get(i+2)-gravity_Z);
//        }
//
//        //然后对三维坐标进行映射
//        if (thirdDir[2]==0)
//        //先判断切面是不是与XOY面垂直，如果垂直就映射到XOZ平面
//        {
//            for (int i=0;i<tangent.size();i+=3) {
//                if(tangent.get(i)>=0 & tangent.get(i+2)>=0) {
//
//                    sec_proj1.add(i);
//
//                }// 第一象限
//                else if(tangent.get(i)<=0 & tangent.get(i+2)>=0) {
//
//                    sec_proj2.add(i);
//
//                }// 第二象限
//                else if(tangent.get(i)<=0 & tangent.get(i+2)<=0) {
//
//                    sec_proj3.add(i);
//
//                }// 第三象限
//                else if(tangent.get(i)>=0 & tangent.get(i+2)<=0) {
//
//                    sec_proj4.add(i);
//
//                }// 第四象限
//
//            }
//
//
//
//            //只用判断大于1的情况，如果没有那就刚好不用管了，如果只有一个元素，那也不用排序了
//            if (sec_proj1.size()>1) {
//                for (int i=0;i<sec_proj1.size();i++) {
//                    for (int j=0;j<sec_proj1.size()-i-1;j++) {
//                        if(tangent.get(sec_proj1.get(j))!=0 & tangent.get(sec_proj1.get(j+1))!=0) {
//                            if(tangent.get(sec_proj1.get(j)+2)/tangent.get(sec_proj1.get(j)) > tangent.get(sec_proj1.get(j+1)+2)/tangent.get(sec_proj1.get(j+1))) {
//                                int temp = sec_proj1.get(j);
//                                sec_proj1.set(j, sec_proj1.get(j+1));
//                                sec_proj1.set(j+1, temp); //冒泡排序
//                            }
//                        }
//                        else {
//                            if(tangent.get(sec_proj1.get(j))==0 & tangent.get(sec_proj1.get(j+1))==0) {
//                                if(tangent.get(sec_proj1.get(j)+2)<tangent.get(sec_proj1.get(j+1)+2)) {
//                                    int temp = sec_proj1.get(j);
//                                    sec_proj1.set(j, sec_proj1.get(j+1));
//                                    sec_proj1.set(j+1, temp); //冒泡排序
//                                }
//                            }
//                            else {
//                                if(tangent.get(sec_proj1.get(j))==0) {
//                                    int temp = sec_proj1.get(j);
//                                    sec_proj1.set(j, sec_proj1.get(j+1));
//                                    sec_proj1.set(j+1, temp); //冒泡排序
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            if (sec_proj2.size()>1) {
//                for (int i=0;i<sec_proj2.size();i++) {
//                    for (int j=0;j<sec_proj2.size()-i-1;j++) {
//                        if(tangent.get(sec_proj2.get(j))!=0 & tangent.get(sec_proj2.get(j+1))!=0) {
//                            if(tangent.get(sec_proj2.get(j)+2)/tangent.get(sec_proj2.get(j)) > tangent.get(sec_proj2.get(j+1)+2)/tangent.get(sec_proj2.get(j+1))) {
//                                int temp = sec_proj2.get(j);
//                                sec_proj2.set(j, sec_proj2.get(j+1));
//                                sec_proj2.set(j+1, temp); //冒泡排序
//                            }
//                        }
//                        else {
//                            if(tangent.get(sec_proj2.get(j))==0 & tangent.get(sec_proj2.get(j+1))==0) {
//                                if(tangent.get(sec_proj2.get(j)+2)<tangent.get(sec_proj2.get(j+1)+2)) {
//                                    int temp = sec_proj2.get(j);
//                                    sec_proj2.set(j, sec_proj2.get(j+1));
//                                    sec_proj2.set(j+1, temp); //冒泡排序
//                                }
//                            }
//                            else {
//                                if(tangent.get(sec_proj2.get(j))==0) {
//                                    int temp = sec_proj2.get(j);
//                                    sec_proj2.set(j, sec_proj2.get(j+1));
//                                    sec_proj2.set(j+1, temp); //冒泡排序
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            if (sec_proj3.size()>1) {
//                for (int i=0;i<sec_proj3.size();i++) {
//                    for (int j=0;j<sec_proj3.size()-i-1;j++) {
//                        if(tangent.get(sec_proj3.get(j))!=0 & tangent.get(sec_proj3.get(j+1))!=0) {
//                            if(tangent.get(sec_proj3.get(j)+2)/tangent.get(sec_proj3.get(j)) > tangent.get(sec_proj3.get(j+1)+2)/tangent.get(sec_proj3.get(j+1))) {
//                                int temp = sec_proj3.get(j);
//                                sec_proj3.set(j, sec_proj3.get(j+1));
//                                sec_proj3.set(j+1, temp); //冒泡排序
//                            }
//                        }
//                        else {
//                            if(tangent.get(sec_proj3.get(j))==0 & tangent.get(sec_proj3.get(j+1))==0) {
//                                if(tangent.get(sec_proj3.get(j)+2)<tangent.get(sec_proj3.get(j+1)+2)) {
//                                    int temp = sec_proj3.get(j);
//                                    sec_proj3.set(j, sec_proj3.get(j+1));
//                                    sec_proj3.set(j+1, temp); //冒泡排序
//                                }
//                            }
//                            else {
//                                if(tangent.get(sec_proj3.get(j))==0) {
//                                    int temp = sec_proj3.get(j);
//                                    sec_proj3.set(j, sec_proj3.get(j+1));
//                                    sec_proj3.set(j+1, temp); //冒泡排序
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            if (sec_proj4.size()>1) {
//                for (int i=0;i<sec_proj4.size();i++) {
//                    for (int j=0;j<sec_proj4.size()-i-1;j++) {
//                        if(tangent.get(sec_proj4.get(j))!=0 & tangent.get(sec_proj4.get(j+1))!=0) {
//                            if(tangent.get(sec_proj4.get(j)+2)/tangent.get(sec_proj4.get(j)) > tangent.get(sec_proj4.get(j+1)+2)/tangent.get(sec_proj4.get(j+1))) {
//                                int temp = sec_proj4.get(j);
//                                sec_proj4.set(j, sec_proj4.get(j+1));
//                                sec_proj4.set(j+1, temp); //冒泡排序
//                            }
//                        }
//                        else {
//                            if(tangent.get(sec_proj4.get(j))==0 & tangent.get(sec_proj4.get(j+1))==0) {
//                                if(tangent.get(sec_proj4.get(j)+1)<tangent.get(sec_proj4.get(j+1)+1)) {
//                                    int temp = sec_proj4.get(j);
//                                    sec_proj4.set(j, sec_proj4.get(j+1));
//                                    sec_proj4.set(j+1, temp); //冒泡排序
//                                }
//                            }
//                            else {
//                                if(tangent.get(sec_proj4.get(j))==0) {
//                                    int temp = sec_proj4.get(j);
//                                    sec_proj4.set(j, sec_proj4.get(j+1));
//                                    sec_proj4.set(j+1, temp); //冒泡排序
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//
//        }
//        else {
//            for (int i=0;i<tangent.size();i+=3) {
//                if(tangent.get(i)>=0 & tangent.get(i+1)>=0) {
//
//                    sec_proj1.add(i);
//
//                }// 第一象限
//                else if(tangent.get(i)<=0 & tangent.get(i+1)>=0) {
//
//                    sec_proj2.add(i);
//
//                }// 第二象限
//                else if(tangent.get(i)<=0 & tangent.get(i+1)<=0) {
//
//                    sec_proj3.add(i);
//
//                }// 第三象限
//                else if(tangent.get(i)>=0 & tangent.get(i+1)<=0) {
//
//                    sec_proj4.add(i);
//
//                }// 第四象限
//
//            }
//
//
//
//
//        //只用判断大于1的情况，如果没有那就刚好不用管了，如果只有一个元素，那也不用排序了
//        if (sec_proj1.size()>1) {
//            for (int i=0;i<sec_proj1.size();i++) {
//                for (int j=0;j<sec_proj1.size()-i-1;j++) {
//                    if(tangent.get(sec_proj1.get(j))!=0 & tangent.get(sec_proj1.get(j+1))!=0) {
//                        if(tangent.get(sec_proj1.get(j)+1)/tangent.get(sec_proj1.get(j)) > tangent.get(sec_proj1.get(j+1)+1)/tangent.get(sec_proj1.get(j+1))) {
//                            int temp = sec_proj1.get(j);
//                            sec_proj1.set(j, sec_proj1.get(j+1));
//                            sec_proj1.set(j+1, temp); //冒泡排序
//                        }
//                    }
//                    else {
//                        if(tangent.get(sec_proj1.get(j))==0 & tangent.get(sec_proj1.get(j+1))==0) {
//                            if(tangent.get(sec_proj1.get(j)+1)<tangent.get(sec_proj1.get(j+1)+1)) {
//                                int temp = sec_proj1.get(j);
//                                sec_proj1.set(j, sec_proj1.get(j+1));
//                                sec_proj1.set(j+1, temp); //冒泡排序
//                            }
//                        }
//                        else {
//                            if(tangent.get(sec_proj1.get(j))==0) {
//                                int temp = sec_proj1.get(j);
//                                sec_proj1.set(j, sec_proj1.get(j+1));
//                                sec_proj1.set(j+1, temp); //冒泡排序
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        if (sec_proj2.size()>1) {
//            for (int i=0;i<sec_proj2.size();i++) {
//                for (int j=0;j<sec_proj2.size()-i-1;j++) {
//                    if(tangent.get(sec_proj2.get(j))!=0 & tangent.get(sec_proj2.get(j+1))!=0) {
//                        if(tangent.get(sec_proj2.get(j)+1)/tangent.get(sec_proj2.get(j)) > tangent.get(sec_proj2.get(j+1)+1)/tangent.get(sec_proj2.get(j+1))) {
//                            int temp = sec_proj2.get(j);
//                            sec_proj2.set(j, sec_proj2.get(j+1));
//                            sec_proj2.set(j+1, temp); //冒泡排序
//                        }
//                    }
//                    else {
//                        if(tangent.get(sec_proj2.get(j))==0 & tangent.get(sec_proj2.get(j+1))==0) {
//                            if(tangent.get(sec_proj2.get(j)+1)<tangent.get(sec_proj2.get(j+1)+1)) {
//                                int temp = sec_proj2.get(j);
//                                sec_proj2.set(j, sec_proj2.get(j+1));
//                                sec_proj2.set(j+1, temp); //冒泡排序
//                            }
//                        }
//                        else {
//                            if(tangent.get(sec_proj2.get(j))==0) {
//                                int temp = sec_proj2.get(j);
//                                sec_proj2.set(j, sec_proj2.get(j+1));
//                                sec_proj2.set(j+1, temp); //冒泡排序
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        if (sec_proj3.size()>1) {
//            for (int i=0;i<sec_proj3.size();i++) {
//                for (int j=0;j<sec_proj3.size()-i-1;j++) {
//                    if(tangent.get(sec_proj3.get(j))!=0 & tangent.get(sec_proj3.get(j+1))!=0) {
//                        if(tangent.get(sec_proj3.get(j)+1)/tangent.get(sec_proj3.get(j)) > tangent.get(sec_proj3.get(j+1)+1)/tangent.get(sec_proj3.get(j+1))) {
//                            int temp = sec_proj3.get(j);
//                            sec_proj3.set(j, sec_proj3.get(j+1));
//                            sec_proj3.set(j+1, temp); //冒泡排序
//                        }
//                    }
//                    else {
//                        if(tangent.get(sec_proj3.get(j))==0 & tangent.get(sec_proj3.get(j+1))==0) {
//                            if(tangent.get(sec_proj3.get(j)+1)<tangent.get(sec_proj3.get(j+1)+1)) {
//                                int temp = sec_proj3.get(j);
//                                sec_proj3.set(j, sec_proj3.get(j+1));
//                                sec_proj3.set(j+1, temp); //冒泡排序
//                            }
//                        }
//                        else {
//                            if(tangent.get(sec_proj3.get(j))==0) {
//                                int temp = sec_proj3.get(j);
//                                sec_proj3.set(j, sec_proj3.get(j+1));
//                                sec_proj3.set(j+1, temp); //冒泡排序
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        if (sec_proj4.size()>1) {
//            for (int i=0;i<sec_proj4.size();i++) {
//                for (int j=0;j<sec_proj4.size()-i-1;j++) {
//                    if(tangent.get(sec_proj4.get(j))!=0 & tangent.get(sec_proj4.get(j+1))!=0) {
//                        if(tangent.get(sec_proj4.get(j)+1)/tangent.get(sec_proj4.get(j)) > tangent.get(sec_proj4.get(j+1)+1)/tangent.get(sec_proj4.get(j+1))) {
//                            int temp = sec_proj4.get(j);
//                            sec_proj4.set(j, sec_proj4.get(j+1));
//                            sec_proj4.set(j+1, temp); //冒泡排序
//                        }
//                    }
//                    else {
//                        if(tangent.get(sec_proj4.get(j))==0 & tangent.get(sec_proj4.get(j+1))==0) {
//                            if(tangent.get(sec_proj4.get(j)+1)<tangent.get(sec_proj4.get(j+1)+1)) {
//                                int temp = sec_proj4.get(j);
//                                sec_proj4.set(j, sec_proj4.get(j+1));
//                                sec_proj4.set(j+1, temp); //冒泡排序
//                            }
//                        }
//                        else {
//                            if(tangent.get(sec_proj4.get(j))==0) {
//                                int temp = sec_proj4.get(j);
//                                sec_proj4.set(j, sec_proj4.get(j+1));
//                                sec_proj4.set(j+1, temp); //冒泡排序
//                            }
//                        }
//                    }
//                }
//            }
//        }        }
//
//
//
//        for(int i=0;i<sec_proj1.size();i++) {
//            sec_anti.add(sec_copy.get(sec_proj1.get(i)));
//            sec_anti.add(sec_copy.get(sec_proj1.get(i)+1));
//            sec_anti.add(sec_copy.get(sec_proj1.get(i)+2));
//        }
//        for(int i=0;i<sec_proj2.size();i++) {
//            sec_anti.add(sec_copy.get(sec_proj2.get(i)));
//            sec_anti.add(sec_copy.get(sec_proj2.get(i)+1));
//            sec_anti.add(sec_copy.get(sec_proj2.get(i)+2));
//        }
//        for(int i=0;i<sec_proj3.size();i++) {
//            sec_anti.add(sec_copy.get(sec_proj3.get(i)));
//            sec_anti.add(sec_copy.get(sec_proj3.get(i)+1));
//            sec_anti.add(sec_copy.get(sec_proj3.get(i)+2));
//        }
//        for(int i=0;i<sec_proj4.size();i++) {
//            sec_anti.add(sec_copy.get(sec_proj4.get(i)));
//            sec_anti.add(sec_copy.get(sec_proj4.get(i)+1));
//            sec_anti.add(sec_copy.get(sec_proj4.get(i)+2));
//        }

        float [] vertexPoints = new float[sec_anti.size()];
        for (int i = 0; i < sec_anti.size(); i++){

            vertexPoints[i] = sec_anti.get(i);
            System.out.print(vertexPoints[i]);
            System.out.print(" ");
            if (i % 3 == 2){
                System.out.print("\n");
            }
        }


//        float [] head = locateHead(dir[0], dir[1], dir[2]);

        boolean gameSucceed = driveMode(vertexPoints, thirdDir, thirdHead);

//        if (!gameSucceed){
//            Toast.makeText(context, "wrong vertex to draw", Toast.LENGTH_SHORT);
//        } else {
//            myGLSurfaceView.requestRender();
//        }
    }


    public static float[] locateHead(float m, float n, float p){
        // (x0,y0,z0)是mark的坐标，（x,n,p）是mark前进方向的向量，Pix还是block的大小
        // 1.首先求防止头脚颠倒的向量。 先求一个与方向向量垂直，且与XOZ 平行的向量（则与XOZ法向量垂直（0,1,0））,然后可以直接利用向量积来求。
        // 然后防止头脚颠倒的向量是与前面两个向量(方向向量和与其垂直的向量)垂直的向量（也可以直接利用向量积就可以求出来）
        // 向量积：(a1,b1,c1)与(a2,b2,c2) 向量积 (b1c2-b2c1,a2c1-a1c2,a1b2-a2b1)
        // 方向向量(m,n,p)是 (a1,b1,c1)

//        float[] XOZ = {0,1,0}; // (a2,b2,c2)
//        float[] dir_ver = new float[3];

        float[] des = new float[3];
//        dir_ver[0] = p; //a2
//        dir_ver[1] = 0; //b2
//        dir_ver[2] = m; //c2

        des[0] = n*m;
        des[1] = p*p-m*m;
        des[2] = -p*n;

        return des;

    }


//    public void setGamePosition(float [] position){
//        gamePosition = position;
//    }
//
//    public void setGameDir(float [] dir){
//        gameDir = dir;
//    }
//
//    public void setGameHead(float [] head) {
//        gameHead = head;
//    }

    public void setIfGame(boolean b){
        ifGame = b;
    }

    public void addMarker(float [] position){
        float [] new_marker = ModeltoVolume(position);

        ImageMarker imageMarker_drawed = new ImageMarker(new_marker[0],
                new_marker[1],
                new_marker[2]);

        imageMarker_drawed.type = lastMarkerType;

        markerList.add(imageMarker_drawed);
    }

    public void clearMarkerList(){
        markerList.clear();
    }

    public void clearCurSwcList(){
        curSwcList.clear();
    }

    public void addSwc(V_NeuronSWC seg){
        curSwcList.append(seg);
    }

    private void convertToPerspective(float [] dis){

        float [] thirdDir = gameCharacter.getThirdDir();
        float [] thirdHead = gameCharacter.getThirdHead();

        float z = (thirdDir[0] * dis[0] + thirdDir[1] * dis[1] + thirdDir[2] * dis[2]) / (float)Math.sqrt(thirdDir[0] * thirdDir[0] + thirdDir[1] * thirdDir[1] + thirdDir[2] * thirdDir[2]);
        float y = (thirdHead[0] * dis[0] + thirdHead[1] * dis[1] + thirdHead[2] * dis[2]) / (float)Math.sqrt(thirdHead[0] * thirdHead[0] + thirdHead[1] * thirdHead[1] + thirdHead[2] * thirdHead[2]);
        float [] axis = new float[]{thirdDir[1] * thirdHead[2] - thirdHead[1] * thirdDir[2], thirdDir[2] * thirdHead[0] - thirdHead[2] * thirdDir[0], thirdDir[0] * thirdHead[1] - thirdHead[0] * thirdDir[1]};
//        System.out.print(axis[0]);
//        System.out.print(' ');
//        System.out.print(axis[1]);
//        System.out.print(' ');
//        System.out.println(axis[2]);
        float x = (axis[0] * dis[0] + axis[1] * dis[1] + axis[2] * dis[2]) / (float)Math.sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2]);
        System.out.print(x);
        System.out.print(' ');
        System.out.print(y);
        System.out.print(' ');
        System.out.println(z);
        Matrix.translateM(translateAfterMoveMatrix, 0, x, -y, -z);
    }

    public void setGameCharacter(GameCharacter g){
        gameCharacter = g;
    }

    public GameCharacter getGameCharacter(){
        return gameCharacter;
    }
}







//    public float [] rotateM(float theta, float x, float y, float z){
//        double len = Math.sqrt(x * x + y * y + z * z);
//        double nx = x / len;
//        double ny = y / len;
//        double nz = z / len;
//        double cos = Math.cos(Math.PI * theta / 180.0f);
//        double sin = Math.sin(Math.PI * theta / 180.0f);
//        float [] rotateMatrix = new float[9];
//        rotateMatrix[0] = (float)(nx * nx * (1 - cos) + cos);  rotateMatrix[1] = (float)(nx * ny * (1 - cos) - nz * sin);  rotateMatrix[2] = (float)(nx * nz * (1 - cos) + ny * sin);
//        rotateMatrix[3] = (float)(nx * ny * (1 - cos) + nz * sin);  rotateMatrix[4] = (float)(ny * ny * (1 - cos) + cos);  rotateMatrix[5] = (float)(ny * nz * (1 - cos) - nx * sin);
//        rotateMatrix[6] = (float)(nx * nz * (1 - cos) - ny * sin);  rotateMatrix[7] = (float)(ny * nz * (1 - cos) + nx * sin);  rotateMatrix[8] = (float)(nz * nz * (1 - cos) + cos);
//        //默认旋转轴始终过原点
//        return rotateMatrix;
//    }






//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


//
//    private void initTexture(Context context){
//
//        GLES30.glGenTextures(  //创建纹理对象
//                128, //产生纹理id的数量
//                textures, //纹理id的数组
//                0  //偏移量
//        );
//
////        int textures[] = new int[1]; //生成纹理id
////
////        GLES30.glGenTextures(  //创建纹理对象
////                1, //产生纹理id的数量
////                textures, //纹理id的数组
////                0  //偏移量
////        );
//
//        byte [][] image_data = getIntensity();
//
//        for(int nID=0; nID < 128; nID++ ){
//
//            mTextureId = textures[nID];
//
//            //绑定纹理id，将对象绑定到环境的纹理单元
//            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,mTextureId);
//
//            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
//                    GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_NEAREST);//设置MIN 采样方式
//            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
//                    GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR);//设置MAG采样方式
//            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
//                    GLES30.GL_TEXTURE_WRAP_S,GLES30.GL_CLAMP_TO_EDGE);//设置S轴拉伸方式
//            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
//                    GLES30.GL_TEXTURE_WRAP_T,GLES30.GL_CLAMP_TO_EDGE);//设置T轴拉伸方式
//
//
//            CreateBuffer(image_data[nID]);
//
//            GLES30.glTexImage2D(
//                    GLES30.GL_TEXTURE_2D, //纹理类型
//                    0,//纹理的层次，0表示基本图像层，可以理解为直接贴图
//                    GLES30.GL_RGBA, //图片的格式
//                    128,   //
//                    128,   //
//                    0, //纹理边框尺寸();
//                    GLES30.GL_RGBA,
//                    GLES30.GL_UNSIGNED_BYTE,
//                    imageBuffer
//            );
//
//            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,mTextureId);
//        }
//
//
//
//
//    }


//
//    //初始化纹理
//    private void initTexture(Context context){
//
//        GLES30.glGenTextures(  //创建纹理对象
//                224, //产生纹理id的数量
//                textures, //纹理id的数组
//                0  //偏移量
//        );
//
//
//        byte [][] image_data = getIntensity();
//
//        for(int nID=0; nID < 224; nID++ ){
//
//            mTextureId = textures[nID];
//
//            //绑定纹理id，将对象绑定到环境的纹理单元
//            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,mTextureId);
//
//            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
//                    GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_NEAREST);//设置MIN 采样方式
//            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
//                    GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR);//设置MAG采样方式
//            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
//                    GLES30.GL_TEXTURE_WRAP_S,GLES30.GL_CLAMP_TO_EDGE);//设置S轴拉伸方式
//            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
//                    GLES30.GL_TEXTURE_WRAP_T,GLES30.GL_CLAMP_TO_EDGE);//设置T轴拉伸方式
//
//
//            CreateBuffer(image_data[nID]);
//
//            GLES30.glTexImage2D(
//                    GLES30.GL_TEXTURE_2D, //纹理类型
//                    0,//纹理的层次，0表示基本图像层，可以理解为直接贴图
//                    GLES30.GL_RGBA, //图片的格式
//                    224,   //
//                    224,   //
//                    0, //纹理边框尺寸();
//                    GLES30.GL_RGBA,
//                    GLES30.GL_UNSIGNED_BYTE,
//                    imageBuffer
//            );
//
//            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,mTextureId);
//        }
//
//    }


//    private byte[][] getIntensity(){
//
//        //最终的纹理信息
//        for (int x = 0; x < 224; x++){
//            for (int y = 0; y < 224; y++){
//                for (int z = 0; z < 224; z++){
//                    texPosition[x][y][z] = 0;
//                }
//            }
//        }
//
//        float [] rotateM = rotateM(angle, rotateX, rotateY, rotateZ);
////        int dx = (int)(112 - (rotateM[0] + rotateM[1] + rotateM[2]) * 64 + 0.5f);
////        int dy = (int)(112 - (rotateM[3] + rotateM[4] + rotateM[5]) * 64 + 0.5f);
////        int dz = (int)(112 - (rotateM[6] + rotateM[7] + rotateM[8]) * 64 + 0.5f);
//        float dx = 112.0f - (finalRotateMatrix[0] + finalRotateMatrix[1] + finalRotateMatrix[2]) * 64.0f;
//        float dy = 112.0f - (finalRotateMatrix[3] + finalRotateMatrix[4] + finalRotateMatrix[5]) * 64.0f;
//        float dz = 112.0f - (finalRotateMatrix[6] + finalRotateMatrix[7] + finalRotateMatrix[8]) * 64.0f;
//        float [] d = {dx, dy, dz};  //旋转后与中心的偏移量
//        rawreader rr = new rawreader();
//        String fileName = filepath;
//        int[][][] grayscale =  rr.run(fileName);
//        byte[][] data_image = new byte[224][224 * 224 * 4];
//
////        byte[] final_image = new byte[128 * 128 * 4];
//        for (int x = 0; x < 128; x++){
//            for (int y = 0; y < 128; y++){
//                for (int z = 0; z < 128; z++){
//                    int [] new_position = {0, 0, 0};
//                    for (int i = 0; i < 3; i++){
////                        float new_p = finalMatrix[i * 4] * x + finalMatrix[i * 4 + 1] * y
////                                + finalMatrix[i * 4 + 2] * z + finalMatrix[i * 4 + 3];
//                        float new_p = finalRotateMatrix[i * 3] * x + finalRotateMatrix[i * 3 + 1] * y
//                                + finalRotateMatrix[i * 3 + 2] * z;
////                        float new_p = rotationMatrix[i * 4] * x + rotationMatrix[i * 4 + 1] * y
////                                + rotationMatrix[i * 4 + 2] * z + rotationMatrix[i * 4 + 3];
////                        Log.v("new_p:", Float.toString(new_p));
//                        new_position[i] = (int)(new_p + 0.5f + d[i]);
////                        Log.v("new_position", Integer.toString(new_position[i]));
//                    }
//                    texPosition[new_position[0]][new_position[1]][new_position[2]] = grayscale[x][y][z];
//                }
//            }
//        }
//
//
//        for (int x = 0; x < 224; ++x){
//            for (int y = 0; y < 224; ++y){
//                for (int z = 0; z < 224; z++) {
//                    data_image[z][(x * 224 + y) * 4] = intToByteArray(texPosition[x][y][z])[3];
//                    data_image[z][(x * 224 + y) * 4 + 1] = intToByteArray(texPosition[x][y][z])[3];
//                    data_image[z][(x * 224 + y) * 4 + 2] = intToByteArray(texPosition[x][y][z])[3];
//                    if (texPosition[x][y][z] >= 20)
//                        data_image[z][(x * 224 + y) * 4 + 3] = intToByteArray(255)[3];
//                    else
//                        data_image[z][(x * 224 + y) * 4 + 3] = intToByteArray(0)[3];
//
//                }
//            }
//        }
//
////        for (int x = 0; x < 128; ++x){
////            for (int y = 0; y < 128; ++y){
////                for (int z = 0; z < 128; z++) {
////                    data_image[z][(x * 128 + y) * 4] = intToByteArray(grayscale[x][y][z])[3];
////                    data_image[z][(x * 128 + y) * 4 + 1] = intToByteArray(grayscale[x][y][z])[3];
////                    data_image[z][(x * 128 + y) * 4 + 2] = intToByteArray(grayscale[x][y][z])[3];
////                    if (grayscale[x][y][z] >= 20)
////                        data_image[z][(x * 128 + y) * 4 + 3] = intToByteArray(255)[3];
////                    else
////                        data_image[z][(x * 128 + y) * 4 + 3] = intToByteArray(0)[3];
////
////                }
////            }
////        }
//
//        return data_image;
//    }
