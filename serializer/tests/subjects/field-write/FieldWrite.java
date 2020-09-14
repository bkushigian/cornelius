public class FieldWrite {
    int x = 0;

    /**
     * This test case tests field modification and readings.
     *
     * Expected PEG:
     * <pre>
     * (method-root (rd (path (var this) (derefs x)) (wr (path (var this) (derefs x)) (- (var a) (var a)) (heap 0))) (wr (path (var this) (derefs x)) (- (var a) (var a)) (heap 0)))
     * </pre>
     */
    int test_field_write(int a) {
        int result = 0;
        x = a - a;
        result = x;
        return result;
    }

    /**
     * This test case tests field writes and reads with the `max` program
     *
     * Expected PEG:
     * <pre>
     * (method-root (rd (path (var this) (derefs x)) (wr (path (var this) (derefs x)) (phi (> (var a) (var b)) (var a) (var b)) (heap 0))) (wr (path (var this) (derefs x)) (phi (> (var a) (var b)) (var a) (var b)) (heap 0)))
     * </pre>
     */
    int test_heapy_max(int a, int b) {
        x = a > b ? a : b;
        int result = x;
        return result;
    }

    /**
     * This test case tests that void methods return unit and that field updates work.
     * Expected PEG:
     * <pre>
     * (method-root unit (wr (path (var this) (derefs x)) (var x) (heap 0)))
     * </pre>
     */
    void setX(int x) {
        this.x = x;
    }

}
