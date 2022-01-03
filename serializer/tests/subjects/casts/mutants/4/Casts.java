class Casts {
    String castString(Object o) {
        return (String)o;
    }

    boolean override(T t) {
        S s = (S) t;
        T t2 = (T) s;

        return s.overriddenMethod() < t.overriddenMethod()
            && t.overriddenMethod() == t2.overriddenMethod();
    }
}

class S {
    int overriddenMethod() {
        return 1;
    }
}

class T extends S {
    int overriddenMethod() {
        return 2;
    }
}
