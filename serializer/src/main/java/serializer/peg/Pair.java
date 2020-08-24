package serializer.peg;

public class Pair<U, V> {
    public final U fst;
    public final V snd;
    public Pair(U f, V s) {
        fst = f;
        snd = s;
    }

    public static <U,V> Pair<U,V> of(U f, V s) {
        return new Pair<>(f,s);
    }
}
