public class FieldAccess {
    public int x = 0;
    public int y = 0;
    FieldAccess fa = this;

    int test(int ex, int y) {
        int a = x;
        int b = y;
        int c = fa.fa.y;

        x = ex;
        ;
        this.y = b;

        int result = a + b + c;
        return result;
    }

}
