package Camera_P2I;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class VisualizationWindow extends JFrame implements ActionListener, ChangeListener, KeyListener  {

    private BufferedImage initialImage, modifiedImage;
    private JPanel content, tweaks;
    private JSlider hue, hueThresh, satThresh, valThresh;
    private JButton resetHue;
    private JLabel img1, img2;
    private JSpinner workPercentage, offsetX, offsetY;
    private boolean panic= false;

    private DetectionMain detector;

    public static void main(String[] args) {

        DetectionMain dec = new DetectionMain();
        VisualizationWindow v = new VisualizationWindow(dec);

        while(true) {

            if (!v.isFocused()) {
                dec.setPanic(false);
                dec.moveMouse(true);
            } else {
                dec.setPanic(true);
                dec.moveMouse(true);
                v.update();
            }
        }

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
        tweaks.setLayout(new BoxLayout(tweaks, BoxLayout.Y_AXIS));
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
        hue.add(new JLabel(("Hue")));
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
        resetHue.addActionListener(this);

        // Ajout panic button



        tweaks.add(hue);
        tweaks.add(hueThresh);
        tweaks.add(satThresh);
        tweaks.add(valThresh);
        tweaks.add(workPercentage);
        tweaks.add(offsetX);
        tweaks.add(offsetY);
        tweaks.add(resetHue);


        content.add(img1);
        content.add(img2);
        content.add(tweaks);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setContentPane(content);
        pack();
        setVisible(true);

    }
    public void actionPerformed(ActionEvent e) {

        if(e.getSource() == resetHue){

            detector.setBestHue();

            hue.setValue(detector.getHue());

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



    public boolean getPanic(){
        return panic;
    }

    public void keyPressed(KeyEvent e){
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            panic=true;
            System.out.println("Panic");
        }
    }

    public void keyReleased( KeyEvent e){
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            panic=false;
            System.out.println("Released Panic");
        }
        System.out.println(e);
    }
    public void keyTyped( KeyEvent e){}

}
