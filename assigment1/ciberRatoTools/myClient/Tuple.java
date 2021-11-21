
/**
 * Tuple class
 */
public class Tuple<X, Y> {

    public final X x;
    public final Y y;

    /**
     * Tuple Constructor
     * 
     * @param x
     * @param y
     */
    public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        Tuple<Integer, Integer> other = (Tuple<Integer, Integer>) obj;
        return this.x == other.x && this.y == other.y;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
