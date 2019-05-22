package Camera_P2I;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Moments;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;




public class DetectionMain  {

    private Mat rawImg, capImg,  colMask, imgFin;  //Matrice de capture //Image filtrée N&B  //Image après traitement
    private BufferedImage initialImg, modifiedImg;  //Image à afficher
    private BackgroundSubtractor bg; //Objet OpenCV pour supprimer l'arrière plan
    private int hue, hueThresh, valThresh, satThresh;
    private  VideoCapture capture;
    private ArrayList<Point> centerHistory;
    private boolean handDetected = false, panic = false;
    private Dimension tailleMax, tailleCam, screenSize;
    private Rect croppedBlackBars, croppedWorkRegion, detectedRegion;
    private double workFieldPercentage = 0.85;
    private Point lastCenter;
    private Robot myRobot;
    private double coeffX, coeffY;


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

        rawImg = new Mat();
        capImg = new Mat();
        colMask = new Mat();
        imgFin = new Mat();

        detectedRegion = new Rect();

        hue = 75;
        hueThresh = 5;
        satThresh = 40;
        valThresh = 40;

        this.bg = Video.createBackgroundSubtractorMOG2();

        //Affichage de la première image pour être sûr que la caméra fonctionne

        capture.read(rawImg);
        croppedBlackBars = getCropMask(rawImg);

        System.out.println(croppedBlackBars);

        croppedWorkRegion = croppedBlackBars.clone();

        croppedWorkRegion.x += (1-workFieldPercentage)/2.0 * croppedBlackBars.width;
        croppedWorkRegion.y += (1-workFieldPercentage)/2.0 * croppedBlackBars.height;
        croppedWorkRegion.height = (int) (workFieldPercentage * croppedBlackBars.height);
        croppedWorkRegion.width = (int) (workFieldPercentage * croppedBlackBars.width);


        System.out.println(croppedWorkRegion);

        capImg = getCroppedImg(rawImg, croppedBlackBars);
        Imgproc.cvtColor(capImg,imgFin, Imgproc.COLOR_BGR2GRAY);

        tailleMax = new Dimension(croppedBlackBars.width, croppedBlackBars.height);
        tailleCam= new Dimension( croppedWorkRegion.width, croppedWorkRegion.height);
        screenSize= Toolkit.getDefaultToolkit().getScreenSize();

        System.out.println("width " +tailleCam.getWidth()+ " height " + tailleCam.getHeight());

        initialImg = (BufferedImage) HighGui.toBufferedImage(capImg);

        centerHistory = new ArrayList<Point>();
        centerHistory.add(new Point(-1,-1));
        lastCenter = new Point(-1,-1);

        try {

            myRobot = new Robot();
            myRobot.setAutoDelay(30);
            myRobot.setAutoWaitForIdle(true);

        } catch (AWTException e) {
            e.printStackTrace();
            System.out.println("Coulnd't initialise robot");
        }

        coeffX= ((double) screenSize.getWidth())/tailleCam.getWidth();
        coeffY= ((double) screenSize.getHeight())/ tailleCam.getHeight();


    }


    public int[] getHandCoordinates() {

        Point p = new Point(-1,-1);

        if (capture.read(rawImg)) {

            //Traitement de l'image

            Core.flip(rawImg, rawImg,1);

            colMask = getFilteredImage(rawImg, new Scalar(hue - hueThresh, satThresh, valThresh), new Scalar(hue + hueThresh, 255, 255));

            capImg = subBackground(colMask, bg);

            imgFin = getCroppedImg(capImg, croppedWorkRegion);

            Point[] ext = findExtPoints(imgFin);

            Point center = ext[2];

            if(center.x != -1) {
                handDetected = true;


                    centerHistory.add(0, center);
                    //System.out.println(centerHistory.size());

                if (centerHistory.size() > 7)
                    centerHistory.remove(centerHistory.size() - 1);
            }else{
                handDetected = false;
            }



            p = hannWindow(centerHistory);

            lastCenter = p;
            detectedRegion = Imgproc.boundingRect(new MatOfPoint(ext));


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

        Imgproc.cvtColor(img, imgHSV, Imgproc.COLOR_BGR2HSV); //Conversion au format HSV

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

        Imgproc.morphologyEx(matDiff, matRet, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3))); //On retire les derniers amas de pixels qui subsistent après le filtre

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
                mass.x = (int)(xCoordinates.get((xCoordinates.size()/ 2 - 1)) + xCoordinates.get((xCoordinates.size()/ 2)))/2;
            } else {
                mass.x= (int) xCoordinates.get(xCoordinates.size()/2);
            }
            if (yCoordinates.size()%2==0) {
                mass.y=  (int)(yCoordinates.get((yCoordinates.size()/ 2 -1)) + yCoordinates.get((yCoordinates.size()/ 2)))/2;
            } else{
                mass.y= (int) yCoordinates.get(yCoordinates.size()/2);
            }


            Point centerOfMass= new Point(sumY/numberOfPoints,sumX/ numberOfPoints);

            ret[0] = new Point(minx, miny);
            ret[1] = new Point(maxx, maxy);
            ret[2]= center;
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

        Imgproc.rectangle(rawImg, croppedWorkRegion, new Scalar(0,0,255));
        initialImg = (BufferedImage) HighGui.toBufferedImage(rawImg);

        return initialImg;
    }

    public BufferedImage getModifiedImg(){

        Mat tmp = new Mat();

        Imgproc.cvtColor(imgFin, tmp, Imgproc.COLOR_GRAY2BGR);
        Imgproc.rectangle(tmp, detectedRegion, new Scalar(0, 255, 0));
        Imgproc.circle(tmp, centerHistory.get(0), 5, new Scalar(255,0,0));
        Imgproc.circle( tmp, lastCenter, 5, new Scalar(255,0,255));

        modifiedImg = (BufferedImage) HighGui.toBufferedImage(tmp);

        return modifiedImg;
    }

    public void moveMouse(){

        if(handDetected == true && panic ==false) {
            myRobot.mouseMove((int) (coeffX * lastCenter.x), (int) (coeffY * lastCenter.y));
        }
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

    public Dimension getDimension (){
        return tailleCam;
    }


    public Rect getCropMask(Mat img){

        Mat gray = new Mat();

        Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);

        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        return Imgproc.boundingRect(contours.get(0));

    }

    public Mat getCroppedImg(Mat img, Rect mask){

        return new Mat(img, mask);

    }

    public void setWorkFieldPercentage(double d){

        workFieldPercentage = d;
        croppedWorkRegion = croppedBlackBars.clone();


        croppedWorkRegion.x += (1-workFieldPercentage)/2.0 * croppedBlackBars.width;
        croppedWorkRegion.y += (1-workFieldPercentage)/2.0 * croppedBlackBars.height;
        croppedWorkRegion.height = (int) (workFieldPercentage * croppedBlackBars.height);
        croppedWorkRegion.width = (int) (workFieldPercentage * croppedBlackBars.width);

        tailleCam= new Dimension( croppedWorkRegion.width, croppedWorkRegion.height);

        coeffX = ((double) screenSize.getWidth()) / tailleCam.getWidth();
        coeffY = ((double) screenSize.getHeight()) / tailleCam.getHeight();

        System.out.println("Position et taille de la région de travail :" + croppedWorkRegion);

    }

    public double getWorkFieldPercentage(){
        return workFieldPercentage;
    }

    public void setWorkFieldOffsetX(int x){

        croppedWorkRegion.x = croppedBlackBars.x + x;

    }

    public void setWorkFieldOffsetY(int y){

        croppedWorkRegion.y = croppedBlackBars.y + y;
    }

    public int getWorkFieldOffsetX(){

        return (croppedWorkRegion.x- croppedBlackBars.x);

    }

    public int getWorkFieldOffsetY(){


        return (croppedWorkRegion.y-croppedBlackBars.y);
    }


    public void resetWorkField(){

        croppedWorkRegion = croppedBlackBars.clone();

    }

    public Dimension getTailleMax(){

        return tailleMax;
    }
}


