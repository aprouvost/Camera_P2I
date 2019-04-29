package Camera_P2I;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class VisualizationWindow extends JFrame implements ActionListener, ChangeListener{

    private BufferedImage initialImage, modifiedImage;
    private JPanel content, tweaks;
    private JSlider hue, hueThresh, satThresh, valThresh;
    private JButton resetHue;

    private DetectionMain detector;

    public VisualizationWindow(DetectionMain d)  {

        detector  = d;
        initialImage = d.getInitialImg();
        modifiedImage = d.getModifiedImg();

        content = new JPanel();
        content.setLayout(new FlowLayout());

        JPanel tweaks = new JPanel(); //Le panel pour les sliders
        tweaks.setLayout(new BoxLayout(tweaks, BoxLayout.Y_AXIS));

        /*
        Création des sliders.
        On se base sur le modèle HSV  : Hue (teinte), Saturation (coloration, vivacité), Value (exposition)
        Dans OpenCV : Hue : 0-180, Sat : 0-255, Val : 0-255
        Le premier slider sert à changer le hue de référence
        Les 3 autres modifient les thresholds pour chaque paramètre.
         */

        hue = new JSlider(0, 180, 60); //Valeur initiale : vert pur
        hue.add(new JLabel(("Hue")));
        hue.setMajorTickSpacing(10);
        hue.setMinorTickSpacing(1);
        hue.setPaintTicks(true);

        hueThresh = new JSlider(0, 180, 5);
        hueThresh.add(new JLabel(("Hue threshold")));
        hueThresh.add(new JLabel(("Hue")));
        hueThresh.setMajorTickSpacing(10);
        hueThresh.setMinorTickSpacing(1);
        hueThresh.setPaintTicks(true);

        satThresh = new JSlider(0, 255, 100);
        satThresh.add(new JLabel(("Saturation threshold")));
        satThresh.add(new JLabel(("Hue")));
        satThresh.setMajorTickSpacing(10);
        satThresh.setMinorTickSpacing(1);
        satThresh.setPaintTicks(true);

        valThresh = new JSlider(0, 255, 100);
        valThresh.add(new JLabel(("Value threshold")));
        valThresh.setMajorTickSpacing(10);
        valThresh.setMinorTickSpacing(1);
        valThresh.setPaintTicks(true);

        //Le bouton pour recalculer le meilleur hue
        resetHue = new JButton("Reset Hue");
        resetHue.addActionListener(this);


        tweaks.add(hue);
        tweaks.add(hueThresh);
        tweaks.add(satThresh);
        tweaks.add(valThresh);
        tweaks.add(resetHue);

        content.add(tweaks);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);

    }
    public void actionPerformed(ActionEvent e) {

        detector.setBestHue();

        hue.setValue(detector.getHue());

    }

    public void stateChanged(ChangeEvent e){


    }
}
