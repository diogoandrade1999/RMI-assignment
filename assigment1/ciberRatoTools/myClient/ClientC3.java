import java.io.*;
import java.util.*;

/**
 * Client for the third challenge
 * 
 */
public class ClientC3 extends ClientC2 {

    private final int nGrounds;
    private Map<Integer, Tuple<Integer, Integer>> grounds;
    private int ground;
    private List<List<Node>> bestPath;

    // Construtor
    public ClientC3(String[] args) {
        super(args);

        // read number of targets
        this.nGrounds = this.getCiberIF().GetNumberOfBeacons();
        this.grounds = new HashMap<>();
    }

    @Override
    public void readSensors() {
        super.readSensors();

        if (this.getCiberIF().IsGroundReady())
            this.ground = this.getCiberIF().GetGroundSensor();
    }

    @Override
    public void runState() {
        if (this.closeToNextPos() && this.getNextMove().equals(Move.NONE)) {
            // on top of a target
            if (this.ground != -1) {
                if (!this.grounds.containsKey(this.ground)) {
                    this.grounds.put(this.ground, this.getNextPos());
                }

                // complete the goal
                if (this.grounds.size() == this.nGrounds) {
                    this.changeState();

                    this.getBestPath(this.findPathBetweenTargets());
                }
            }
        }
        super.runState();
    }

    @Override
    public void finishState() {
        System.out.println("Time: " + this.getCiberIF().GetTime());
        this.exportPath("path.out");
    }

    public static void main(String[] args) {
        // create client
        Client client = new ClientC3(args);

        // main loop
        client.mainLoop();
    }

    /**
     * Find best paths between targets
     */
    private Map<String, List<Node>> findPathBetweenTargets() {
        Map<String, List<Node>> paths = new HashMap<>();
        for (int i = 0; i < this.nGrounds; i++) {
            Node nodeI = new Node((MyMap.CELLROWS - 1) + this.grounds.get(i).y,
                    (MyMap.CELLCOLS - 1) + this.grounds.get(i).x);

            for (int j = i + 1; j < this.nGrounds; j++) {
                Node nodeJ = new Node((MyMap.CELLROWS - 1) + this.grounds.get(j).y,
                        (MyMap.CELLCOLS - 1) + this.grounds.get(j).x);

                // apply A*
                AStar aStar = new AStar(MyMap.CELLROWS * 2 - 1, MyMap.CELLCOLS * 2 - 1, nodeI, nodeJ);
                aStar.setBlocks(this.getMyMap().getLabMap(), MyMap.BLOCKERS_WALLS);
                List<Node> bestPath = aStar.findPath();

                aStar = new AStar(MyMap.CELLROWS * 2 - 1, MyMap.CELLCOLS * 2 - 1, nodeI, nodeJ);
                aStar.setBlocks(this.getMyMap().getLabMap(), MyMap.BLOCKERS_ALL);
                List<Node> path = aStar.findPath();

                if (bestPath.size() < path.size())
                    System.out.println("Path between " + i + " and " + j + " is not the best!");
                paths.put((i + "" + j), path);
            }
        }
        return paths;
    }

    /**
     * Find best path
     * 
     * @param paths path between targets
     */
    private void getBestPath(Map<String, List<Node>> paths) {
        String targetsId = "";
        for (int i = 1; i < this.nGrounds; i++)
            targetsId += i;
        List<String> permutationsTargets = getAllPermutationsTargets(targetsId);
        int bestPathSize = -1;

        for (int i = 0; i < permutationsTargets.size() - 1; i++) {
            int pathSize = 0;
            List<List<Node>> path = new ArrayList<>();
            String seqTarget = "0" + permutationsTargets.get(i) + "0";

            for (int j = 0; j < seqTarget.length() - 1; j++) {
                int t0 = Character.getNumericValue(seqTarget.charAt(j));
                int t1 = Character.getNumericValue(seqTarget.charAt(j + 1));

                List<Node> subPath;
                if (t0 < t1)
                    subPath = paths.get(t0 + "" + t1);
                else {
                    subPath = paths.get(t1 + "" + t0);
                    Collections.reverse(subPath);
                }

                if (j != seqTarget.length() - 2)
                    subPath.remove(subPath.size() - 1);

                pathSize += subPath.size();
                if (bestPathSize != -1 && pathSize > bestPathSize)
                    break;
                path.add(subPath);
            }

            if (bestPathSize == -1 || path.size() < bestPathSize) {
                bestPathSize = path.size();
                this.bestPath = path;
            }
        }
    }

    /**
     * Find permutations of targets
     * 
     * @param targetsId all targets ids
     * @return permutations of targets
     */
    private static List<String> getAllPermutationsTargets(String targetsId) {
        if (targetsId.length() == 0) {
            List<String> empty = new ArrayList<>();
            empty.add("");
            return empty;
        }

        char firtsTarget = targetsId.charAt(0);
        String subTargetsId = targetsId.substring(1);
        List<String> prevResult = getAllPermutationsTargets(subTargetsId);
        List<String> result = new ArrayList<>();
        for (String target : prevResult)
            for (int i = 0; i <= target.length(); i++)
                result.add(target.substring(0, i) + firtsTarget + target.substring(i));
        return result;
    }

    /**
     * Export best path to file
     */
    private void exportPath(String fileName) {
        try {
            File myObj = new File(fileName);
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            }
            FileWriter myWriter = new FileWriter(fileName);
            String extra = "";
            for (int i = 0; i < this.nGrounds; i++) {
                if (i > 0)
                    extra = " #" + i;
                for (Node node : this.bestPath.get(i)) {
                    Tuple<Integer, Integer> position = this.convertNodeToTuple(node);
                    myWriter.write(position.x + " " + position.y + extra + "\n");
                    extra = "";
                }
            }
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            System.exit(1);
        }
    }

}
