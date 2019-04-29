

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.video.Video;
import  org.opencv.video.BackgroundSubtractor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import static org.opencv.imgproc.Imgproc.*;


public class DetectionMain extends Thread {

    private boolean capture;
    private Mat capImg,  colMask, imgFin;  //Matrice de capture //Image filtrée N&B  //Image après traitement
    private BufferedImage image;  //Image à afficher
    private BackgroundSubtractor bg; //Objet OpenCV pour supprimer l'arrière plan
    private  Mat[] imgTest;
    private Point[] ext;
    private int hue, hueThresh, valThresh, satThresh;
    private Point center;
    private  VideoCapture capture;


    /* Constructeur de la classe

    *
    *
    */

    public DetectionMain(){

        //Loading the OpenCV core library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        this.capture = new VideoCapture(0);

        if (!capture.isOpened()) {
            System.out.println("Camera not opened");
            return;
        }

        this.bg = Video.createBackgroundSubtractorMOG2();

        //Affichage de la première image pour être sûr que la caméra fonctionne
        capture.read(capImg);
        image = (BufferedImage) HighGui.toBufferedImage(capImg);

        imgTest = new Mat[10];

        for (int i = 0; i < imgTest.length; i++) {
            boolean isCapDone = false;
            imgTest[i] = new Mat();

            while (!isCapDone) {
                if (capture.read(imgTest[i])) {
                    isCapDone = true;
                }
            }
        }
    }


}








        while (true) {

        if (capture.read(capImg)) {

            //Traitement de l'image

            int hue = hue.getValue();
            int huethresh = hueThresh.getValue();
            int satthresh = satThresh.getValue();
            int valthresh = valThresh.getValue();

            colMask = getFilteredImage(capImg, new Scalar(ihue - ihuethresh, isatthresh, ivalthresh), new Scalar(ihue + ihuethresh, 255, 255));

            imgFin = subBackground(colMask, bg);

            Point[] ext = findExtPoints(imgFin);

            Point center = new Point((ext[0].x + ext[1].x )/ 2, (ext[0].y + ext[1].y) / 2);

            Imgproc.cvtColor(imgFin, imgFin, COLOR_GRAY2BGR);

            Imgproc.rectangle(imgFin, ext[0], ext[1], new Scalar(0, 255, 0));
            Imgproc.circle(imgFin, center, 5, new Scalar(255,0,0));

        }


    }
}

    //Calcul du meilleur hue : On regarde pour chaque hue précisément (threshold de 0) le nombre de pixels qui passent à travers le filtre et on séléctionne celui ou il y en à le +

    public static int calcBestHue(Mat[] imgTest) {

        int bestHue = 0;
        int maxPix = 0;

        for (int h = 0; h < 180; h++) {

            int sumOfPix = 0;

            for (int i = 0; i < imgTest.length; i++) {

                sumOfPix += Core.countNonZero(getFilteredImage(imgTest[i], new Scalar(h, 100, 100), new Scalar(h, 255, 255)));

            }

            //System.out.println("Hue :"+ h + " pix number : " + sumOfPix);

            if (sumOfPix > maxPix) {

                maxPix = sumOfPix;
                bestHue = h;
            }

        }

        System.out.println("Best hue : " + bestHue);
        return bestHue;

    }

    private Mat getFilteredImage( Scalar lowerBound, Scalar upperBound) { //Filtre de couleur seule


        Mat imgHSV = new Mat(); //Matrice HSV
        Mat colMask = new Mat(); //Image filtrée

        Imgproc.cvtColor(imageBGR, imgHSV, COLOR_BGR2HSV); //Conversion au format HSV

        Core.inRange(imgHSV, lowerBound, upperBound, colMask);

        return colMask;

    }

     private Mat subBackground(BackgroundSubtractor bs) { //Filtre de l'arrière plan : même code qu'Adèle mais avec l'objet de OpenCV qui le fait pour nous

        Mat bgMask = new Mat();
        Mat matDiff = new Mat();
        Mat matRet = new Mat();

        bs.apply(img, bgMask, -1); //Il faut trouver une bonne valeur du learning rate

        Core.copyTo(img, matDiff, bgMask);

        Imgproc.medianBlur(matDiff, matDiff, 5); //Filtre médian

        Imgproc.morphologyEx(matDiff, matRet, MORPH_OPEN, Imgproc.getStructuringElement(MORPH_RECT, new Size(5, 5))); //On retire les derniers amas de pixels qui subsistent après le filtre

        return matDiff;

    }

    //Fonction qui retourne les deux points extrémums des formes détectées (pour pouvoir tracer un carré autour) et coordonnées du centre de gravité de la main

      private Point[] findExtPoints(Mat img) {

        Point[] ret = new Point[3]; // [0] et [1]: coordonnées des min et max   [2] coordonnées du centre de gravité de la main

        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        // ArrayList<int> xCoordinatesMassCenter = new ArrayList<int>(); //Tous les x des points de contours
        //ArrayList<int> yCoordinatesMassCenter= new ArrayList<int>(); // Tous les y des points de contours
        int sumX=0;
        int sumY=0;

        Mat hierarchy = new Mat();
        Imgproc.findContours(img, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);


        if (contours.size() > 0 ) {
            int minx = 640, miny = 480, maxx = 0, maxy = 0;

            for (MatOfPoint m : contours) {

                Point[] cont = m.toArray();

                for (Point p : cont) {

                    sumX+= (int) p.x;
                    sumY+= (int) p.y;

                    if (p.x > maxx) maxx = (int) p.x;
                    if (p.x < minx) minx = (int) p.x;

                    if (p.y > maxy) maxy = (int) p.y;
                    if (p.y < miny) miny = (int) p.y;

                }

            }


            Point centerOfMasse= new Point(sumY/contours.size(),sumX/ contours.size());

            ret[0] = new Point(minx, miny);
            ret[1] = new Point(maxx, maxy);
            ret[2]= centerOfMasse;
            return ret;

        } else {

            ret[0] = new Point(-1, -1);
            ret[1] = ret[0];
            return ret;
        }

    }

}


