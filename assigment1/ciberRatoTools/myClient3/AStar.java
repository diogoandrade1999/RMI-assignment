import java.util.*;

/**
 * A Star Algorithm
 * 
 */
public class AStar {

    private static int DEFAULT_COST = 10;
    private int cost;
    private Node[][] searchArea;
    private PriorityQueue<Node> openList;
    private Set<Node> closedSet;
    private Node initialNode;
    private Node finalNode;

    public AStar(int rows, int cols, Node initialNode, Node finalNode) {
        this.cost = DEFAULT_COST;
        this.initialNode = initialNode;
        this.finalNode = finalNode;
        this.searchArea = new Node[rows][cols];
        this.openList = new PriorityQueue<Node>(new Comparator<Node>() {
            @Override
            public int compare(Node node0, Node node1) {
                return Integer.compare(node0.getFCost(), node1.getFCost());
            }
        });
        this.closedSet = new HashSet<>();
        this.createNodes(rows, cols);
    }

    private void createNodes(int rows, int cols) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (i % 2 != 0 || j % 2 != 0) {
                    Node node = new Node(i, j);
                    node.calculateHeuristic(this.finalNode);
                    this.searchArea[i][j] = node;
                }
            }
        }
    }

    public void setBlocks(char[][] map) {
        for (int row = map.length - 1; row >= 0; row--)
            for (int col = 0; col < map[row].length; col++)
                if (row % 2 != 0 || col % 2 != 0)
                    if (map[row][col] != 'X')
                        this.searchArea[row][col].setBlock(true);
    }

    public List<Node> findPath() {
        this.openList.add(initialNode);
        while (!this.openList.isEmpty()) {
            Node currentNode = this.openList.poll();
            this.closedSet.add(currentNode);
            if (currentNode.equals(this.finalNode))
                return getPath(currentNode);
            else
                addAdjacentNodes(currentNode);
        }
        return new ArrayList<Node>();
    }

    private List<Node> getPath(Node currentNode) {
        List<Node> path = new ArrayList<Node>();
        path.add(currentNode);
        Node parent;
        while ((parent = currentNode.getParent()) != null) {
            path.add(0, parent);
            currentNode = parent;
        }
        return path;
    }

    private void addAdjacentNodes(Node currentNode) {
        int row = currentNode.getRow();
        int col = currentNode.getCol();
        if (row - 2 >= 0)
            checkNode(currentNode, this.searchArea[row - 1][col], this.searchArea[row - 2][col]);
        if (col - 2 >= 0)
            checkNode(currentNode, this.searchArea[row][col - 1], this.searchArea[row][col - 2]);
        if (col + 2 < this.searchArea[0].length)
            checkNode(currentNode, this.searchArea[row][col + 1], this.searchArea[row][col + 2]);
        if (row + 2 < this.searchArea.length)
            checkNode(currentNode, this.searchArea[row + 1][col], this.searchArea[row + 2][col]);
    }

    private void checkNode(Node currentNode, Node adjacentNode, Node nextNode) {
        if (!adjacentNode.isBlock() && !this.closedSet.contains(nextNode)) {
            if (!this.openList.contains(nextNode)) {
                nextNode.setNodeData(currentNode, this.cost);
                this.openList.add(nextNode);
            } else {
                if (nextNode.checkBetterPath(currentNode, this.cost)) {
                    // Remove and Add the changed node, so that the PriorityQueue can sort again its
                    // contents with the modified "finalCost" value of the modified node
                    this.openList.remove(nextNode);
                    this.openList.add(nextNode);
                }
            }
        }
    }
}
