package Camera_P2I;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class VisualizationWindow extends JFrame implements ActionListener, ChangeListener, Runnable  {

    private BufferedImage initialImage, modifiedImage;
    private JPanel content, tweaks;
    private JSlider hue, hueThresh, satThresh, valThresh;
    private JButton resetHue;
    private JLabel img1, img2;
    private JSpinner workPercentage, offsetX, offsetY;
    private JCheckBox subBg;
    private boolean stopThread = false;
    private  Thread updateThread;


    private DetectionMain detector;

    public static void main(String[] args) {

        DetectionMain dec = new DetectionMain();
        VisualizationWindow v = new VisualizationWindow(dec);
        (new Thread(v)).start();


    }

    public void run(){

        while (updateThread.isAlive()){

            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
        this.dispose();

    }

    public VisualizationWindow(DetectionMain d)  {

        detector  = d;
        initialImage = d.getInitialImg();
        modifiedImage = d.getModifiedImg();

        img1 = new JLabel();

        img2 = new JLabel();

        content = new JPanel();
        content.setLayout(new FlowLayout());

        workPercentage = new JSpinner(new SpinnerNumberModel(detector.getWorkFieldPercentage() * 100, 0,100,1));


        JPanel tweaks = new JPanel(); //Le panel pour les sliders
        GridLayout layoutTweaks = new GridLayout(8,2);
        layoutTweaks.setHgap(5);
        layoutTweaks.setVgap(2);
        tweaks.setLayout(layoutTweaks);
        tweaks.setBackground(Color.gray);
        tweaks.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));


        /*
        Création des sliders.
        On se base sur le modèle HSV  : Hue (teinte), Saturation (coloration, vivacité), Value (exposition)
        Dans OpenCV : Hue : 0-180, Sat : 0-255, Val : 0-255
        Le premier slider sert à changer le hue de référence
        Les 3 autres modifient les thresholds pour chaque paramètre.
         */

        hue = new JSlider(0, 180, d.getHue()); //Valeur initiale : vert pur
        hue.setMajorTickSpacing(10);
        hue.setMinorTickSpacing(1);
        hue.setPaintTicks(true);
        hue.addChangeListener(this);



        hueThresh = new JSlider(0, 180, d.getHueThresh());
        hueThresh.add(new JLabel(("Hue threshold")));
        hueThresh.add(new JLabel(("Hue")));
        hueThresh.setMajorTickSpacing(10);
        hueThresh.setMinorTickSpacing(1);
        hueThresh.setPaintTicks(true);
        hueThresh.addChangeListener(this);

        satThresh = new JSlider(0, 255, d.getSatThresh());
        satThresh.add(new JLabel(("Saturation threshold")));
        satThresh.add(new JLabel(("Hue")));
        satThresh.setMajorTickSpacing(10);
        satThresh.setMinorTickSpacing(1);
        satThresh.setPaintTicks(true);
        satThresh.addChangeListener(this);

        valThresh = new JSlider(0, 255, d.getValThresh());
        valThresh.add(new JLabel(("Value threshold")));
        valThresh.setMajorTickSpacing(10);
        valThresh.setMinorTickSpacing(1);
        valThresh.setPaintTicks(true);
        valThresh.addChangeListener(this);

        workPercentage = new JSpinner(new SpinnerNumberModel(detector.getWorkFieldPercentage() * 100, 0,100,1));
        workPercentage.addChangeListener(this);

        offsetX = new JSpinner(new SpinnerNumberModel(detector.getWorkFieldOffsetX(), 0, detector.getTailleMax().width - detector.getDimension().width, 1));
        offsetX.addChangeListener(this);
        offsetY = new JSpinner(new SpinnerNumberModel(detector.getWorkFieldOffsetY(), 0, detector.getTailleMax().height - detector.getDimension().height, 1));
        offsetY.addChangeListener(this);

        //Le bouton pour recalculer le meilleur hue
        resetHue = new JButton("Reset Hue");
        resetHue.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                detector.drawHueRegion(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                detector.drawHueRegion(false);
            }
        });
        resetHue.addActionListener(this);

        subBg = new JCheckBox("Soustraire arrière-plan");
        subBg.setBackground(Color.gray);
        subBg.addActionListener(this);



        tweaks.add(hue);
        tweaks.add(new JLabel(("Hue")));
        tweaks.add(hueThresh);
        tweaks.add(new JLabel("Threshold for hue"));
        tweaks.add(satThresh);
        tweaks.add(new JLabel("Threshold for saturation"));
        tweaks.add(valThresh);
        tweaks.add(new JLabel("Threshold for value"));
        tweaks.add(workPercentage);
        tweaks.add(new JLabel("Pourcentage de l'image utilisée"));
        tweaks.add(offsetX);
        tweaks.add(new JLabel("Offset horizontal"));
        tweaks.add(offsetY);
        tweaks.add(new JLabel("Offset vertical"));
        tweaks.add(resetHue);
        tweaks.add(subBg);


        content.add(img1);
        content.add(img2);
        content.add(tweaks);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopThread = true;
                System.out.println("Visualization window is closing");
            }
        });

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(content);
        pack();
        setVisible(true);

        updateThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while(stopThread == false) {

                    if (!isFocused()) {
                        d.setPanic(false);
                        d.moveMouse();
                    } else {
                        d.setPanic(true);
                        d.moveMouse();
                        update();
                    }


                }
            }
        });

        updateThread.start();

    }
    public void actionPerformed(ActionEvent e) {

        if(e.getSource() == resetHue){

            detector.setBestHue();
            hue.setValue(detector.getHue());

        }
        if(e.getSource() == subBg){

            detector.setSubBackground(subBg.isSelected());

        }


    }

    public void stateChanged(ChangeEvent e){

        if(e.getSource() != workPercentage && e.getSource() != offsetX && e.getSource()!= offsetY) {
            detector.setHue(hue.getValue());
            System.out.println("Hue :" + hue.getValue());
            detector.setHueThresh(hueThresh.getValue());
            detector.setSatThresh(satThresh.getValue());
            detector.setValThresh(valThresh.getValue());
        }else{

            if(e.getSource() == workPercentage)
            detector.setWorkFieldPercentage(((Double) workPercentage.getValue()).doubleValue() / 100.0);



                if (e.getSource() == offsetX)
                    detector.setWorkFieldOffsetX(((Integer) offsetX.getValue()).intValue());
                if (e.getSource() == offsetY)
                    detector.setWorkFieldOffsetY(((Integer) offsetY.getValue()).intValue());

                offsetX.setModel(new SpinnerNumberModel(detector.getWorkFieldOffsetX(), 0, detector.getTailleMax().width - detector.getDimension().width, 1));
                offsetY.setModel(new SpinnerNumberModel(detector.getWorkFieldOffsetY(), 0, detector.getTailleMax().height - detector.getDimension().height, 1));
            }



    }



    public void update(){

        initialImage = detector.getInitialImg();
        modifiedImage = detector.getModifiedImg();

        img1.setIcon(new ImageIcon(initialImage));
        img2.setIcon(new ImageIcon(modifiedImage));

        pack();
        repaint();

    }





}
