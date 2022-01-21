import ciberIF.*;

/**
 * Abstract Client
 * 
 */
public abstract class Client {

    /** Client States */
    public static enum State {
        WAIT, RUN, FINISH
    }

    /** Client Moves */
    public static enum Move {
        UP, DOWN, RIGHT, LEFT, NONE;

        /**
         * Check if is the oposite move
         * 
         * @param move0
         * @param move1
         * @return true if is the oposite move, otherwise false
         */
        public static boolean isOpositeMove(Move move0, Move move1) {
            switch (move0) {
                case UP:
                    if (move1.equals(DOWN))
                        return true;
                    break;
                case DOWN:
                    if (move1.equals(UP))
                        return true;
                    break;
                case RIGHT:
                    if (move1.equals(LEFT))
                        return true;
                    break;
                case LEFT:
                    if (move1.equals(RIGHT))
                        return true;
                    break;
                default:
                    return true;
            }
            return false;
        }
    }

    private ciberIF cif;
    private State state;
    private double[] sensorsAngle;
    private String filename;

    /**
     * Client Constructor
     * 
     * @param args         command line arguments
     * @param sensorsAngle angle of the sensors
     */
    public Client(String[] args, double[] sensorsAngle) {
        this.sensorsAngle = sensorsAngle;
        this.cif = new ciberIF();
        this.state = State.WAIT;
        this.commandLineValidate(args);
    }

    public ciberIF getCiberIF() {
        return this.cif;
    }

    public State getState() {
        return this.state;
    }

    public void changeState() {
        this.state = State.FINISH;
    }

    public String getFilename() {
        return this.filename;
    }

    /**
     * Reads a new message, decides what to do and sends action to simulator
     */
    public void mainLoop() {
        while (true) {
            this.readSensors();
            if (this.cif.GetStartButton() && this.state == State.WAIT)
                this.state = State.RUN;
            this.decide();
        }
    }

    /**
     * Command line validator
     * 
     * @param args arguments of passed by commad line
     */
    public void commandLineValidate(String[] args) {
        String host, robName, filename;
        int pos;
        int arg;

        // default values
        host = "localhost";
        robName = "jClient";
        filename = "file.out";
        pos = 1;

        // parse command-line arguments
        try {
            arg = 0;
            while (arg < args.length) {
                if (args[arg].equals("--pos") || args[arg].equals("-p")) {
                    if (args.length > arg + 1) {
                        pos = Integer.valueOf(args[arg + 1]).intValue();
                        arg += 2;
                    }
                } else if (args[arg].equals("--robname") || args[arg].equals("-r")) {
                    if (args.length > arg + 1) {
                        robName = args[arg + 1];
                        arg += 2;
                    }
                } else if (args[arg].equals("--host") || args[arg].equals("-h")) {
                    if (args.length > arg + 1) {
                        host = args[arg + 1];
                        arg += 2;
                    }
                } else if (args[arg].equals("--filename") || args[arg].equals("-f")) {
                    if (args.length > arg + 1) {
                        filename = args[arg + 1];
                        arg += 2;
                    }
                } else
                    throw new Exception();
            }
        } catch (Exception e) {
            System.out.println(
                    "Usage: java ClientC[1||2||3] [--robname <robname>] [--pos <pos>] [--host <hostname>[:<port>]] [--filename <filename>]");
            return;
        }

        // register robot in simulator
        if (this.sensorsAngle == null)
            this.cif.InitRobot(robName, pos, host);
        else
            this.cif.InitRobot2(robName, pos, this.sensorsAngle, host);
        this.filename = filename;
    }

    /**
     * Basic reactive decision algorithm, decides action based on current sensor
     * values
     */
    public void decide() {
        switch (this.state) {
            case WAIT:
                break;
            case RUN:
                // runState
                this.runState();

                // move
                this.wander();

                // time out
                if (this.getCiberIF().GetTime() >= this.cif.GetFinalTime())
                    this.changeState();
                break;
            case FINISH:
                this.getCiberIF().Finish();
                this.finishState();
                System.exit(0);
                break;
        }
    }

    /**
     * Give movement to the agent
     */
    public abstract void wander();

    /**
     * Read sensors, compass, gps, ground
     */
    public abstract void readSensors();

    /**
     * What to do on run state
     */
    public abstract void runState();

    /**
     * What to do on finish state
     */
    public abstract void finishState();

};
