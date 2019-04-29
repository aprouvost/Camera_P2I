

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


public class Main {
    public static void main(String args[]) {
        //Loading the OpenCV core library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        VideoCapture capture = new VideoCapture(0);

        if (!capture.isOpened()) {

            System.out.println("Camera not opened");
            return;

        }

        Mat capImg = new Mat(); //Matrice de capture
        Mat colMask = new Mat(); //Image filtrée N&B
        Mat imgFin = new Mat(); //Image après traitement


        BufferedImage image; //Image à afficher


        BackgroundSubtractor bg = Video.createBackgroundSubtractorMOG2(); //Objet OpenCV pour supprimer l'arrière plan

        //Affichage de la première image pour être sûr que la caméra fonctionne
        capture.read(capImg);
        image = (BufferedImage) HighGui.toBufferedImage(capImg);


        JFrame frame = new JFrame();

        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(new JLabel(new ImageIcon(image)));
        frame.getContentPane().add(new JLabel(new ImageIcon(image)));
        JPanel tweaks = new JPanel(); //Le panel pour les sliders
        tweaks.setLayout(new BoxLayout(tweaks, BoxLayout.Y_AXIS));

        /*
        Création des sliders.
        On se base sur le modèle HSV  : Hue (teinte), Saturation (coloration, vivacité), Value (exposition)
        Dans OpenCV : Hue : 0-180, Sat : 0-255, Val : 0-255
        Le premier slider sert à changer le hue de référence
        Les 3 autres modifient les thresholds pour chaque paramètre.
         */

        JSlider hue = new JSlider(0, 180, 60); //Valeur initiale : vert pur
        hue.add(new JLabel(("Hue")));
        hue.setMajorTickSpacing(10);
        hue.setMinorTickSpacing(1);
        hue.setPaintTicks(true);

        JSlider hueThresh = new JSlider(0, 180, 5);
        hueThresh.add(new JLabel(("Hue threshold")));
        hueThresh.add(new JLabel(("Hue")));
        hueThresh.setMajorTickSpacing(10);
        hueThresh.setMinorTickSpacing(1);
        hueThresh.setPaintTicks(true);

        JSlider satThresh = new JSlider(0, 255, 100);
        satThresh.add(new JLabel(("Saturation threshold")));
        satThresh.add(new JLabel(("Hue")));
        satThresh.setMajorTickSpacing(10);
        satThresh.setMinorTickSpacing(1);
        satThresh.setPaintTicks(true);

        JSlider valThresh = new JSlider(0, 255, 100);
        valThresh.add(new JLabel(("Value threshold")));
        valThresh.setMajorTickSpacing(10);
        valThresh.setMinorTickSpacing(1);
        valThresh.setPaintTicks(true);

        //Le bouton pour recalculer le meilleur hue
        JButton resetHue = new JButton("Reset Hue");
        resetHue.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

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

                hue.setValue(calcBestHue(imgTest));

            }
        });


        tweaks.add(hue);
        tweaks.add(hueThresh);
        tweaks.add(satThresh);
        tweaks.add(valThresh);
        tweaks.add(resetHue);

        frame.getContentPane().add(tweaks);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        //Initialisation, on cherche le meilleur hue, on capture pour cela 10 images de test

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

        hue.setValue(calcBestHue(imgTest));


        while (true) {

            if (capture.read(capImg)) {

                //Traitement de l'image

                int ihue = hue.getValue();
                int ihuethresh = hueThresh.getValue();
                int isatthresh = satThresh.getValue();
                int ivalthresh = valThresh.getValue();

                colMask = getFilteredImage(capImg, new Scalar(ihue - ihuethresh, isatthresh, ivalthresh), new Scalar(ihue + ihuethresh, 255, 255));

                imgFin = subBackground(colMask, bg);

                Point[] ext = findExtPoints(imgFin);

                Point center = new Point((ext[0].x + ext[1].x )/ 2, (ext[0].y + ext[1].y) / 2);

                Imgproc.cvtColor(imgFin, imgFin, COLOR_GRAY2BGR);

                Imgproc.rectangle(imgFin, ext[0], ext[1], new Scalar(0, 255, 0));
                Imgproc.circle(imgFin, center, 5, new Scalar(255,0,0));

            }

            //Affichage de l'image
            image = (BufferedImage) HighGui.toBufferedImage(capImg);
            JLabel tmp = (JLabel) frame.getContentPane().getComponent(0);
            tmp.setIcon(new ImageIcon(image));
            image = (BufferedImage) HighGui.toBufferedImage(imgFin);
            tmp = (JLabel) frame.getContentPane().getComponent(1);
            tmp.setIcon(new ImageIcon(image));
            frame.repaint();

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

    public static Mat getFilteredImage(Mat imageBGR, Scalar lowerBound, Scalar upperBound) { //Filtre de couleur seule


        Mat imgHSV = new Mat(); //Matrice HSV
        Mat colMask = new Mat(); //Image filtrée

        Imgproc.cvtColor(imageBGR, imgHSV, COLOR_BGR2HSV); //Conversion au format HSV

        Core.inRange(imgHSV, lowerBound, upperBound, colMask);

        return colMask;

    }

    public static Mat subBackground(Mat img, BackgroundSubtractor bs) { //Filtre de l'arrière plan : même code qu'Adèle mais avec l'objet de OpenCV qui le fait pour nous

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

    public static Point[] findExtPoints(Mat img) {

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

