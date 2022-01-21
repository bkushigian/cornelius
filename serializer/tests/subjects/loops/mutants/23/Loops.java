public class Loops {

    int simpleFor() {
        int x = 1;

        for (int i = 0; i < 5; i++) {
            x = x + i;
        }

        return x;
    }

    int multipleStatements(int a, int b) {
        for (
            int c = a + b,
                d = (a + b) % c,
                e = 1;
                d < c * a - b;
                a--, b--, c++)
        {
            a = b - 1;

            b = a + 14;
        }

        return a * b;
    }
    


    int y;

    boolean isEven(int x) {
        return (x % 2) == 0;
    }


    int simpleBody() {
        int x = 0;

        while (x < 3) {
            x = x + 1;
        }

        return x;
    }

    int condSideEffect() {
        int x = 0;

        while ((x = x + 1) < 3) {
            x = x + 1;
        }

        return x;
    }

    int multipleVars() {
        int i = 0;
        int j = 1;

        while (i < 10) {
            int temp = i;

            i = j + j;

            j = temp + temp;
        }

        return j;
    }

    int condMethodCall(int x) {
        int i = 0;

        while(isEven(i + x)) {
            i++;
        }

        return i;
    }

    int nestedTheta() {
        int i = 0;

        while (i < 5) {

            int j = 0;

            while (i + j < 5) {
	            j++;
            }

            i++;
        }
        return i;
    }

    int sumUpToNumber(int n) {
        int i = n;
        int total = 0;
        while (i > 0) total += i;
        return i;
    }
}
