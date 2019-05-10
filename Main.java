package Camera_P2I;

public class Main {

    public static void main(String[] args) {

        DetectionMain d = new DetectionMain();
        VisualizationWindow w = new VisualizationWindow(d);

        while(true){

            d.getHandCoordinates();
            w.update();

        }

    }

}
