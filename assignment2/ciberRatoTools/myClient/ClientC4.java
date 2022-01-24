import java.io.*;
import java.util.*;

/**
 * Client for the fourth challenge
 * 
 */
public class ClientC4 extends Client {

    private static final double[] SENSORS_ANGLES = { 0, 90, -90, 180 };
    private static final double[][] ANGLE_VARIATION = { { -10, 10 }, { -100, -80 }, { 80, 100 }, { -170, 170 } };
    private static final double THRESHHOLD_SENSORS = 1.1;
    private static final double WALL_SIZE = 0.1;
    private static final double RADIUS = 0.5;
    private static final double KP = 0.00065; // use 0.007 for MAX POWER 0.1
    private static final double KP2 = 0.2; // use 0.5 for MAX POWER 0.1
    private static final double MAX_POWER = 0.15;
    private static final double MIN_POWER = -0.15;
    private final int nGrounds;
    private double irSensor0, irSensor1, irSensor2, irSensor3, compass, outL, outR, probX, probY, probAngle;
    private int actualPosX, actualPosY, nextPosX, nextPosY, ground;
    private Move actualMove, nextMove, lastMove;
    private Stack<Tuple<Integer, Integer>> posToView;
    private Queue<Tuple<Integer, Integer>> listNextPos;
    private MyMap myMap;
    private Map<Integer, Tuple<Integer, Integer>> grounds;
    private Map<String, List<Node>> pathBetweenTargets;
    private List<List<Node>> bestPath;
    private Tuple<Integer, Integer> actualPos, lastPos;
    private boolean allowRotate, inRotation, goInitPos;

    /**
     * Client Constructor
     * 
     * @param args command line arguments
     */
    public ClientC4(String[] args) {
        super(args, SENSORS_ANGLES);

        this.outL = 0;
        this.outR = 0;
        this.probX = 0;
        this.probY = 0;
        this.probAngle = 0;
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
        this.allowRotate = true;
        this.inRotation = false;
        this.goInitPos = false;

        // read number of targets
        this.nGrounds = this.getCiberIF().GetNumberOfBeacons();
        this.grounds = new HashMap<>();
    }

    @Override
    public void wander() {
        double lPow = 0, rPow = 0;

        // change diretion
        if (this.allowRotate && !this.actualMove.equals(this.lastMove) && !this.inRotation) {
            this.inRotation = true;
            this.allowRotate = false;
        }

        // in rotation
        if (inRotation) {
            if (!this.inGoodAngle()) {
                double rot = Math.toRadians(this.getAngle());
                double needIn = rot - ((this.outR - this.outL) / 2);
                double power = needIn;

                // limit power to max
                if (power > MAX_POWER)
                    power = MAX_POWER;
                else if (power < MIN_POWER)
                    power = MIN_POWER;

                // increase power when is close to goal angle
                if (-0.8 < needIn && needIn < 0.8)
                    power *= KP2;

                lPow = -power;
                rPow = power;
            } else
                inRotation = false;
        } else {
            double rot = Double.NaN, rot1 = Double.NaN, rot2 = Double.NaN;

            // check wall left side
            if (this.irSensor1 >= THRESHHOLD_SENSORS)
                rot1 = KP * (this.irSensor1 - 2.5);
            // check wall right side
            if (this.irSensor2 >= THRESHHOLD_SENSORS)
                rot2 = KP * (this.irSensor2 - 2.5);

            // calcule rot
            if (!Double.isNaN(rot1) && !Double.isNaN(rot2))
                if (rot1 >= 0 && rot2 <= 0)
                    rot = (rot1 - rot2) / 2;
                else if (rot1 <= 0 && rot2 >= 0)
                    rot = (rot1 - rot2) / 2;
                else
                    rot = (rot1 + rot2) / 2;

            // ajust wheels power
            lPow = MAX_POWER;
            rPow = MAX_POWER;
            if (!Double.isNaN(rot))
                if (rot > 0)
                    rPow -= 2 * rot;
                else
                    lPow += 2 * rot;

            // add code for agent dont go to far from center of cell
            if (this.actualMove.equals(Move.RIGHT) || this.actualMove.equals(Move.LEFT)) {
                double needLinX = (this.nextPosX - this.probX) / Math.cos(this.probAngle);
                double needIn = (4 * needLinX) - (this.outR + this.outR);
                double gettedIn = rPow + lPow;
                if (needIn < gettedIn) {
                    lPow = (lPow * needIn) / gettedIn;
                    rPow = (rPow * needIn) / gettedIn;
                }
            } else if (this.actualMove.equals(Move.UP) || this.actualMove.equals(Move.DOWN)) {
                double needLinY = (this.nextPosY - this.probY) / Math.sin(this.probAngle);
                double needIn = (4 * needLinY) - (this.outR + this.outR);
                double gettedIn = rPow + lPow;
                if (needIn < gettedIn) {
                    lPow = (lPow * needIn) / gettedIn;
                    rPow = (rPow * needIn) / gettedIn;
                }
            }
        }

        // give power to motors
        this.getCiberIF().DriveMotors(lPow, rPow);

        // update pose estimate
        this.updateEstimate(lPow, rPow);
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
                if (this.actualMove.equals(Move.NONE)) {
                    // complete goal
                    if (this.posToView.isEmpty()) {
                        this.goInitPos = true;
                        this.posToView.add(new Tuple<Integer, Integer>(0, 0));

                        // go to initial postion
                        this.searchNextPosShortPath();

                        // complete goal
                        if (this.listNextPos.isEmpty())
                            this.changeState();
                    } else {
                        // check position left behind
                        this.searchNextPosShortPath();

                        // complete goal
                        if (this.listNextPos.isEmpty()) {
                            this.goInitPos = true;
                            this.posToView.add(new Tuple<Integer, Integer>(0, 0));

                            // go to initial postion
                            this.searchNextPosShortPath();

                            // complete goal
                            if (this.listNextPos.isEmpty())
                                this.changeState();
                        }
                    }
                }
            }

            // moving in a explored position on the map
            if (!this.listNextPos.isEmpty())
                this.readNextMoveFromList();

            // on top of a target
            if (this.ground != -1)
                // found new target
                if (!this.grounds.containsKey(this.ground)) {
                    this.grounds.put(this.ground, this.actualPos);
                    this.myMap.addTargets(this.actualPos.x, this.actualPos.y, this.ground);
                }

            // backup draw map
            this.myMap.exportMap(this.getFilename() + ".map");
        }
    }

    @Override
    public void finishState() {
        // check if found all grounds
        if (this.nGrounds != this.grounds.size())
            System.out.println("Number of grounds not founded: " + (this.nGrounds - this.grounds.size()));

        // find best path
        if (this.findPathBetweenTargets())
            this.getBestPath();

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
     * Check if the angle is accurate
     * 
     * @return true if angle is accurate otherwise false
     */
    private boolean inGoodAngle() {
        double angle = Math.toDegrees(this.probAngle);
        if ((-1.5 < angle && angle < 1.5 && this.actualMove.equals(Move.RIGHT))
                || (88.5 < angle && angle < 91.5 && this.actualMove.equals(Move.UP))
                || (-91.5 < angle && angle < -88.5 && this.actualMove.equals(Move.DOWN))
                || ((-178.5 > angle || angle > 178.5) && this.actualMove.equals(Move.LEFT)))
            return true;
        return false;
    }

    /**
     * Localization estimate
     * 
     * @param inL left wheel power
     * @param inR right wheel power
     */
    private void updateEstimate(double inL, double inR) {
        double valY0 = Double.NaN, valY1 = Double.NaN, valX0 = Double.NaN, valX1 = Double.NaN;

        // inverse of senseros read
        double cosDeviationAngle = Math.abs(Math.cos(Math.toRadians(this.getAngle())));
        double invIrSensor0 = cosDeviationAngle / irSensor0;
        double invIrSensor1 = cosDeviationAngle / irSensor1;
        double invIrSensor2 = cosDeviationAngle / irSensor2;
        double invIrSensor3 = cosDeviationAngle / irSensor3;

        // check if exist walls
        double angle = Math.toDegrees(this.probAngle);
        if (ANGLE_VARIATION[0][0] < angle && angle < ANGLE_VARIATION[0][1]) {
            if (this.irSensor1 >= THRESHHOLD_SENSORS)
                valY0 = (this.actualPosY + 1) - invIrSensor1 - WALL_SIZE - RADIUS;
            if (this.irSensor2 >= THRESHHOLD_SENSORS)
                valY1 = (this.actualPosY - 1) + invIrSensor2 + WALL_SIZE + RADIUS;
            if (this.irSensor0 >= THRESHHOLD_SENSORS + 0.3)
                valX0 = (this.nextPosX + 1) - invIrSensor0 - WALL_SIZE - RADIUS;
            if (this.irSensor3 >= THRESHHOLD_SENSORS + 0.3)
                valX1 = (this.actualPosX - 1) + invIrSensor3 + WALL_SIZE + RADIUS;
        } else if (ANGLE_VARIATION[3][0] > angle || angle > ANGLE_VARIATION[3][1]) {
            if (this.irSensor1 >= THRESHHOLD_SENSORS)
                valY0 = (this.actualPosY - 1) + invIrSensor1 + WALL_SIZE + RADIUS;
            if (this.irSensor2 >= THRESHHOLD_SENSORS)
                valY1 = (this.actualPosY + 1) - invIrSensor2 - WALL_SIZE - RADIUS;
            if (this.irSensor0 >= THRESHHOLD_SENSORS + 0.3)
                valX0 = (this.nextPosX - 1) + invIrSensor0 + WALL_SIZE + RADIUS;
            if (this.irSensor3 >= THRESHHOLD_SENSORS + 0.3)
                valX1 = (this.actualPosX + 1) - invIrSensor3 - WALL_SIZE - RADIUS;
        } else if (ANGLE_VARIATION[1][0] < angle && angle < ANGLE_VARIATION[1][1]) {
            if (this.irSensor1 >= THRESHHOLD_SENSORS)
                valX0 = (this.actualPosX + 1) - invIrSensor1 - WALL_SIZE - RADIUS;
            if (this.irSensor2 >= THRESHHOLD_SENSORS)
                valX1 = (this.actualPosX - 1) + invIrSensor2 + WALL_SIZE + RADIUS;
            if (this.irSensor0 >= THRESHHOLD_SENSORS + 0.3)
                valY0 = (this.nextPosY - 1) + invIrSensor0 + WALL_SIZE + RADIUS;
            if (this.irSensor3 >= THRESHHOLD_SENSORS + 0.3)
                valY1 = (this.actualPosY + 1) - invIrSensor3 - WALL_SIZE - RADIUS;
        } else if (ANGLE_VARIATION[2][0] < angle && angle < ANGLE_VARIATION[2][1]) {
            if (this.irSensor1 >= THRESHHOLD_SENSORS)
                valX0 = (this.actualPosX - 1) + invIrSensor1 + WALL_SIZE + RADIUS;
            if (this.irSensor2 >= THRESHHOLD_SENSORS)
                valX1 = (this.actualPosX + 1) - invIrSensor2 - WALL_SIZE - RADIUS;
            if (this.irSensor0 >= THRESHHOLD_SENSORS + 0.3)
                valY0 = (this.nextPosY + 1) - invIrSensor0 - WALL_SIZE - RADIUS;
            if (this.irSensor3 >= THRESHHOLD_SENSORS + 0.3)
                valY1 = (this.actualPosY - 1) + invIrSensor3 + WALL_SIZE + RADIUS;
        }

        // calcule out
        this.outL = this.getOut(this.outL, inL);
        this.outR = this.getOut(this.outR, inR);

        // calcule new estimate of pose
        this.probX = this.getProbX();
        this.probY = this.getProbY();
        this.probAngle = this.getProbAngle();

        // System.out.print(this.probX + " " + this.probY);

        if (this.actualMove.equals(Move.RIGHT) || this.actualMove.equals(Move.LEFT)) {
            // ajust estimate of X position
            if (!Double.isNaN(valX0) && (this.probX - 1 < valX0 && valX0 < this.probX + 1))
                this.probX = (this.probX * 3 + valX0) / 4;
            else if (!Double.isNaN(valX1) && (this.probX - 1 < valX1 && valX1 < this.probX + 1))
                this.probX = (this.probX * 3 + valX1) / 4;

            // ajust estimate of Y position
            if (!Double.isNaN(valY0) && !Double.isNaN(valY1))
                this.probY = (this.probY * 2 + ((valY0 + valY1) / 2)) / 3;
        } else if (this.actualMove.equals(Move.UP) || this.actualMove.equals(Move.DOWN)) {
            // ajust estimate of X position
            if (!Double.isNaN(valX0) && !Double.isNaN(valX1))
                this.probX = (this.probX * 2 + ((valX0 + valX1) / 2)) / 3;

            // ajust estimate of Y position
            if (!Double.isNaN(valY0) && (this.probY - 1 < valY0 && valY0 < this.probY + 1))
                this.probY = (this.probY * 3 + valY0) / 4;
            else if (!Double.isNaN(valY1) && (this.probY - 1 < valY1 && valY1 < this.probY + 1))
                this.probY = (this.probY * 3 + valY1) / 4;
        }

        // System.out.println(" " + this.probX + " " + this.probY);

        // ajust estimate of angle
        if (this.probAngle > (Math.PI / 2) && this.compass < 0)
            this.probAngle -= (2 * Math.PI);
        else if (this.probAngle < -(Math.PI / 2) && this.compass > 0)
            this.probAngle += (2 * Math.PI);

        // 2/3 estimate + 1/3 compass
        this.probAngle = (this.probAngle * 2 + Math.toRadians(this.compass)) / 3;

        // fix angle over 180 and under -180
        if (this.probAngle > Math.PI)
            this.probAngle -= (2 * Math.PI);
        else if (this.probAngle < -Math.PI)
            this.probAngle += (2 * Math.PI);
    }

    /**
     * Calcule out
     * 
     * @param out last out
     * @param in  wheels power
     * @return actual out
     */
    private double getOut(double lastOut, double in) {
        return (lastOut + in) / 2;
    }

    /**
     * Calcule estimate of X position
     * 
     * @return estimate of X position
     */
    private double getProbX() {
        return this.probX + this.getLin() * Math.cos(this.probAngle);
    }

    /**
     * Calcule estimate of Y position
     * 
     * @return estimate of Y position
     */
    private double getProbY() {
        return this.probY + this.getLin() * Math.sin(this.probAngle);
    }

    /**
     * Calcule linear move
     * 
     * @return linear move
     */
    private double getLin() {
        return (this.outR + this.outL) / 2;
    }

    /**
     * Calcule rotational move
     * 
     * @return rotational move
     */
    private double getRot() {
        return (this.outR - this.outL);
    }

    /**
     * Calcule estimate of angle
     * 
     * @return estimate of angle
     */
    private double getProbAngle() {
        return this.probAngle + this.getRot();
    }

    /**
     * Gives the agent's angle of deviation
     * 
     * @return angle of deviation
     */
    private double getAngle() {
        double angleValue = Math.toDegrees(this.probAngle);
        double angle = 0;

        switch (this.actualMove) {
            case UP:
                angle = 90 - angleValue;
                break;
            case DOWN:
                angle = -90 - angleValue;
                break;
            case LEFT:
                if (angleValue < 0)
                    angle = -180 - angleValue;
                else
                    angle = 180 - angleValue;
                break;
            case RIGHT:
                angle = 0 - angleValue;
                break;
            default:
                angle = angleValue;
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
        if ((-0.12 < this.probX - this.nextPosX && (this.actualMove.equals(Move.RIGHT))
                || this.probX - this.nextPosX <= 0.12 && this.actualMove.equals(Move.LEFT))
                || (-0.12 < this.probY - this.nextPosY && this.actualMove.equals(Move.UP))
                || (this.probY - this.nextPosY <= 0.12 && this.actualMove.equals(Move.DOWN))
                || this.actualMove.equals(Move.NONE)
                || (this.irSensor0 >= 2 && (-10 < angle && angle < 10))) {

            if (!this.inGoodAngle() && !this.actualMove.equals(Move.NONE)) {
                this.inRotation = true;
                return false;
            }

            this.actualPosX = this.nextPosX;
            this.actualPosY = this.nextPosY;
            this.actualPos = new Tuple<>(this.actualPosX, this.actualPosY);
            this.allowRotate = true;
            this.inRotation = false;

            // System.out.println("\n-> " + this.probX + " " + this.probY + " | " +
            // this.nextPosX + " " + this.nextPosY + " | " + this.irSensor0);
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
        double angle = Math.toDegrees(this.probAngle);
        if ((ANGLE_VARIATION[0][0] < angle && angle < ANGLE_VARIATION[0][1]
                && irSensors[0] >= THRESHHOLD_SENSORS)
                || (ANGLE_VARIATION[2][0] < angle && angle < ANGLE_VARIATION[2][1]
                        && irSensors[1] >= THRESHHOLD_SENSORS)
                || (ANGLE_VARIATION[1][0] < angle && angle < ANGLE_VARIATION[1][1]
                        && irSensors[2] >= THRESHHOLD_SENSORS)
                || ((ANGLE_VARIATION[3][0] > angle || angle > ANGLE_VARIATION[3][1])
                        && irSensors[3] >= THRESHHOLD_SENSORS))
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
            if (!pos.equals(this.actualPos) && (!this.myMap.ckeckedPos(pos.x, pos.y, false) || this.goInitPos)) {
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
        if (!this.actualMove.equals(Move.NONE))
            this.lastMove = this.actualMove;
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
    private Tuple<Integer, Integer> convertNodeToTuple(Node node) {
        return new Tuple<>(node.getCol() - (MyMap.CELLCOLS - 1), node.getRow() - (MyMap.CELLROWS - 1));
    }

    /**
     * Find best paths between targets
     * 
     * @return true if find best paths between targets otherwise false
     */
    private boolean findPathBetweenTargets() {
        this.pathBetweenTargets = new HashMap<>();

        for (int i = 0; i < this.grounds.size(); i++) {
            Node nodeI = new Node((MyMap.CELLROWS - 1) + this.grounds.get(i).y,
                    (MyMap.CELLCOLS - 1) + this.grounds.get(i).x);

            for (int j = i + 1; j < this.grounds.size(); j++) {
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
                // if (bestSubPath.size() < path.size()) {
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
        this.nextPosX = nextPos.x;
        this.nextPosY = nextPos.y;
        this.actualMove = this.giveMoveGivenNextPos(nextPos);
    }

    /**
     * Find best path
     */
    private void getBestPath() {
        // get all possible permutations of path
        String targetsId = "";
        for (int i = 1; i < this.grounds.size(); i++)
            targetsId += i;
        List<String> permutationsTargets = getAllPermutationsTargets(targetsId);

        int bestPathCost = 0;
        // int bestPathSize = 0;

        // get path from all permutations
        for (int i = 0; i < permutationsTargets.size() - 1; i++) {
            List<List<Node>> path = new ArrayList<>();
            int pathCost = 0;
            // int pathSize = 0;

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
                pathCost += subPath.get(subPath.size() - 1).getFCost();

                // add sub path size
                // pathSize += subPath.size();

                // actual path is worst than actual path
                // if (this.bestPath != null && pathSize > bestPathSize)
                if (this.bestPath != null && pathCost > bestPathCost)
                    break;
                path.add(subPath);
            }

            // if path founded is better than before
            // if (this.bestPath == null || pathSize < bestPathSize) {
            if (this.bestPath == null || pathCost < bestPathCost) {
                this.bestPath = path;
                bestPathCost = pathCost;
                // bestPathSize = pathSize;
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
