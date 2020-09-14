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

    Integer createNewIntegers() {
        return new java.lang.Integer(x) + new Integer(2);
    }

    int add(int x, int y) {
        return x + y;
    }
}
