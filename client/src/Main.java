import java.awt.*;
import java.util.ArrayList;
import java.util.UUID;

public class Main {
    public static void main(String[] args) throws Exception {
        int M = 79;
        int a = 1;
        int b = 0;
        EllipticGroup ellipticGroup = new EllipticGroup(M);
        GUI gui = new GUI(ellipticGroup);
        ArrayList<Integer> divisors;
        int n;
        Point G;
        UUID uuid;
        if (ellipticGroup.setAB(a, b)) {
            try {
                ellipticGroup.generateElements();
                for (Point p : ellipticGroup.elements) {
                    System.out.println(p.x + " " + p.y);
                }
                divisors = ellipticGroup.getDivisors();
                int index;
                do {
                    index = (int) (Math.random() * divisors.size());
                } while (!EllipticGroup.isPrime(divisors.get(index)) || divisors.get(index) == 1);
                n = divisors.get(index);
                gui.setN(n);
                G = ellipticGroup.getBasePoint(n);
                gui.setG(G);
                gui.setLabels();
                Request request = new Request();
                uuid = UUID.randomUUID();
                System.out.println(uuid);
                gui.setGuid(uuid.toString());
                if (!request.post("http://localhost:8081/parameters?guid=" + uuid.toString() + "&M=" + M + "&a=" + a + "&b=" + b + "&xG=" + G.x + "&yG=" + G.y + "&n=" + n)) {
                    System.out.println("server error");
                    System.exit(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            System.out.println("invalid a and b parameters");
            System.exit(1);
        }
    }
}
