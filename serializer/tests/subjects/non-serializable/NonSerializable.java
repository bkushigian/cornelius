public class NonSerializable {
    int x = 0;
    void setX(int x) {
        this.x = x;
    }

    int setAndReturnX(int x) {
        setX(x);
        return this.x;
    }

    int allocateXAsInteger() {
        Integer x = Integer.valueOf(this.x);
        return x;
    }

    int add(int x, int y) {
        return x + y;
    }
}
