/**
 * This class contains test cases for heap and context modifying expressions
 */
public class Expressions {
    int x;

    /**
     * This test case modifies {@code this.x} to {@code a} and then returns
     * {@code b}. The resulting serialized PEG should be
     * <pre>
     * (method-root (var b) (wr (path (var this) (derefs x)) (var a) (heap 0)))
     * </pre>
     */
    int foo(int a, int b) {
        // Heap: (heap 0)
        // Context: {a -> (var a), b -> (var b)}
        x = a;
        // Heap: (wr (path (var this) (derefs x)) (var a) (heap 0))
        return b;
    }

    /**
     * This test case tests assigment expressions nested inside other
     * expressions. Here parameter {@code a} is assigned to paramter {@code b},
     * and the resulting value is added to {@code a}. The second instance of
     * {@code a} should now be bound to {@code b}, and the method should return
     * {@code b + b}.
     *
     * The resulting PEG should be
     *
     * <pre>
     * (method-root (+ (var b) (var b)) (heap 0))
     * </pre>
     *
     */
    int assignInExpr(int a, int b) {
        a = (a = b) + a;
        return a;
    }

    /**
     * This test case tests field updates inside of expressions. Here parameter
     * {@code a} is assigned to field {@code this.x}, and the resulting value is
     * added to {@code this.x}, which should now take the value of {@code a}.
     * The method should return {@code a + a}, and the resulting heap should
     * show that {@code this.x == a + a}
     *
     * The resulting PEG should be
     *
     * <pre>
     * (method-root (rd (path (var this) (derefs x)) (wr (path (var this) (derefs x)) (+ (var a) (rd (path (var this) (derefs x)) (wr (path (var this) (derefs x)) (var a) (heap 0)))) (wr (path (var this) (derefs x)) (var a) (heap 0)))) (wr (path (var this) (derefs x)) (+ (var a) (rd (path (var this) (derefs x)) (wr (path (var this) (derefs x)) (var a) (heap 0)))) (wr (path (var this) (derefs x)) (var a) (heap 0))))
     * </pre>
     *
     */
    int fieldUpdateInExpr(int a) {
        this.x = (this.x = a) + this.x;
        return this.x;
    }
}
