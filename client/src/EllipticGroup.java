import java.awt.*;
import java.math.BigInteger;
import java.util.*;

public class EllipticGroup {
    double a;
    double b;
    int M;
    Set<Point> elements = new HashSet<>();
    int N;
    //digital signature:
    int r, s;

    EllipticGroup(int M) {
        this.M = M;
    }

    boolean setAB(double a, double b) {
        if (checkAB(a, b)) {
            this.a = a;
            this.b = b;
            return true;
        }
        return false;
    }

    double getY2(double x) {
        return ((Math.pow(x, 3) + a * x + b) % M + M) % M;
    }

    private boolean checkAB(double a, double b) {
        if (((4 * Math.pow(a, 3) + 27 * Math.pow(b, 2)) % M + M) % M == 0) {
            return false;
        }
        return true;
    }

    private int legendreSymbol(int a) {
        if (a % M == 0) {
            return 0;
        }
        if ((new BigInteger(String.valueOf(a)).pow((M - 1) / 2).mod(new BigInteger(String.valueOf(M))).intValue()) == 1) {
            return 1;
        }
        return -1;
    }

    private int inverse(int a, int b) throws Exception {
        int B = b;
        if (a == 0) {
            throw new Exceptions.NoInverseElementException();
        }
        a = (a % B + B) % B;
        int d, x, y;
        if (b == 0) {
            x = 1;
            y = 0;
        } else {
            int q, r, x1 = 0, x2 = 1, y1 = 1, y2 = 0;
            while (b > 0) {
                q = a / b;
                r = a - q * b;
                x = x2 - q * x1;
                y = y2 - q * y1;
                a = b;
                b = r;
                x2 = x1;
                x1 = x;
                y2 = y1;
                y1 = y;
            }
            x = x2;
            y = y2;
        }
        d = a;
        if (d == 1) {
            return (x % B + B) % B;
        }
        return 0;
    }

    void generateElements() throws Exception {
        for (int x = 0; x < M; ++x) {
            int a = (int) getY2(x);
            int legendre = legendreSymbol(a);
            if (legendre != -1) {
                int b;
                do {
                    b = (int) (Math.random() * M) % M;
                } while (legendreSymbol(b) != -1);
                int s = 0, t = M - 1;
                while ((t & 1) == 0) {
                    s++;
                    t >>= 1;
                }
                if (a == 0) {
                    elements.add(new Point(x, 0));
                    continue;
                }
                int aInverse = inverse(a, M);
                int c = new BigInteger(String.valueOf(b)).pow(t).mod(new BigInteger(String.valueOf(M))).intValue();
                int r = new BigInteger(String.valueOf(a)).pow((t + 1) / 2).mod(new BigInteger(String.valueOf(M))).intValue();
                for (int i = 1; i < s; ++i) {
                    int d = (int) Math.pow((Math.pow(r, 2) * aInverse), Math.pow(2, s - i - 1)) % M;
                    if (d == -1) {
                        r = (r * c) % M;
                    }
                    c = (int) Math.pow(c, 2) % M;
                }
                elements.add(new Point(x, (r % M + M) % M));
                elements.add(new Point(x, (-r % M + M) % M));
            }
        }
        N = 1 + elements.size();
    }

    Point doublePoint(Point p) throws Exception {
        if (!contains(p)) {
            throw new Exceptions.BadPointException();
        }
        if (2 * p.y == 0) {
            return null;
        }
        int m = (int) (((3 * Math.pow(p.x, 2) + a) * inverse(2 * p.y, M)) % M + M) % M;
        int x = (int) ((Math.pow(m, 2) - 2 * p.x) % M + M) % M;
        int y = -(p.y + m * (x - p.x));
        return new Point(x, (y % M + M) % M);
    }

    Point sum(Point p, Point q) throws Exception {
        if (!contains(p) || !contains(q)) {
            throw new Exceptions.BadPointException();
        }
        if (p.equals(q)) {
            return doublePoint(p);
        }
        if (p.x - q.x == 0) {
            return null;
        }
        int m = (((p.y - q.y) * inverse(p.x - q.x, M)) % M + M) % M;
        int x = (int) ((Math.pow(m, 2) - p.x - q.x) % M + M) % M;
        int y = (-(p.y + m * (x - p.x)) % M + M) % M;
        return new Point(x, y);
    }

    Point multN(int n, Point p) throws Exception {
        if (!contains(p)) {
            throw new Exceptions.BadPointException();
        }
        Point result = null;
        boolean emptyResult = true;
        Point current = new Point(p);
        if (n % 2 == 1) {
            result = new Point(p);
            emptyResult = false;
        }
        n >>= 1;
        while (n != 0) {
            current = doublePoint(current);
            if (current == null) {
                return null;
            }
            int k = n & (~n + 1);
            if (k == 1) {
                if (emptyResult) {
                    result = new Point(current);
                    emptyResult = false;
                } else {
                    result = sum(result, current);
                    if (result == null) {
                        return null;
                    }
                }
            }
            n >>= 1;
        }
        return result;
    }

    ArrayList<Integer> getDivisors() {
        ArrayList<Integer> divisors = new ArrayList<>();
        for (int i = 1; i < N; i++) {
            if (i > (double) (N / i)) {
                break;
            }
            if (N % i == 0) {
                divisors.add(i);
                if (i != N / i) {
                    divisors.add(N / i);
                }
            }
        }
        return divisors;
    }

    int nP(Point p) throws Exception {
        if (!contains(p)) {
            throw new Exceptions.BadPointException();
        }
        ArrayList<Integer> divisors = getDivisors();
        Collections.sort(divisors);
        for (int divisor : divisors) {
            Point q = multN(divisor, p);
            if (q == null) {
                return divisor;
            }
        }
        return N;
    }

    Point getBasePoint(int n) throws Exception {
        if (N % n != 0) {
            return null;
        }
        if (!isPrime(n)) {
            throw new Exceptions.NotPrimeException();
        }
        int h = N / n;
        Point G = null;
        do {
            int index = (int) (Math.random() * elements.size());
            Point[] setElements = new Point[elements.size()];
            elements.toArray(setElements);
            Point P = setElements[index];
            try {
                G = multN(h, P);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while(G == null);
        return G;
    }

    static boolean isPrime(int n) {
        for (int i = 2; i < n; i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }

    boolean contains(Point p) {
        return (Math.pow(p.y, 2) % M + M) % M == getY2(p.x);
    }

    void setDigitalSignature(int n, Point G, int z, int privateKey) {
        try {
            int k;
            do {
                do {
                    k = (int) (1 + Math.random() * (n - 1));
                    Point P = multN(k, G);
                    r = P.x % n;
                } while (r == 0);
                s = (inverse(k, n) * (z + r * privateKey) % n);
            } while (s == 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
