import java.util.*;

/**
 * Client for the second challenge
 * 
 */
public class ClientC2 extends Client {

    private static final double[] SENSORS_ANGLES = { 0, 90, -90, 180 };
    private static final double THRESHHOLD_SENSORS = 1.1;
    private final double initGpsX, initGpsY;
    private double irSensor0, irSensor1, irSensor2, irSensor3, compass, gpsX, gpsY;
    private int actualPosX, actualPosY, nextPosX, nextPosY;
    private Move actualMove, nextMove;
    private Stack<Tuple<Integer, Integer>> posToView;
    private Queue<Tuple<Integer, Integer>> listNextPos;
    private MyMap myMap;
    private boolean inGoalPos, discoverMapGoal, useCheckedPos;

    /**
     * Client Constructor
     * 
     * @param args          command line arguments
     * @param useCheckedPos true is agent can use the method checkedPos of the MyMap
     */
    public ClientC2(String[] args, boolean useCheckedPos) {
        super(args, SENSORS_ANGLES);

        // read first position
        do {
            this.getCiberIF().ReadSensors();
        } while (!this.getCiberIF().IsGPSReady());

        this.initGpsX = this.getCiberIF().GetX();
        this.initGpsY = this.getCiberIF().GetY();

        this.gpsX = 0;
        this.gpsY = 0;
        this.actualPosX = 0;
        this.actualPosY = 0;
        this.nextPosX = 0;
        this.nextPosY = 0;
        this.actualMove = Move.NONE;
        this.nextMove = Move.NONE;
        this.posToView = new Stack<>();
        this.listNextPos = new LinkedList<>();
        this.myMap = new MyMap();
        this.inGoalPos = false;
        this.discoverMapGoal = true;
        this.useCheckedPos = useCheckedPos;
    }

    public Tuple<Integer, Integer> getActualPos() {
        return new Tuple<>(this.actualPosX, this.actualPosY);
    }

    public MyMap getMyMap() {
        return this.myMap;
    }

    public boolean inGoalPos() {
        return this.inGoalPos;
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

        if (this.getCiberIF().IsGPSReady()) {
            this.gpsX = this.getCiberIF().GetX() - this.initGpsX;
            this.gpsY = this.getCiberIF().GetY() - this.initGpsY;
        }
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
        }
    }

    @Override
    public void finishState() {
        System.out.println("Time: " + this.getCiberIF().GetTime());
        this.myMap.exportMap(this.getFilename());
    }

    public static void main(String[] args) {
        // create client
        Client client = new ClientC2(args, true);

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
        if (((-0.25 < this.gpsX - this.nextPosX && this.gpsX - this.nextPosX <= 0.25)
                && (-0.25 < this.gpsY - this.nextPosY && this.gpsY - this.nextPosY <= 0.25))
                || (this.irSensor0 >= 3.3 && (-3 < angle && angle < 3))) {
            this.actualPosX = this.nextPosX;
            this.actualPosY = this.nextPosY;
            this.inGoalPos = true;
            return true;
        }
        this.inGoalPos = false;
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
            this.checkFree(Move.DOWN, 0, -2);
            break;
        case DOWN:
            this.checkFree(Move.DOWN, 0, -2);
            this.checkFree(Move.RIGHT, +2, 0);
            this.checkFree(Move.LEFT, -2, 0);
            this.checkFree(Move.UP, 0, +2);
            break;
        case LEFT:
            this.checkFree(Move.LEFT, -2, 0);
            this.checkFree(Move.UP, 0, +2);
            this.checkFree(Move.DOWN, 0, -2);
            this.checkFree(Move.RIGHT, +2, 0);
            break;
        case RIGHT:
        default:
            this.checkFree(Move.RIGHT, +2, 0);
            this.checkFree(Move.UP, 0, +2);
            this.checkFree(Move.DOWN, 0, -2);
            this.checkFree(Move.LEFT, -2, 0);
            break;
        }

        // pass next move to actual move
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
        List<Node> choosedPath = new ArrayList<>();
        Tuple<Integer, Integer> choosedPos = new Tuple<>(0, 0);
        Stack<Tuple<Integer, Integer>> newPosToView = new Stack<>();

        // actual position
        Node initialNode = new Node((MyMap.CELLROWS - 1) + this.actualPosY, (MyMap.CELLCOLS - 1) + this.actualPosX);

        for (int i = 0; i < this.posToView.size(); i++) {
            // position left behind
            Tuple<Integer, Integer> pos = this.posToView.get(i);

            // check if position was verified by another path
            if (!pos.equals(this.getActualPos())
                    && (!this.useCheckedPos || (this.useCheckedPos && !this.myMap.ckeckedPos(pos.x, pos.y)))) {
                // goal position
                Node finalNode = new Node((MyMap.CELLROWS - 1) + pos.y, (MyMap.CELLCOLS - 1) + pos.x);

                // apply A*
                AStar aStar = new AStar(MyMap.CELLROWS * 2 - 1, MyMap.CELLCOLS * 2 - 1, initialNode, finalNode);
                aStar.setBlocks(this.myMap.getLabMap(), MyMap.BLOCKERS_ALL);
                List<Node> path = aStar.findPath();

                // if this position has the close path from actual position
                if (path.size() > 0 && (choosedPath.size() == 0 || path.size() < choosedPath.size())) {
                    if (choosedPos.x != 0 || choosedPos.y != 0)
                        newPosToView.add(choosedPos);
                    choosedPath = path;
                    choosedPos = pos;
                }
                // otherwise check later
                else if (path.size() >= choosedPath.size())
                    newPosToView.add(pos);
            }
        }

        // if find position to check
        if (!choosedPath.isEmpty()) {
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
     * Change agent goal
     */
    public void changeGoal() {
        this.discoverMapGoal = false;
    }

}
