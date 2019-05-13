package Camera_P2I;

import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Moments;

import java.awt.image.BufferedImage;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.opencv.imgproc.Imgproc.*;


public class DetectionMain  {

    private Mat capImg,  colMask, imgFin;  //Matrice de capture //Image filtrée N&B  //Image après traitement
    private BufferedImage initialImg, modifiedImg;  //Image à afficher
    private BackgroundSubtractor bg; //Objet OpenCV pour supprimer l'arrière plan
    private Point[] ext;
    private int hue, hueThresh, valThresh, satThresh;
    private Point center;
    private  VideoCapture capture;
    private ArrayList<Point> centerHistory;
    private boolean handDetected = false;


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

        capImg = new Mat();
        colMask = new Mat();
        imgFin = new Mat();

        this.bg = Video.createBackgroundSubtractorMOG2();

        //Affichage de la première image pour être sûr que la caméra fonctionne
        capture.read(capImg);
        initialImg = (BufferedImage) HighGui.toBufferedImage(capImg);

        centerHistory = new ArrayList<Point>();

        setBestHue();
    }


    public int[] getHandCoordinates() {

        Point p = new Point(-1,-1);

        if (capture.read(capImg)) {

            //Traitement de l'image



            colMask = getFilteredImage(capImg, new Scalar(hue - hueThresh, satThresh, valThresh), new Scalar(hue + hueThresh, 255, 255));

            imgFin = subBackground(colMask, bg);

            Core.flip(imgFin, imgFin, 1);
            Core.flip(capImg, capImg, 1);

            Point[] ext = findExtPoints(imgFin);

            Point center = new Point((ext[0].x + ext[1].x )/ 2, (ext[0].y + ext[1].y) / 2);

            if(center.x != -1) {
                handDetected = true;
                centerHistory.add(0, center);

                if (centerHistory.size() > 5)
                    centerHistory.remove(centerHistory.size() - 1);
            }else{
                handDetected = false;
            }


            Imgproc.cvtColor(imgFin, imgFin, COLOR_GRAY2BGR);

            p = hannWindow(centerHistory);
            Imgproc.rectangle(imgFin, ext[0], ext[1], new Scalar(0, 255, 0));
            Imgproc.circle(imgFin, center, 5, new Scalar(255,0,0));
            Imgproc.circle( imgFin, p, 5, new Scalar(255,0,255));

            initialImg = (BufferedImage) HighGui.toBufferedImage(capImg);
            modifiedImg = (BufferedImage) HighGui.toBufferedImage(imgFin);

            //p = center;

        }else{

            System.out.println("Camera couldn't snap");

        }

        int[] ret = new int[2];

        ret[0] = (int) p.x;
        ret[1] = (int) p.y;
        return ret;

    }



    public boolean isHandDetected(){

        return handDetected;

    }




    //Calcul du meilleur hue : On regarde pour chaque hue précisément (threshold de 0) le nombre de pixels qui passent à travers le filtre et on séléctionne celui ou il y en à le +

    public void setBestHue() {

        Mat[] imgTest = new Mat[10];

        for (int i = 0; i < imgTest.length; i++) {
            boolean isCapDone = false;
            imgTest[i] = new Mat();

            while (!isCapDone) {
                if (capture.read(imgTest[i])) {
                    isCapDone = true;
                }
            }


        }

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
        this.hue = bestHue;

    }

    private Mat getFilteredImage(Mat img, Scalar lowerBound, Scalar upperBound) { //Filtre de couleur seule


        Mat imgHSV = new Mat(); //Matrice HSV
        Mat colMask = new Mat(); //Image filtrée

        Imgproc.cvtColor(img, imgHSV, COLOR_BGR2HSV); //Conversion au format HSV

        Core.inRange(imgHSV, lowerBound, upperBound, colMask);

        return colMask;

    }

     private Mat subBackground(Mat img, BackgroundSubtractor bs) { //Filtre de l'arrière plan : même code qu'Adèle mais avec l'objet de OpenCV qui le fait pour nous

        Mat bgMask = new Mat();
        Mat matDiff = new Mat();
        Mat matRet = new Mat();

        bs.apply(img, bgMask, -1); //Il faut trouver une bonne valeur du learning rate

        Core.copyTo(img, matDiff, bgMask);

        Imgproc.medianBlur(matDiff, matDiff, 3); //Filtre médian

        Imgproc.morphologyEx(matDiff, matRet, MORPH_OPEN, Imgproc.getStructuringElement(MORPH_RECT, new Size(3, 3))); //On retire les derniers amas de pixels qui subsistent après le filtre

        return matRet;

    }

    //Fonction qui retourne les deux points extrémums des formes détectées (pour pouvoir tracer un carré autour) et coordonnées du centre de gravité de la main

      private Point[] findExtPoints(Mat img) {

        Point[] ret = new Point[3]; // [0] et [1]: coordonnées des min et max   [2] coordonnées du centre de gravité de la main

        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        ArrayList<Integer> xCoordinates = new ArrayList<Integer>(); //Tous les x des points de contours
        ArrayList<Integer> yCoordinates= new ArrayList<Integer>(); // Tous les y des points de contours
        int sumX=0;
        int sumY=0;

        Mat hierarchy = new Mat();
        Imgproc.findContours(img, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        int numberOfPoints = 0;

        if (contours.size() > 0 ) {


            int minx = img.width(), miny = img.height(), maxx = 0, maxy = 0;

            for (MatOfPoint m : contours) {

                Point[] cont = m.toArray();

                for (Point p : cont) {
                    numberOfPoints++;

                    sumX+= (int) p.x;
                    sumY+= (int) p.y;

                    yCoordinates.add( (int)p.y);
                    xCoordinates.add((int)p.x);

                    if (p.x > maxx) maxx = (int) p.x;
                    if (p.x < minx) minx = (int) p.x;

                    if (p.y > maxy) maxy = (int) p.y;
                    if (p.y < miny) miny = (int) p.y;

                }

            }

            //Calcul centre de masses avec méthode des moments


            Moments moments = Imgproc.moments( img);
            Point center= new Point();
            center.x= moments.get_m10() / moments.get_m00();
            center.y= moments.get_m01() / moments.get_m00();


            // Calcul centre de masses avec méthode de la médiane

            Point mass = new Point();
            Collections.sort(xCoordinates);
            Collections.sort(yCoordinates);

            if (xCoordinates.size()%2==0) {
                mass.x = (int)(xCoordinates.get((xCoordinates.size()/ 2)) + xCoordinates.get((xCoordinates.size()/ 2)+1))/2;
            } else {
                mass.x= (int) xCoordinates.get(xCoordinates.size()/2);
            }
            if (yCoordinates.size()%2==0) {
                mass.y=  (int)(yCoordinates.get((yCoordinates.size()/ 2)) + yCoordinates.get((yCoordinates.size()/ 2)+1))/2;
            } else{
                mass.y= (int) yCoordinates.get(yCoordinates.size()/2);
            }


            Point centerOfMass= new Point(sumY/numberOfPoints,sumX/ numberOfPoints);

            ret[0] = new Point(minx, miny);
            ret[1] = new Point(maxx, maxy);
            ret[2]= mass;
            return ret;

        } else {

            ret[0] = new Point(-1, -1);
            ret[1] = ret[0];
            ret[2] = ret[0];
            return ret;
        }

    }

    public Point hannWindow(List<Point> list){

        double sumx = 0;
        double sumy = 0;
        double sumcoefs = 0;

        for(int i = 0; i < list.size()-1; i++){

            Point current = list.get(i);

            if(current.x > -1) {

                double coef = Math.pow(Math.sin((Math.PI * i) / (list.size() - 1)), 2);
                sumcoefs += coef;


                sumx += current.x * coef;
                sumy += current.y * coef;
            }

        }

        int x = (int) (sumx/sumcoefs);
        int y = (int) (sumy/sumcoefs);

        return new Point(x,y);

    }

    public Point moyenneGlissante(List<Point> list, double c){

        int sumx = 0, sumy=0;
        double totCoefs = 0;

        for(int i = 0; i < list.size(); i++){

            Point current = list.get(i);

            double coef = Math.pow(c,i);
            totCoefs += coef;

            sumx += current.x * coef;
            sumy += current.y * coef;

        }

        int x = (int) (sumx/totCoefs);
        int y = (int) (sumy/totCoefs);

        return new Point(x,y);
    }

    public BufferedImage getInitialImg() {
        return initialImg;
    }

    public BufferedImage getModifiedImg(){
        return modifiedImg;
    }

    public int getHue() {
        return hue;
    }

    public void setHue(int hue) {
        this.hue = hue;
    }

    public int getHueThresh() {
        return hueThresh;
    }

    public void setHueThresh(int hueThresh) {
        this.hueThresh = hueThresh;
    }

    public int getValThresh() {
        return valThresh;
    }

    public void setValThresh(int valThresh) {
        this.valThresh = valThresh;
    }

    public int getSatThresh() {
        return satThresh;
    }

    public void setSatThresh(int satThresh) {
        this.satThresh = satThresh;
    }


}


