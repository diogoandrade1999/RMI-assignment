
/**
 * Client for the first challenge
 * 
 */
public class ClientC1 extends Client {

    private double irSensor0, irSensor1, irSensor2, lastTime;
    private int ground, countLaps, lastTarget;
    private boolean inGround, turnAround;

    /**
     * Client Constructor
     * 
     * @param args command line arguments
     */
    public ClientC1(String[] args) {
        super(args, null);
        this.lastTime = 0;
        this.ground = 0;
        this.countLaps = 0;
        this.lastTarget = 0;
        this.inGround = true;
    }

    @Override
    public void wander() {
        // turn around
        if (this.turnAround) {
            boolean rLeft = false;
            if (this.irSensor1 > this.irSensor2)
                rLeft = true;
            double rot = 0.15;
            double sensor0 = this.irSensor0;
            while (true) {
                if (!rLeft)
                    this.getCiberIF().DriveMotors(+rot, -rot);
                else
                    this.getCiberIF().DriveMotors(-rot, +rot);
                this.getCiberIF().ReadSensors();
                if (sensor0 > this.getCiberIF().GetObstacleSensor(0))
                    break;
                sensor0 = this.getCiberIF().GetObstacleSensor(0);
            }
            this.turnAround = false;
        }

        // rotate with motors going inverse
        else if (this.irSensor0 > 1.3) {
            boolean rLeft = false;
            if (this.irSensor1 < this.irSensor2)
                rLeft = true;
            double rot = 0.15;
            for (int i = 0; i < 3; i++) {
                if (!rLeft)
                    this.getCiberIF().DriveMotors(+rot, -rot);
                else
                    this.getCiberIF().DriveMotors(-rot, +rot);
            }
        }

        // warning fix position
        else if (this.irSensor1 > 5.0 || this.irSensor2 > 5.0)
            if (this.irSensor1 > this.irSensor2)
                this.getCiberIF().DriveMotors(+0.15, +0.05);
            else
                this.getCiberIF().DriveMotors(+0.05, +0.15);

        // fix position
        else if (this.irSensor1 > 3.3 || this.irSensor2 > 3.3) {
            if (this.irSensor1 > this.irSensor2)
                this.getCiberIF().DriveMotors(+0.15, +0.12);
            else
                this.getCiberIF().DriveMotors(+0.12, +0.15);
        }

        // in front
        else
            this.getCiberIF().DriveMotors(0.15, 0.15);
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

        if (this.getCiberIF().IsGroundReady())
            this.ground = this.getCiberIF().GetGroundSensor();
    }

    @Override
    public void runState() {
        // on top of a target
        if (this.ground != -1) {
            if (!this.inGround) {
                // safety mechanism for when the agent turns around
                if (!(this.ground > this.lastTarget
                        || (this.ground < this.lastTarget && this.ground == 0 && this.lastTarget != 1))) {
                    this.turnAround = true;
                    System.out.println("Turn Around in target: " + this.ground);
                }

                // complete a lap
                if (this.ground == 0 && !this.turnAround) {
                    this.countLaps++;
                    double time = this.getCiberIF().GetTime();
                    System.out.println("Lap: " + this.countLaps + " - Lap Time: " + (time - this.lastTime));
                    this.lastTime = time;
                }
            }
            this.lastTarget = this.ground;
            this.inGround = true;

            // complete goal
            if (this.countLaps == 10) {
                this.changeState();
            }
        } else
            this.inGround = false;
    }

    @Override
    public void finishState() {
        System.out.println("Complete " + this.countLaps + " laps - Total Time: " + this.lastTime + " - Average Time: "
                + (this.lastTime / this.countLaps));
    }

    public static void main(String[] args) {
        // create client
        Client client = new ClientC1(args);

        // main loop
        client.mainLoop();
    }

}
