package com.tracingfunc.gsdt;

import com.example.basic.Image4DSimple;
import com.example.basic.ImageMarker;

import static java.lang.System.out;

public class GSDT {
    public static boolean GSDT_Fun(ParaGSDT p) throws Exception {
        //Image4DSimple inimg = loadImage(img_filepath,img_filetype);
        if(!p.p4DImage.valid()){
            out.println("image is invalid!");
            return false;
        }
        Image4DSimple inimg = p.p4DImage;
        p.outImage = inimg;
        long sz0 = inimg.getSz0();
        long sz1 = inimg.getSz1();
        long sz2 = inimg.getSz2();
        long sz3 = inimg.getSz3();
        //out.println(sz0+" "+sz1+" "+sz2+" "+sz3);
        //int bkg_thresh = p.bkg_thresh;
        int cnn_type = p.cnn_type;
        p.channel = (int)sz3;
        out.println("p.channel:"+p.channel);
        int z_thickness = p.z_thickness;
        p.phi = new float[(int) sz2][(int) sz1][(int) sz0];
        int[] sz = new int[]{(int) sz0,(int) sz1,(int) sz2};
        out.println("FM_GSDTfun..");

        //reset the para bkg_thresh(eg.image_ave)
        for(int c=0;c<=(p.channel-1);c++){
            //out.println("here now");
            double imgAve, imgStd;
            //out.println("here now");
            double[] meanStd = inimg.getMeanStdValue(c);//problems here!!
            //out.println(meanStd[0]+"----"+meanStd[1]);
            //out.println("here now!");
            imgAve = meanStd[0];
            imgStd = meanStd[1];
            //out.println("here now!!");
            double td= (imgStd<10)? 10: imgStd;
            p.bkg_thresh[c] = (int) (imgAve +0.5*td) ; //(imgAve < imgStd)? imgAve : (imgAve+imgStd)*.5; //20170523, PHC
            //out.println(p.bkg_thresh[c]);
        }
        out.println(p.bkg_thresh[0]+"====="+p.bkg_thresh[1]+"====="+p.bkg_thresh[2]);

        //prepare the processed img format
        int[][][][] outimage = new int[(int) sz3][(int) sz2][(int) sz1][(int) sz0];//CZYX
        for(int cc=0;cc<=(p.channel-1);cc++){
            int bkg_thresh = p.bkg_thresh[cc];
            out.println("here now!!");
            FM_GSDT.gsdt_FM(inimg.getDataCZYX()[cc],p.phi, sz, cnn_type, bkg_thresh);
            //find the local maximum
            p.max_val = p.phi[0][0][0];
            for(int ix=10;ix<=(sz[0]-11);ix=ix+21){
                for(int iy=10;iy<=(sz[1]-11);iy=iy+21){
                    for(int iz=10;iz<=(sz[2]-11);iz=iz+21){
                        p.local_maxval = p.phi[iz][iy][ix];
                        p.local_maxloc[0] = ix;
                        p.local_maxloc[1] = iy;
                        p.local_maxloc[2] = iz;
                        for(int dx=-10;dx<=10;dx++){
                            for(int dy=-10;dy<=10;dy++){
                                for(int dz=-10;dz<=10;dz++){
                                    if(p.phi[iz+dz][iy+dy][ix+dx]>p.local_maxval){
                                        p.local_maxval = p.phi[iz+dz][iy+dy][ix+dx];
                                        p.local_maxloc[0] = ix + dx;
                                        p.local_maxloc[1] = iy + dy;
                                        p.local_maxloc[2] = iz + dz;
                                    }
                                }
                            }
                        }
                        if(p.local_maxval>p.max_val){
                            p.max_val = p.local_maxval;
                            p.max_loc[0] = p.local_maxloc[0];
                            p.max_loc[1] = p.local_maxloc[1];
                            p.max_loc[2] = p.local_maxloc[2];
                        }
                        if(p.local_maxval>=200){
                            ImageMarker marker_new = new ImageMarker(p.local_maxloc[0], p.local_maxloc[1], p.local_maxloc[2]);
                            marker_new.radius = 5;
                            marker_new.type = 3;
                            p.markers.add(marker_new);
                        }
                    }
                }
            }
            //prepare the processed img format
            for(int zz=0;zz<sz2;zz++){
                for(int yy=0;yy<sz1;yy++){
                    for(int xx=0;xx<sz0;xx++){
                        outimage[cc][zz][yy][xx] = (int)p.phi[zz][yy][xx];
                    }
                }
            }
        }

        //int bkg_thresh = p.bkg_thresh;
        //FM_GSDT.gsdt_FM(inimg.getDataCZYX()[0],p.phi, sz, cnn_type, bkg_thresh);//re
        //FM.fastmarching_dt(inimg.getDataCZYX()[0],p.phi, sz, cnn_type, bkg_thresh);

        //prepare the processed img format
        //int[][][][] outimage = new int[(int) sz3][(int) sz2][(int) sz1][(int) sz0];//CZYX
        p.outImage.setDataFormCZYX(outimage,sz0,sz1,sz2,sz3,p.p4DImage.getDatatype(),p.p4DImage.getIsBig());
        out.println("FM_GSDTfun..finished");
/*
        //find the local maximum
        p.max_val = p.phi[0][0][0];
        for(int ix=10;ix<=(sz[0]-11);ix=ix+21){
            for(int iy=10;iy<=(sz[1]-11);iy=iy+21){
                for(int iz=10;iz<=(sz[2]-11);iz=iz+21){
                    p.local_maxval = p.phi[iz][iy][ix];
                    p.local_maxloc[0] = ix;
                    p.local_maxloc[1] = iy;
                    p.local_maxloc[2] = iz;
                    for(int dx=-10;dx<=10;dx++){
                        for(int dy=-10;dy<=10;dy++){
                            for(int dz=-10;dz<=10;dz++){
                                if(p.phi[iz+dz][iy+dy][ix+dx]>p.local_maxval){
                                    p.local_maxval = p.phi[iz+dz][iy+dy][ix+dx];
                                    p.local_maxloc[0] = ix + dx;
                                    p.local_maxloc[1] = iy + dy;
                                    p.local_maxloc[2] = iz + dz;
                                }
                            }
                        }
                    }
                    if(p.local_maxval>p.max_val){
                        p.max_val = p.local_maxval;
                        p.max_loc[0] = p.local_maxloc[0];
                        p.max_loc[1] = p.local_maxloc[1];
                        p.max_loc[2] = p.local_maxloc[2];
                    }
                    if(p.local_maxval>=100){
                        ImageMarker marker_new = new ImageMarker(p.local_maxloc[0], p.local_maxloc[1], p.local_maxloc[2]);
                        marker_new.radius = 1;
                        marker_new.type = 3;
                        p.markers.add(marker_new);
                    }
                }
            }
        }
*/
        p.MaxMarker = new ImageMarker(p.max_loc[0], p.max_loc[1], p.max_loc[2]);
        p.MaxMarker.radius = 3;
        p.MaxMarker.type = 2;

        return true;
    }
}
