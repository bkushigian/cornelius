import java.util.List;
import java.util.ArrayList;

public class MethodInvocation {

    /**
     * This method does nothing
     */
    public void doNothing() {
        return;
    }

    /**
     * The identity function on primitive ints
     */
    public int identity(int x) {
        return x;
    }

    /**
     * add two numbers
     */
    public int add(int a, int b) {
        return a + b;
    }

    /**
     * A poorly named method that invokes another method
     */
    public int invoke(int a, int b) {
        return add(a, b) + add(a, b);
    }

    /**
     * The identity function on Integers
     */
    public Integer identity(Integer x) {
        return x;
    }

    /**
     * Create a small {@code List} with two elements
     */
    public List<Integer> aSmallList(int a, int b) {
        List<Integer> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        return list;
    }

    /**
     * This method is equivalent to {@code identity} (plus an extra parameter c)
     */
    public int branching(boolean c, int x) {
        if (c) {
            return identity(x);
        } else {
            return identity(x);
        }
    }

    /**
     * This is like {@code branching} but adds in an extra call to {@code
     * doNothing()}. This is equivalent but cornelious won't be able to tell
     * that it's equivalent until there is either purity analysis or some sort
     * of inlining.
     */
    public int branchingWithInvocation1(boolean c, int x) {
        if (c) {
            return identity(x);
        } else {
            doNothing();
            return identity(x);
        }
    }

    /**
     * This is like {@code branching} but adds in an extra call to {@code
     * aSmallList(1,2)}. This is <i>technically</i> equivalent since it creates
     * a new list that will be immediately garbage collected. However, Cornelius
     * won't be able to detect this equivalence since I'm not trying to model
     * garbage collection.
     */
    public int branchingWithInvocation2(boolean c, int x) {
        if (c) {
            return identity(x);
        } else {
            aSmallList(1,2);
            return identity(x);
        }
    }

    public int x = 1;

    public int updateField(int a) {
        int b = branchingWithInvocation2(true, x);
        int c = branchingWithInvocation2(true, x);
        ;
        return c + c;
    }
}
