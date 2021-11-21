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
    private Map<String, List<Node>> pathBetweenTargets;
    private List<List<Node>> bestPath;
    private Tuple<Integer, Integer> actualPos, lastPos;

    /**
     * Client Constructor
     * 
     * @param args command line arguments
     */
    public ClientC3(String[] args) {
        super(args, false);

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
        super.runState();

        // in goal position
        if (this.inGoalPos()) {
            this.actualPos = this.getActualPos();

            // on top of a target
            if (this.ground != -1)
                // found new target
                if (!this.grounds.containsKey(this.ground))
                    this.grounds.put(this.ground, this.actualPos);

            // if find all targets
            if (this.grounds.size() == this.nGrounds) {
                // change goal of agent
                this.changeGoal();

                // complete the goal
                if (this.findPathBetweenTargets()) {
                    this.changeState();

                    // find best path
                    this.getBestPath();
                }
            }
        }
    }

    @Override
    public void finishState() {
        System.out.println("Time: " + this.getCiberIF().GetTime());
        this.exportPath(this.getFilename());
    }

    public static void main(String[] args) {
        // create client
        Client client = new ClientC3(args);

        // main loop
        client.mainLoop();
    }

    /**
     * Find best paths between targets
     * 
     * @return true if find best paths between targets otherwise false
     */
    private boolean findPathBetweenTargets() {
        this.pathBetweenTargets = new HashMap<>();

        for (int i = 0; i < this.nGrounds; i++) {
            Node nodeI = new Node((MyMap.CELLROWS - 1) + this.grounds.get(i).y,
                    (MyMap.CELLCOLS - 1) + this.grounds.get(i).x);

            for (int j = i + 1; j < this.nGrounds; j++) {
                Node nodeJ = new Node((MyMap.CELLROWS - 1) + this.grounds.get(j).y,
                        (MyMap.CELLCOLS - 1) + this.grounds.get(j).x);

                // apply A*
                // look for possible better path
                AStar aStar = new AStar(MyMap.CELLROWS * 2 - 1, MyMap.CELLCOLS * 2 - 1, nodeI, nodeJ);
                aStar.setBlocks(this.getMyMap().getLabMap(), MyMap.BLOCKERS_WALLS);
                List<Node> bestSubPath = aStar.findPath(this.getLasMove());

                // look for better path
                aStar = new AStar(MyMap.CELLROWS * 2 - 1, MyMap.CELLCOLS * 2 - 1, nodeI, nodeJ);
                aStar.setBlocks(this.getMyMap().getLabMap(), MyMap.BLOCKERS_ALL);
                List<Node> path = aStar.findPath(this.getLasMove());

                // possible better path is small than better path founded
                if (bestSubPath.get(bestSubPath.size() - 1).getFCost() < path.get(path.size() - 1).getFCost()) {
                    System.out.println("There may be a better path between targets " + i + " and " + j);
                    this.pathBetweenTargets = null;

                    // check possible better path
                    this.checkOtherPath(bestSubPath);
                    return false;
                }
                this.pathBetweenTargets.put((i + "" + j), path);
            }
        }
        return true;
    }

    /**
     * Check possible best path between targets
     * 
     * @param bestSubPath possible best path between targets
     */
    private void checkOtherPath(List<Node> bestSubPath) {
        // lock for actual position on path
        boolean passLastPos = false;
        int i;
        for (i = 1; i < bestSubPath.size() - 1; i++) {
            Tuple<Integer, Integer> pos = this.convertNodeToTuple(bestSubPath.get(i));
            if (this.lastPos == null || this.actualPos.equals(pos))
                break;
            else if (this.lastPos != null && this.lastPos.equals(pos))
                passLastPos = true;
        }

        // get next position from path
        Tuple<Integer, Integer> nextPos;
        if (this.lastPos == null) {
            if (this.actualPos.equals(this.convertNodeToTuple(bestSubPath.get(0))))
                nextPos = this.convertNodeToTuple(bestSubPath.get(1));
            else
                nextPos = this.convertNodeToTuple(bestSubPath.get(bestSubPath.size() - 2));
        } else {
            if (passLastPos)
                nextPos = this.convertNodeToTuple(bestSubPath.get(i + 1));
            else
                nextPos = this.convertNodeToTuple(bestSubPath.get(i - 1));
        }
        this.lastPos = this.actualPos;

        // give to agent next position and move
        this.goToThisPos(nextPos, this.giveMoveGivenNextPos(nextPos));
    }

    /**
     * Find best path
     */
    private void getBestPath() {
        // get all possible permutations of path
        String targetsId = "";
        for (int i = 1; i < this.nGrounds; i++)
            targetsId += i;
        List<String> permutationsTargets = getAllPermutationsTargets(targetsId);

        // get path from all permutations
        for (int i = 0; i < permutationsTargets.size() - 1; i++) {
            List<List<Node>> path = new ArrayList<>();

            // add target 0 to beginning and end
            String seqTarget = "0" + permutationsTargets.get(i) + "0";

            for (int j = 0; j < seqTarget.length() - 1; j++) {
                int t0 = Character.getNumericValue(seqTarget.charAt(j));
                int t1 = Character.getNumericValue(seqTarget.charAt(j + 1));

                // get path between targets
                List<Node> subPath;
                if (t0 < t1)
                    subPath = this.pathBetweenTargets.get(t0 + "" + t1);
                else {
                    subPath = this.pathBetweenTargets.get(t1 + "" + t0);
                    Collections.reverse(subPath);
                }

                // remove target position duplicated
                if (j != 0)
                    subPath.remove(0);

                // actual path is worst than actual path
                if (this.bestPath != null && path.size() > this.bestPath.size())
                    break;
                path.add(subPath);
            }

            // if path founded is better than before
            if (this.bestPath == null || path.size() < this.bestPath.size())
                this.bestPath = path;
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
            File file = new File(fileName);

            // create file if does not exist
            if (file.createNewFile())
                System.out.println("File created: " + file.getName());

            if (this.bestPath != null) {
                // open file writer
                FileWriter fileWriter = new FileWriter(fileName);

                // loop all sub paths
                for (int i = 0; i < this.bestPath.size(); i++) {
                    List<Node> l = this.bestPath.get(i);

                    // loop for positions
                    for (int j = 0; j < l.size(); j++) {
                        Tuple<Integer, Integer> position = this.convertNodeToTuple(l.get(j));

                        // write the position
                        fileWriter.write(position.x + " " + position.y);

                        // if is the position is a target and not the target 0
                        if (i != this.bestPath.size() - 1 && j == l.size() - 1) {
                            for (int groundId = 1; groundId < this.grounds.size(); groundId++)
                                if (position.equals(this.grounds.get(groundId))) {
                                    fileWriter.write(" #" + groundId);
                                    break;
                                }
                        }
                        fileWriter.write("\n");
                    }
                }

                // close file writer
                fileWriter.close();
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            System.exit(1);
        }
    }

    /**
     * Give next move given next position
     * 
     * @param nextPos next position
     * @return next move
     */
    private Move giveMoveGivenNextPos(Tuple<Integer, Integer> nextPos) {
        if (this.actualPos.x + 2 == nextPos.x)
            return Move.RIGHT;
        else if (this.actualPos.x - 2 == nextPos.x)
            return Move.LEFT;
        else if (this.actualPos.y + 2 == nextPos.y)
            return Move.UP;
        else if (this.actualPos.y - 2 == nextPos.y)
            return Move.DOWN;
        return Move.NONE;
    }
}
