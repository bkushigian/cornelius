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
}
