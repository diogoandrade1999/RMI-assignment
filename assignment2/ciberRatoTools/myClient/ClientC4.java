import java.io.*;
import java.util.*;

/**
 * Client for the fourth challenge
 * 
 */
public class ClientC4 extends Client {

    private static final double[] SENSORS_ANGLES = { 0, 90, -90, 180 };
    private static final double THRESHHOLD_SENSORS = 1.1;
    private final int nGrounds;
    private double irSensor0, irSensor1, irSensor2, irSensor3, compass, probPos;
    private int actualPosX, actualPosY, nextPosX, nextPosY, ground;
    private Move actualMove, nextMove, lastMove;
    private Stack<Tuple<Integer, Integer>> posToView;
    private Queue<Tuple<Integer, Integer>> listNextPos;
    private MyMap myMap;
    private boolean discoverMapGoal;
    private Map<Integer, Tuple<Integer, Integer>> grounds;
    private Map<String, List<Node>> pathBetweenTargets;
    private List<List<Node>> bestPath;
    private Tuple<Integer, Integer> actualPos, lastPos;

    /**
     * Client Constructor
     * 
     * @param args command line arguments
     */
    public ClientC4(String[] args) {
        super(args, SENSORS_ANGLES);

        // read first position
        do {
            this.getCiberIF().ReadSensors();
        } while (!this.getCiberIF().IsGPSReady());

        this.probPos = 1;
        this.actualPosX = 0;
        this.actualPosY = 0;
        this.nextPosX = 0;
        this.nextPosY = 0;
        this.actualMove = Move.NONE;
        this.nextMove = Move.NONE;
        this.lastMove = Move.NONE;
        this.posToView = new Stack<>();
        this.listNextPos = new LinkedList<>();
        this.myMap = new MyMap();
        this.discoverMapGoal = true;

        // read number of targets
        this.nGrounds = this.getCiberIF().GetNumberOfBeacons();
        this.grounds = new HashMap<>();
    }

    @Override
    public void wander() {
        double rot = Math.toRadians(this.getAngle());
        if (rot > 0)
            this.getCiberIF().DriveMotors(0.15 - rot, 0.15);
        else if (rot < 0)
            this.getCiberIF().DriveMotors(0.15, 0.15 + rot);
        else
            this.getCiberIF().DriveMotors(+0.15, +0.15);
    }

    @Override
    public void readSensors() {
        this.getCiberIF().ReadSensors();

        if (this.getCiberIF().IsObstacleReady(0))
            this.irSensor0 = this.getCiberIF().GetObstacleSensor(0);
        if (this.getCiberIF().IsObstacleReady(1))
            this.irSensor1 = this.getCiberIF().GetObstacleSensor(1);
        if (this.getCiberIF().IsObstacleReady(2))
            this.irSensor2 = this.getCiberIF().GetObstacleSensor(2);
        if (this.getCiberIF().IsObstacleReady(3))
            this.irSensor3 = this.getCiberIF().GetObstacleSensor(3);

        if (this.getCiberIF().IsCompassReady())
            this.compass = this.getCiberIF().GetCompassSensor();

        if (this.getCiberIF().IsGroundReady())
            this.ground = this.getCiberIF().GetGroundSensor();
    }

    @Override
    public void runState() {
        // in goal position
        if (this.closeToNextPos()) {
            // moving in an unexplored position on the map
            if (this.listNextPos.isEmpty()) {
                // explore map
                this.discoverMap();

                // if does not find any possible move
                if (this.discoverMapGoal && this.actualMove.equals(Move.NONE)) {
                    // complete goal
                    if (this.posToView.isEmpty())
                        this.changeState();
                    else {
                        // check position left behind
                        this.searchNextPosShortPath();

                        // complete goal
                        if (this.listNextPos.isEmpty())
                            this.changeState();
                    }
                }
            }

            // moving in a explored position on the map
            if (!this.listNextPos.isEmpty())
                this.readNextMoveFromList();

            // on top of a target
            if (this.ground != -1)
                // found new target
                if (!this.grounds.containsKey(this.ground))
                    this.grounds.put(this.ground, this.actualPos);

            // if find all targets
            if (this.grounds.size() == this.nGrounds) {
                // change goal of agent
                this.discoverMapGoal = false;

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
        this.myMap.exportMap(this.getFilename() + ".map");
        this.exportPath(this.getFilename() + ".path");
    }

    public static void main(String[] args) {
        // create client
        Client client = new ClientC4(args);

        // main loop
        client.mainLoop();
    }

    /**
     * Gives the agent's angle of deviation
     * 
     * @return angle of deviation
     */
    private double getAngle() {
        double angle = 0;

        switch (this.actualMove) {
            case UP:
                angle = 90 - this.compass;
                break;
            case DOWN:
                angle = -90 - this.compass;
                break;
            case LEFT:
                if (this.compass < 0)
                    angle = -180 - this.compass;
                else
                    angle = 180 - this.compass;
                break;
            case RIGHT:
                angle = 0 - this.compass;
                break;
            default:
                angle = this.compass;
                break;
        }
        if (260 < angle && angle < 280)
            angle -= 360;
        if (-280 < angle && angle < -260)
            angle += 360;
        return angle;
    }

    /**
     * Verify if agent is in the intended position
     * 
     * @return true if the agent is in the intended position, otherwise false
     */
    private boolean closeToNextPos() {
        double angle = this.getAngle();
        System.out.println(angle);
        if ((0.7 < this.probPos && this.probPos <= 1) || (this.irSensor0 >= 3.3 && (-3 < angle && angle < 3))) {
            this.actualPosX = this.nextPosX;
            this.actualPosY = this.nextPosY;
            this.actualPos = new Tuple<>(this.actualPosX, this.actualPosY);
            return true;
        }
        return false;
    }

    /**
     * Check if the positions of the sides are obstacles or free path
     */
    private void discoverMap() {
        double[] irSensorsRight = { irSensor0, irSensor2, irSensor1, irSensor3 };
        double[] irSensorsUp = { irSensor1, irSensor0, irSensor3, irSensor2 };
        double[] irSensorsDown = { irSensor2, irSensor3, irSensor0, irSensor1 };
        double[] irSensorsLeft = { irSensor3, irSensor1, irSensor2, irSensor0 };

        // check obstacle in (x+1, y)
        this.checkObstacle(irSensorsRight, Move.RIGHT);
        // check obstacle in (x, y+1)
        this.checkObstacle(irSensorsUp, Move.UP);
        // check obstacle in (x, y-1)
        this.checkObstacle(irSensorsDown, Move.DOWN);
        // check obstacle in (x-1, y)
        this.checkObstacle(irSensorsLeft, Move.LEFT);

        // continue to do the same move if possible
        // get next move
        switch (this.actualMove) {
            case UP:
                this.checkFree(Move.UP, 0, +2);
                this.checkFree(Move.RIGHT, +2, 0);
                this.checkFree(Move.LEFT, -2, 0);
                break;
            case DOWN:
                this.checkFree(Move.DOWN, 0, -2);
                this.checkFree(Move.RIGHT, +2, 0);
                this.checkFree(Move.LEFT, -2, 0);
                break;
            case LEFT:
                this.checkFree(Move.LEFT, -2, 0);
                this.checkFree(Move.UP, 0, +2);
                this.checkFree(Move.DOWN, 0, -2);
                break;
            case RIGHT:
                this.checkFree(Move.RIGHT, +2, 0);
                this.checkFree(Move.UP, 0, +2);
                this.checkFree(Move.DOWN, 0, -2);
            default:
                this.checkFree(Move.RIGHT, +2, 0);
                this.checkFree(Move.UP, 0, +2);
                this.checkFree(Move.DOWN, 0, -2);
                this.checkFree(Move.LEFT, -2, 0);
                break;
        }

        // pass next move to actual move
        this.lastMove = this.actualMove;
        this.actualMove = this.nextMove;
        this.nextMove = Move.NONE;
    }

    /**
     * Check if the positions of the sides are obstacles
     * 
     * @param irSensors sensors values
     * @param move      side positons
     */
    private void checkObstacle(double[] irSensors, Move move) {
        if ((-3 < this.compass && this.compass < 3 && irSensors[0] >= THRESHHOLD_SENSORS)
                || (87 < this.compass && this.compass < 93 && irSensors[1] >= THRESHHOLD_SENSORS)
                || (-93 < this.compass && this.compass < -87 && irSensors[2] >= THRESHHOLD_SENSORS)
                || ((-177 > this.compass || this.compass > 177) && irSensors[3] >= THRESHHOLD_SENSORS))
            this.myMap.addObstacle(this.actualPosX, this.actualPosY, move);
    }

    /**
     * Check if the positions of the sides are free path
     * 
     * @param move  side positons
     * @param plusX value of x to add to actual x position
     * @param plusY value of y to add to actual y position
     */
    private void checkFree(Move move, int plusX, int plusY) {
        if (!this.myMap.checkObstaclePos(this.actualPosX, this.actualPosY, move)) {
            if (!this.myMap.checkFreePos(this.actualPosX, this.actualPosY, move))
                if (this.nextMove.equals(Move.NONE)) {
                    this.nextMove = move;
                    this.nextPosX += plusX;
                    this.nextPosY += plusY;
                } else
                    this.posToView.add(new Tuple<>(this.actualPosX + plusX, this.actualPosY + plusY));
            this.myMap.addFree(this.actualPosX, this.actualPosY, move);
        }
    }

    /**
     * Search for a position left begind using A* algorithm sorted by shortest path
     * from actual position
     */
    private void searchNextPosShortPath() {
        List<Node> choosedPath = null;
        Tuple<Integer, Integer> choosedPos = null;
        Stack<Tuple<Integer, Integer>> newPosToView = new Stack<>();

        // actual position
        Node initialNode = new Node((MyMap.CELLROWS - 1) + this.actualPosY, (MyMap.CELLCOLS - 1) + this.actualPosX);

        for (int i = 0; i < this.posToView.size(); i++) {
            // position left behind
            Tuple<Integer, Integer> pos = this.posToView.get(i);

            // check if position was verified by another path
            if (!pos.equals(this.actualPos)) {
                // goal position
                Node finalNode = new Node((MyMap.CELLROWS - 1) + pos.y, (MyMap.CELLCOLS - 1) + pos.x);

                // apply A*
                AStar aStar = new AStar(MyMap.CELLROWS * 2 - 1, MyMap.CELLCOLS * 2 - 1, initialNode, finalNode);
                aStar.setBlocks(this.myMap.getLabMap(), MyMap.BLOCKERS_ALL);
                List<Node> path = aStar.findPath(this.lastMove);

                if (path.size() > 0) {
                    // if this position has the close path from actual position
                    if (choosedPath == null || path.get(path.size() - 1).getFCost() < choosedPath
                            .get(choosedPath.size() - 1).getFCost()) {
                        if (choosedPos != null)
                            newPosToView.add(choosedPos);
                        choosedPath = path;
                        choosedPos = pos;
                    }
                    // otherwise check later
                    else if (path.get(path.size() - 1).getFCost() >= choosedPath.get(choosedPath.size() - 1).getFCost())
                        newPosToView.add(pos);
                }
            }
        }

        // if find position to check
        if (choosedPath != null) {
            this.posToView = newPosToView;
            for (int i = 1; i < choosedPath.size(); i++)
                this.listNextPos.add(this.convertNodeToTuple(choosedPath.get(i)));
        }
    }

    /**
     * Read next move from list
     */
    private void readNextMoveFromList() {
        Tuple<Integer, Integer> pos = this.listNextPos.poll();
        if (this.actualPosX + 2 == pos.x)
            this.actualMove = Move.RIGHT;
        else if (this.actualPosX - 2 == pos.x)
            this.actualMove = Move.LEFT;
        else if (this.actualPosY + 2 == pos.y)
            this.actualMove = Move.UP;
        else if (this.actualPosY - 2 == pos.y)
            this.actualMove = Move.DOWN;
        this.nextPosX = pos.x;
        this.nextPosY = pos.y;
    }

    /**
     * Convert node position in tuple position
     * 
     * @param node node position
     * @return tuple position
     */
    public Tuple<Integer, Integer> convertNodeToTuple(Node node) {
        return new Tuple<>(node.getCol() - (MyMap.CELLCOLS - 1), node.getRow() - (MyMap.CELLROWS - 1));
    }

    /**
     * Go to this position
     * 
     * @param position next position
     * @param move     actual move
     */
    public void goToThisPos(Tuple<Integer, Integer> position, Move move) {
        this.nextPosX = position.x;
        this.nextPosY = position.y;
        this.actualMove = move;
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
                aStar.setBlocks(this.myMap.getLabMap(), MyMap.BLOCKERS_WALLS);
                List<Node> bestSubPath = aStar.findPath(this.lastMove);

                // look for better path
                aStar = new AStar(MyMap.CELLROWS * 2 - 1, MyMap.CELLCOLS * 2 - 1, nodeI, nodeJ);
                aStar.setBlocks(this.myMap.getLabMap(), MyMap.BLOCKERS_ALL);
                List<Node> path = aStar.findPath(this.lastMove);

                // possible better path is small than better path founded
                // if (bestSubPath.get(bestSubPath.size() - 1).getFCost() < path.get(path.size()
                // - 1).getFCost()) {
                if (bestSubPath.size() < path.size()) {
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

        // int bestPathCost = 0;
        int bestPathSize = 0;

        // get path from all permutations
        for (int i = 0; i < permutationsTargets.size() - 1; i++) {
            List<List<Node>> path = new ArrayList<>();
            // int pathCost = 0;
            int pathSize = 0;

            // add target 0 to beginning and end
            String seqTarget = "0" + permutationsTargets.get(i) + "0";

            for (int j = 0; j < seqTarget.length() - 1; j++) {
                int t0 = Character.getNumericValue(seqTarget.charAt(j));
                int t1 = Character.getNumericValue(seqTarget.charAt(j + 1));

                // get path between targets
                List<Node> subPath;
                if (t0 < t1)
                    subPath = new ArrayList<>(this.pathBetweenTargets.get(t0 + "" + t1));
                else {
                    subPath = new ArrayList<>(this.pathBetweenTargets.get(t1 + "" + t0));
                    Collections.reverse(subPath);
                }

                // remove target position duplicated
                if (j != 0)
                    subPath.remove(0);

                // add sub path cost
                // pathCost += subPath.get(subPath.size() - 1).getFCost();

                // add sub path size
                pathSize += subPath.size();

                // actual path is worst than actual path
                // if (this.bestPath != null && pathCost > bestPathCost)
                if (this.bestPath != null && pathSize > bestPathSize)
                    break;
                path.add(subPath);
            }

            // if path founded is better than before
            // if (this.bestPath == null || pathCost < bestPathCost) {
            if (this.bestPath == null || pathSize < bestPathSize) {
                this.bestPath = path;
                // bestPathCost = pathCost;
                bestPathSize = pathSize;
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
