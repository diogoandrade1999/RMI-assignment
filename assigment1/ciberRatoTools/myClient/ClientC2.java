import java.util.*;

/**
 * Client for the second challenge
 * 
 */
public class ClientC2 extends Client {

    private static final double THRESHHOLD_SENSORS = 1.1;
    private double irSensor0, irSensor1, irSensor2, irSensor3, compass, gpsX, gpsY;
    public double initGpsX, initGpsY;
    private int nextPosX, nextPosY;
    private Move actualMove, nextMove;
    private Stack<Tuple<Integer, Integer>> posToView;
    private Queue<Tuple<Integer, Integer>> listNextPos;
    private MyMap myMap;

    // Construtor
    public ClientC2(double[] sensorsAngle) {
        super(sensorsAngle);

        this.gpsX = 0;
        this.gpsY = 0;
        this.nextPosX = 0;
        this.nextPosY = 0;
        this.actualMove = Move.RIGHT;
        this.nextMove = Move.NONE;
        this.posToView = new Stack<>();
        this.listNextPos = new LinkedList<>();
        this.myMap = new MyMap();
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
        if (this.closeToNextPos() && this.nextMove.equals(Move.NONE)) {
            if (this.listNextPos.isEmpty())
                this.discoverMap();

            if (!this.listNextPos.isEmpty())
                this.readNextMoveFromList();

            if (!this.nextMove.equals(Move.NONE)) {
                this.giveNextPosGivenMove();
            }
        }
    }

    @Override
    public void finishState() {
        System.out.println("Time: " + this.getCiberIF().GetTime());
        this.myMap.exportMap("map.out");
    }

    public static void main(String[] args) {
        double[] sensorsAngle = { 0, 90, -90, 180 };

        // create client
        ClientC2 client = new ClientC2(sensorsAngle);
        client.commandLineValidate(args);

        // read first position
        do {
            client.getCiberIF().ReadSensors();
        } while (!client.getCiberIF().IsGPSReady());

        client.initGpsX = client.getCiberIF().GetX();
        client.initGpsY = client.getCiberIF().GetY();

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
        if (((-0.3 < gpsX - nextPosX && gpsX - nextPosX <= 0.3) && (-0.3 < gpsY - nextPosY && gpsY - nextPosY <= 0.3))
                || (irSensor0 >= 3.3 && (-3 < angle && angle < 3)))
            return true;
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
        if (!this.myMap.checkObstaclePos(this.nextPosX, this.nextPosY, this.actualMove)
                && !this.myMap.checkFreePos(this.nextPosX, this.nextPosY, this.actualMove))
            this.nextMove = this.actualMove;

        // ! redudant switch code with if above
        // get next move
        switch (this.actualMove) {
        case UP:
            this.checkFree(irSensorsRight, Move.RIGHT, +2, 0);
            this.checkFree(irSensorsLeft, Move.LEFT, -2, 0);
            this.checkFree(irSensorsUp, Move.UP, 0, +2);
            this.checkFree(irSensorsDown, Move.DOWN, 0, -2);
            break;
        case DOWN:
            this.checkFree(irSensorsRight, Move.RIGHT, +2, 0);
            this.checkFree(irSensorsLeft, Move.LEFT, -2, 0);
            this.checkFree(irSensorsDown, Move.DOWN, 0, -2);
            this.checkFree(irSensorsUp, Move.UP, 0, +2);
            break;
        case LEFT:
            this.checkFree(irSensorsUp, Move.UP, 0, +2);
            this.checkFree(irSensorsDown, Move.DOWN, 0, -2);
            this.checkFree(irSensorsLeft, Move.LEFT, -2, 0);
            this.checkFree(irSensorsRight, Move.RIGHT, +2, 0);
            break;
        case RIGHT:
        default:
            this.checkFree(irSensorsUp, Move.UP, 0, +2);
            this.checkFree(irSensorsDown, Move.DOWN, 0, -2);
            this.checkFree(irSensorsRight, Move.RIGHT, +2, 0);
            this.checkFree(irSensorsLeft, Move.LEFT, -2, 0);
            break;
        }

        // if does not find any possible move
        if (this.nextMove.equals(Move.NONE)) {
            if (this.posToView.isEmpty())
                this.setState(State.FINISH);
            else
                this.searchNextPosShortPath();
        }
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
            this.myMap.addObstacle(this.nextPosX, this.nextPosY, move);
    }

    /**
     * Check if the positions of the sides are free path
     * 
     * @param irSensors sensors values
     * @param move      side positons
     * @param plusX     value of x to add to actual x position
     * @param plusY     value of y to add to actual y position
     */
    private void checkFree(double[] irSensors, Move move, int plusX, int plusY) {
        if ((-3 < this.compass && this.compass < 3 && irSensors[0] < THRESHHOLD_SENSORS)
                || (87 < this.compass && this.compass < 93 && irSensors[1] < THRESHHOLD_SENSORS)
                || (-93 < this.compass && this.compass < -87 && irSensors[2] < THRESHHOLD_SENSORS)
                || ((-177 > this.compass || this.compass > 177) && irSensors[3] < THRESHHOLD_SENSORS)) {
            if ((this.nextMove.equals(Move.NONE) || this.nextMove.equals(move))
                    && !this.myMap.checkFreePos(this.nextPosX, this.nextPosY, move))
                this.nextMove = move;
            else if (!this.myMap.checkFreePos(this.nextPosX, this.nextPosY, move))
                this.posToView.add(new Tuple<>(this.nextPosX + plusX, this.nextPosY + plusY));
            this.myMap.addFree(this.nextPosX, this.nextPosY, move);
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
        Node initialNode = new Node((MyMap.CELLROWS - 1) + this.nextPosY, (MyMap.CELLCOLS - 1) + this.nextPosX);
        for (int i = 0; i < this.posToView.size(); i++) {
            Tuple<Integer, Integer> pos = this.posToView.get(i);
            if (!this.myMap.ckeckedPos(pos.x, pos.y)) {
                Node finalNode = new Node((MyMap.CELLROWS - 1) + pos.y, (MyMap.CELLCOLS - 1) + pos.x);
                AStar aStar = new AStar(MyMap.CELLROWS * 2 - 1, MyMap.CELLCOLS * 2 - 1, initialNode, finalNode);
                aStar.setBlocks(this.myMap.getLabMap());
                List<Node> path = aStar.findPath();
                if (path.size() > 0 && (choosedPath.size() == 0 || path.size() < choosedPath.size())) {
                    if (choosedPos.x != 0 || choosedPos.y != 0)
                        newPosToView.add(choosedPos);
                    choosedPath = path;
                    choosedPos = pos;
                } else if (path.size() == 0 || path.size() >= choosedPath.size())
                    newPosToView.add(pos);
            }
        }

        if (choosedPath.isEmpty()) {
            this.setState(State.FINISH);
        } else {
            this.posToView = newPosToView;
            for (int i = 1; i < choosedPath.size(); i++) {
                Node node = choosedPath.get(i);
                this.listNextPos
                        .add(new Tuple<>(node.getCol() - (MyMap.CELLCOLS - 1), node.getRow() - (MyMap.CELLROWS - 1)));
            }
        }
    }

    /**
     * Search for a position left begind using A* algorithm sorted by last position
     * he left begind
     */
    private void searchNextPosLastPos() {
        Tuple<Integer, Integer> choosedPos;
        boolean validPos;
        do {
            choosedPos = this.posToView.pop();
            validPos = !this.myMap.ckeckedPos(choosedPos.x, choosedPos.y);
        } while (!validPos && !posToView.isEmpty());

        if (!validPos) {
            this.setState(State.FINISH);
        } else {
            // apply A*
            Node initialNode = new Node((MyMap.CELLROWS - 1) + this.nextPosY, (MyMap.CELLCOLS - 1) + this.nextPosX);
            Node finalNode = new Node((MyMap.CELLROWS - 1) + choosedPos.y, (MyMap.CELLCOLS - 1) + choosedPos.x);
            AStar aStar = new AStar(MyMap.CELLROWS * 2 - 1, MyMap.CELLCOLS * 2 - 1, initialNode, finalNode);
            aStar.setBlocks(this.myMap.getLabMap());
            List<Node> path = aStar.findPath();
            if (path.isEmpty()) {
                this.setState(State.FINISH);
                return;
            }
            for (int i = 1; i < path.size(); i++) {
                Node node = path.get(i);
                this.listNextPos
                        .add(new Tuple<>(node.getCol() - (MyMap.CELLCOLS - 1), node.getRow() - (MyMap.CELLROWS - 1)));
            }
        }
    }

    /**
     * Read next move from list
     */
    private void readNextMoveFromList() {
        Tuple<Integer, Integer> pos = this.listNextPos.poll();
        if (this.nextPosX + 2 == pos.x)
            this.nextMove = Move.RIGHT;
        else if (this.nextPosX - 2 == pos.x)
            this.nextMove = Move.LEFT;
        else if (this.nextPosY + 2 == pos.y)
            this.nextMove = Move.UP;
        else if (this.nextPosY - 2 == pos.y)
            this.nextMove = Move.DOWN;
    }

    /**
     * Give next position given the next move
     */
    private void giveNextPosGivenMove() {
        switch (nextMove) {
        case RIGHT:
            nextPosX += 2;
            break;
        case LEFT:
            nextPosX -= 2;
            break;
        case UP:
            nextPosY += 2;
            break;
        case DOWN:
            nextPosY -= 2;
            break;
        default:
            break;
        }

        actualMove = nextMove;
        nextMove = Move.NONE;
    }
}
