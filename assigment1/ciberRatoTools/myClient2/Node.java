
/**
 * Node Class
 *
 */
public class Node {

    private int gCost;
    private int fCost;
    private int hCost;
    private int row;
    private int col;
    private boolean isBlock;
    private Node parent;

    public Node(int row, int col) {
        this.row = row;
        this.col = col;
        this.isBlock = false;
    }

    public int getHCost() {
        return this.hCost;
    }

    public int getGCost() {
        return this.gCost;
    }

    public int getFCost() {
        return this.fCost;
    }

    public Node getParent() {
        return parent;
    }

    public boolean isBlock() {
        return isBlock;
    }

    public void setBlock(boolean isBlock) {
        this.isBlock = isBlock;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void calculateHeuristic(Node finalNode) {
        this.hCost = Math.abs(finalNode.getRow() - this.row) + Math.abs(finalNode.getCol() - this.col);
    }

    public void setNodeData(Node currentNode, int cost) {
        this.gCost = currentNode.getGCost() + cost;
        this.parent = currentNode;
        this.fCost = this.gCost + this.hCost;
    }

    public boolean checkBetterPath(Node currentNode, int cost) {
        int gCost = currentNode.getGCost() + cost;
        if (gCost < this.gCost) {
            setNodeData(currentNode, cost);
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        Node node = (Node) o;
        return this.getRow() == node.getRow() && this.getCol() == node.getCol();
    }

    @Override
    public String toString() {
        return "Node [col=" + col + ", row=" + row + "]";
    }
}
