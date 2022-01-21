public class DataFlow {
    int dataflow1(boolean c) {
        int result = 0;
        if (c) {
            result = 1;
        }
        return result;
    }

    boolean dataflow2(boolean c1, boolean c2) {
        int result = 0;
        if (c1) {
            result = 1;
        }
        if (c2) {
            result += 1;
        }
        return result == 0;
    }

    int miniTriangle(int a, int b, int c) {
		final int INVALID = 0, SCALENE = 1, OTHER = 2, ISOSCELES = 3;
        int trian = 0;
        if (a <= 0 || b <= 0 || c < 0) return INVALID;
        if (a == b) {
            trian += 1;
        }
        if (trian == 0) {
            if (a + b <= c) {
                return INVALID;
            }
            return SCALENE;
        }
        return OTHER;
    }
}
