public class Misc {

    int zero(int a, int b) {
        int x = a + b;
        int y = x - a;
        int z = y - b;
        if (z == 0) {
            return z;
        }
        return 1;
    }

    boolean isNotZero(int a, int b) {
        if (a == 0) {
            return false;
        }
        int x = a - b;
        int y = x + b;
        return y != 0;
    }

    int maxOfThree(int a, int b, int c) {
        if (a > b) {
            if (a > c) {
                return a;
            }
            return c;
        }
        if (b > c) {
            return b;
        }
        return c;
    }

    int identityOnFirstArg(int a, int b) {
        int y = b * a;
        int z = b * y - y * a;
        if (z == 0) {
            z = 1;
        }
        return a;
    }
}
