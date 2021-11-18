import ciberIF.*;

/**
 * Abstract Client
 * 
 */
public abstract class Client {

    /** Client States */
    public static enum State {
        RUN, FINISH
    }

    /** Client Moves */
    public static enum Move {
        UP, DOWN, RIGHT, LEFT, NONE
    }

    private ciberIF cif;
    private State state;
    private double[] sensorsAngle;

    // Constructor
    public Client(double[] sensorsAngle) {
        this.sensorsAngle = sensorsAngle;
        this.cif = new ciberIF();
        this.state = State.RUN;
    }

    public ciberIF getCiberIF() {
        return this.cif;
    }

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    /**
     * Reads a new message, decides what to do and sends action to simulator
     */
    public void mainLoop() {
        while (true) {
            this.readSensors();
            this.decide();
        }
    }

    /**
     * Command line validator
     * 
     * @param args arguments of passed by commad line
     */
    public void commandLineValidate(String[] args) {
        String host, robName;
        int pos;
        int arg;

        // default values
        host = "localhost";
        robName = "jClient";
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
                } else
                    throw new Exception();
            }
        } catch (Exception e) {
            System.out.println(
                    "Usage: java ClientC[1||2||3] [--robname <robname>] [--pos <pos>] [--host <hostname>[:<port>]]");
            return;
        }

        // register robot in simulator
        if (this.sensorsAngle == null)
            this.cif.InitRobot(robName, pos, host);
        else
            this.cif.InitRobot2(robName, pos, this.sensorsAngle, host);
    }

    /**
     * Basic reactive decision algorithm, decides action based on current sensor
     * values
     */
    public void decide() {
        switch (this.state) {
        case RUN:
            // runState
            this.runState();

            // move
            this.wander();

            // time out
            if (this.getCiberIF().GetTime() >= 5000)
                this.setState(State.FINISH);
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
     * Read sensors
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
