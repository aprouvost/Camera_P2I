package Camera_P2I;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;

public class Main {

    public static  boolean panic = false;

    public static void main(String[] args) throws AWTException {

        DetectionMain d = new DetectionMain();
        VisualizationWindow w = new VisualizationWindow(d);

        Robot r = new Robot();

        Dimension screenSize= Toolkit.getDefaultToolkit().getScreenSize();
        double coeffX= ((double) screenSize.getWidth())/d.getDimension().getWidth();
        double coeffY= ((double) screenSize.getHeight())/ d.getDimension().getHeight();

        System.out.println("coef x : "+ coeffX);
        System.out.println(" coeff y:"+ coeffY);



        int[] c = new int[2];
        Toolkit.getDefaultToolkit().addAWTEventListener(
                new AWTEventListener(){
                    public void eventDispatched(AWTEvent event){
                        KeyEvent ke = (KeyEvent)event;
                        if(ke.getID() == KeyEvent.KEY_RELEASED){
                            //System.out.println("released");
                            if(ke.getKeyCode() == KeyEvent.VK_ESCAPE){

                                panic = false;

                            }
                        }else if(ke.getID() == KeyEvent.KEY_PRESSED){
                            //System.out.println("pressed");
                            if(ke.getKeyCode() == KeyEvent.VK_ESCAPE){

                                panic = true;

                            }
                        }else if(ke.getID() == KeyEvent.KEY_TYPED){
                            //System.out.println("typed");
                        }
                    }
                }, AWTEvent.KEY_EVENT_MASK);


        while(true){

            c = d.getHandCoordinates();
            w.update();

            if(panic == false && d.isHandDetected() == true){

                r.mouseMove((int) (coeffX* c[0]), (int) (coeffY * c[1]));


            }

        }

    }

}
