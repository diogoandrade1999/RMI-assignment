/*
    This file is part of ciberRatoToolsSrc.

    Copyright (C) 2001-2015 Universidade de Aveiro

    ciberRatoToolsSrc is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    ciberRatoToolsSrc is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Flow.Subscriber;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import ciberIF.*;

enum Cord {UP, DOWN, LEFT, RIGHT}

/**
 * the map
 */
class Map {
    static final int CELLROWS = 7;
    static final int CELLCOLS = 14;

    /*
     * ! In this map the center of cell (i,j), (i in 0..6, j in 0..13) is mapped to
     * labMap[i*2][j*2]. to know if there is a wall on top of cell(i,j) (i in 0..5),
     * check if the value of labMap[i*2+1][j*2] is space or not
     */
    char[][] labMap;

    public Map() {
        labMap = new char[CELLROWS * 2 - 1][CELLCOLS * 2 - 1];

        for (int r = 0; r < labMap.length; r++) {
            Arrays.fill(labMap[r], ' ');
        }
    }
};

/**
 * class MapHandler parses a XML file defining the labyrinth
 */
class MapHandler extends DefaultHandler {

    /**
     */
    private Map map;

    /**
     * returns the Parameters collected during parsing of message
     */
    Map getMap() {
        return map;
    }

    public void startElement(String namespaceURI, String sName, // simple name
            String qName, // qualified name
            Attributes attrs) throws SAXException {

        // Create map object to hold map
        if (map == null)
            map = new Map();

        if (qName.equals("Row")) { // Row Values

            if (attrs != null) {
                String rowStr = attrs.getValue("Pos");
                if (rowStr != null) {
                    int row = Integer.valueOf(rowStr).intValue();
                    String pattern = attrs.getValue("Pattern");
                    for (int col = 0; col < pattern.length(); col++) {
                        if (row % 2 == 0) { // only vertical walls are allowed here
                            if (pattern.charAt(col) == '|') {
                                map.labMap[row][(col + 1) / 3 * 2 - 1] = '|';
                            }
                        } else {// only horizontal walls are allowed at odd rows
                            if (col % 3 == 0) { // if there is a wall at this collumn then there must also be a wall in
                                                // the next one
                                if (pattern.charAt(col) == '-') {
                                    map.labMap[row][col / 3 * 2] = '-';
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void endElement(String namespaceURI, String sName, // simple name
            String qName // qualified name
    ) throws SAXException {
    }
};

class MyMap {
    static final int CELLROWS = Map.CELLROWS * 2;
    static final int CELLCOLS = Map.CELLCOLS * 2;

    /*
     * ! In this map the center of cell (i,j), (i in 0..6, j in 0..13) is mapped to
     * labMap[i*2][j*2]. to know if there is a wall on top of cell(i,j) (i in 0..5),
     * check if the value of labMap[i*2+1][j*2] is space or not
     */
    char[][] labMap;

    public MyMap() {
        labMap = new char[CELLROWS * 2 - 1][CELLCOLS * 2 - 1];

        for (int r = 0; r < labMap.length; r++) {
            Arrays.fill(labMap[r], ' ');
        }
    }

    // The positions must be given between (0 and CELLROWS) and (0 and CELLCOLS)
    public void addObstacle(int xPos, int yPos, Cord cord) {
        System.out.println(xPos + " " + yPos + " " + cord);
        switch(cord) {
            case UP:
                labMap[(CELLROWS - 1) + (yPos + 1)][(CELLCOLS - 1) + xPos] = '-'; // Adds '-' to the top of the position (yPos + 1)
                break;
            case DOWN:
                labMap[(CELLROWS - 1) + (yPos - 1)][(CELLCOLS - 1) + xPos] = '-'; // Adds '-' to the bottom of the position (yPos - 1)
                break;
            case LEFT:
                labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos - 1)] = '|'; // Adds '|' to the left of the position (xPos - 1)
                break;
            case RIGHT:
                labMap[(CELLROWS - 1)  + yPos][(CELLCOLS - 1)+ (xPos + 1)] = '|'; // Adds '|' to the right of the position (xPos + 1)
                break;
        }
    }

    public void addFree(int xPos, int yPos, Cord cord) {
        switch(cord) {
            case UP:
                labMap[(CELLROWS - 1) + (yPos + 1)][(CELLCOLS - 1) + xPos] = 'X'; // Adds 'X' to the top of the position (yPos + 1)
                break;
            case DOWN:
                labMap[(CELLROWS - 1) + (yPos - 1)][(CELLCOLS - 1) + xPos] = 'X'; // Adds 'X' to the bottom of the position (yPos - 1)
                break;
            case LEFT:
                labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos - 1)] = 'X'; // Adds 'X' to the left of the position (xPos - 1)
                break;
            case RIGHT:
                labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos + 1)] = 'X'; // Adds 'X' to the right of the position (xPos + 1)
                break;
        }
    }

    public boolean checkFreePos(int xPos, int yPos, Cord cord) {
        boolean response = false;
        switch(cord) {
            case UP:
                if (labMap[(CELLROWS - 1) + (yPos + 1)][(CELLCOLS - 1) + xPos] == 'X') // Check if 'X' to the top of the position (yPos + 1)
                    response = true;
                break;
            case DOWN:
                if (labMap[(CELLROWS - 1) + (yPos - 1)][(CELLCOLS - 1) + xPos] == 'X') // Check if 'X' to the bottom of the position (yPos - 1)
                    response = true;
                break;
            case LEFT:
                if (labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos - 1)] == 'X') // Check if 'X' to the left of the position (xPos - 1)
                    response = true;
                break;
            case RIGHT:
                if (labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos + 1)] == 'X') // Check if 'X' to the right of the position (xPos + 1)
                    response = true;
                break;
        }
        return response;
    }

    public boolean checkObstaclePos(int xPos, int yPos, Cord cord) {
        boolean response = false;
        switch(cord) {
            case UP:
                if (labMap[(CELLROWS - 1) + (yPos + 1)][(CELLCOLS - 1) + xPos] == '-') // Check if '-' to the top of the position (yPos + 1)
                    response = true;
                break;
            case DOWN:
                if (labMap[(CELLROWS - 1) + (yPos - 1)][(CELLCOLS - 1) + xPos] == '-') // Check if '-' to the bottom of the position (yPos - 1)
                    response = true;
                break;
            case LEFT:
                if (labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos - 1)] == '|') // Check if '|' to the left of the position (xPos - 1)
                    response = true;
                break;
            case RIGHT:
                if (labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos + 1)] == '|') // Check if '|' to the right of the position (xPos + 1)
                    response = true;
                break;
        }
        return response;
    }

    public boolean ckeckedPos(int xPos, int yPos) {
        if (labMap[(CELLROWS - 1) + (yPos + 1)][(CELLCOLS - 1) + xPos] == ' ' ||
            labMap[(CELLROWS - 1) + (yPos - 1)][(CELLCOLS - 1) + xPos] == ' ' ||
            labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos - 1)] == ' ' ||
            labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos + 1)] == ' ')
            return false;
        return true;
    }
};

/**
 * example of a basic agent implemented using the java interface library.
 */
public class jClient {

    ciberIF cif;
    Map map;

    enum State {
        RUN, FINISH
    }

    enum Move {
        UP, DOWN, RIGHT, LEFT, NONE
    }

    public static void main(String[] args) {

        String host, robName;
        int pos;
        int arg;
        Map map;

        // default values
        host = "localhost";
        robName = "jClient";
        pos = 1;
        map = null;

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
                } else if (args[arg].equals("--map") || args[arg].equals("-m")) {
                    if (args.length > arg + 1) {

                        MapHandler mapHandler = new MapHandler();

                        SAXParserFactory factory = SAXParserFactory.newInstance();
                        SAXParser saxParser = factory.newSAXParser();
                        FileInputStream fstream = new FileInputStream(args[arg + 1]);
                        saxParser.parse(fstream, mapHandler);

                        map = mapHandler.getMap();

                        arg += 2;
                    }
                } else
                    throw new Exception();
            }
        } catch (Exception e) {
            print_usage();
            return;
        }

        // create client
        jClient client = new jClient();

        client.robName = robName;

        // register robot in simulator
        double[] sensorAngle = {0, 90, -90, 180};
        client.cif.InitRobot2(robName, pos, sensorAngle, host);
        client.map = map;
        client.printMap();

        // main loop
        client.mainLoop();
    }

    // Constructor
    jClient() {
        cif = new ciberIF();

        state = State.RUN;
    }

    /**
     * reads a new message, decides what to do and sends action to simulator
     */
    public void mainLoop() {

        // create empty map
        myMap = new MyMap();
        posToView = new Stack<>();
        listNextPos = new LinkedList<>();

        // read first position
        cif.ReadSensors();
        while(!cif.IsGPSReady());
        initGpsX = cif.GetX();
        initGpsY = cif.GetY();

        actualMove = Move.NONE;
        nextMove = Move.NONE;
        gpsX = 0;
        gpsY = 0;
        nextPosX = 0;
        nextPosY = 0;

        while (true) {
            cif.ReadSensors();
            decide();
        }
    }

    private double getGpsX() {
        return cif.GetX() - initGpsX;
    }

    private double getGpsY() {
        return cif.GetY() - initGpsY;
    }

    private void moveRight() {
        if ((-3 < compass && compass < 3 && irSensor0 < 1.1) ||
            (87 < compass && compass < 93 && irSensor2 < 1.1) ||
            (-93 < compass && compass < -87 && irSensor1 < 1.1) ||
            ((-177 > compass || compass > 177) && irSensor3 < 1.1)) {
            if ((nextMove.equals(Move.NONE) || nextMove.equals(Move.RIGHT)) && !myMap.checkFreePos(nextPosX, nextPosY, Cord.RIGHT)) {
                nextMove = Move.RIGHT;
            } else if (!myMap.checkFreePos(nextPosX, nextPosY, Cord.RIGHT)) {
                int[] pos = {nextPosX + 2, nextPosY};
                posToView.add(pos);
            }
            myMap.addFree(nextPosX, nextPosY, Cord.RIGHT);
        }
    }

    private void moveLeft() {
        if ((-3 < compass && compass < 3 && irSensor3 < 1.1) ||
            (87 < compass && compass < 93 && irSensor1 < 1.1) ||
            (-93 < compass && compass < -87 && irSensor2 < 1.1) ||
            ((-177 > compass || compass > 177) && irSensor0 < 1.1)) {
            if ((nextMove.equals(Move.NONE) || nextMove.equals(Move.LEFT)) && !myMap.checkFreePos(nextPosX, nextPosY, Cord.LEFT)) {
                nextMove = Move.LEFT;
            } else if (!myMap.checkFreePos(nextPosX, nextPosY, Cord.LEFT)) {
                int[] pos = {nextPosX - 2, nextPosY};
                posToView.add(pos);
            }
            myMap.addFree(nextPosX, nextPosY, Cord.LEFT);
        }
    }

    private void moveUp() {
        if ((-3 < compass && compass < 3 && irSensor1 < 1.1) ||
            (87 < compass && compass < 93 && irSensor0 < 1.1) ||
            (-93 < compass && compass < -87 && irSensor3 < 1.1) ||
            ((-177 > compass || compass > 177) && irSensor2 < 1.1)) {
            if ((nextMove.equals(Move.NONE) || nextMove.equals(Move.UP)) && !myMap.checkFreePos(nextPosX, nextPosY, Cord.UP)) {
                nextMove = Move.UP;
            } else if (!myMap.checkFreePos(nextPosX, nextPosY, Cord.UP)) {
                int[] pos = {nextPosX, nextPosY + 2};
                posToView.add(pos);
            }
            myMap.addFree(nextPosX, nextPosY, Cord.UP);
        }
    }

    private void moveDown() {
        if ((-3 < compass && compass < 3 && irSensor2 < 1.1) ||
            (87 < compass && compass < 93 && irSensor3 < 1.1) ||
            (-93 < compass && compass < -87 && irSensor0 < 1.1) ||
            ((-177 > compass || compass > 177) && irSensor1 < 1.1)) {
            if ((nextMove.equals(Move.NONE) || nextMove.equals(Move.DOWN)) && !myMap.checkFreePos(nextPosX, nextPosY, Cord.DOWN)) {
                nextMove = Move.DOWN;
            } else if (!myMap.checkFreePos(nextPosX, nextPosY, Cord.DOWN)) {
                int[] pos = {nextPosX, nextPosY - 2};
                posToView.add(pos);
            }
            myMap.addFree(nextPosX, nextPosY, Cord.DOWN);
        }
    }

    private void discoverMap() {
        if ((-3 < compass && compass < 3 && irSensor0 >= 1.1) ||
            (87 < compass && compass < 93 && irSensor2 >= 1.1) ||
            (-93 < compass && compass < -87 && irSensor1 >= 1.1) ||
            ((-177 > compass || compass > 177) && irSensor3 >= 1.1)) {
                myMap.addObstacle(nextPosX, nextPosY, Cord.RIGHT);
        } 
         if ((-3 < compass && compass < 3 && irSensor1 >= 1.1) ||
            (87 < compass && compass < 93 && irSensor0 >= 1.1) ||
            (-93 < compass && compass < -87 && irSensor3 >= 1.1) ||
            ((-177 > compass || compass > 177) && irSensor2 >= 1.1)) {
                myMap.addObstacle(nextPosX, nextPosY, Cord.UP);
        }
        if ((-3 < compass && compass < 3 && irSensor2 >= 1.1) ||
            (87 < compass && compass < 93 && irSensor3 >= 1.1) ||
            (-93 < compass && compass < -87 && irSensor0 >= 1.1) ||
            ((-177 > compass || compass > 177) && irSensor1 >= 1.1)) {
                myMap.addObstacle(nextPosX, nextPosY, Cord.DOWN);
        }
        if ((-3 < compass && compass < 3 && irSensor3 >= 1.1) ||
            (87 < compass && compass < 93 && irSensor1 >= 1.1) ||
            (-93 < compass && compass < -87 && irSensor2 >= 1.1) ||
            ((-177 > compass || compass > 177) && irSensor0 >= 1.1)) {
                myMap.addObstacle(nextPosX, nextPosY, Cord.LEFT);
        }

        Cord cord = convertMoveToCord(actualMove);
        if (!myMap.checkObstaclePos(nextPosX, nextPosY, cord) && !myMap.checkFreePos(nextPosX, nextPosY, cord)) {
            nextMove = actualMove;
        }

        switch (actualMove){
            case UP:
                moveUp();
                moveRight();
                moveLeft();
                moveDown();
                break;
            case DOWN:
                moveDown();
                moveRight();
                moveLeft();
                moveUp();
                break;
            case LEFT:
                moveLeft();
                moveUp();
                moveDown();
                moveRight();
                break;
            case RIGHT:
            default:
                moveRight();
                moveUp();
                moveDown();
                moveLeft();
                break;
        }

        if (nextMove.equals(Move.NONE)) {
            searchNextPos();
        }
    }

    private void searchNextPos() {
        if (posToView.isEmpty()) {
            state = State.FINISH;
            return;
        }

        int[] nextPos;
        boolean validPos;
        int goalPosX, goalPosY;
        do {
            nextPos = posToView.pop();
            goalPosX = nextPos[0];
            goalPosY = nextPos[1];
            validPos = !myMap.ckeckedPos(goalPosX, goalPosY);
        } while (!validPos && !posToView.isEmpty());

        if (!validPos) {
            state = State.FINISH;
        } else {
            //apply A*
            Node initialNode = new Node((MyMap.CELLROWS - 1) + nextPosY, (MyMap.CELLCOLS - 1) + nextPosX);
            Node finalNode = new Node((MyMap.CELLROWS - 1) + goalPosY, (MyMap.CELLCOLS - 1) + goalPosX);
            AStar aStar = new AStar(MyMap.CELLROWS * 2 - 1, MyMap.CELLCOLS * 2 - 1, initialNode, finalNode);
            aStar.setBlocks(myMap.labMap);
            List<Node> path = aStar.findPath();
            if (path.isEmpty()) {
                state = State.FINISH;
                return;
            }
            for (int i = 1; i < path.size(); i++) {
                Node node = path.get(i);
                int[] pos = {node.getCol() - (MyMap.CELLCOLS - 1), node.getRow() - (MyMap.CELLROWS - 1)};
                listNextPos.add(pos);
            }
            System.out.println("path: " + path);
        }
    }

    public void wander() {
        double rot = Math.toRadians(getAngle());
        if(rot > 0)
            cif.DriveMotors(0.15 - rot, 0.15);
        else if(rot < 0)
            cif.DriveMotors(0.15, 0.15 + rot);
        else
            cif.DriveMotors(+0.15, +0.15);
    }

    private double getAngle() {
        double angle = 0;

        switch (actualMove){
            case UP:
                angle = 90 - compass;
                break;
            case DOWN:
                angle = -90 - compass;
                break;
            case LEFT:
                if (compass < 0)
                    angle = -180 - compass;
                else
                    angle = 180 - compass;
                break;
            case RIGHT:
                angle = 0 - compass;
                break;
            default:
                angle = compass;
                break;
        }
        return angle;
    }

    private void getNextPos() {
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

    private void getNextMove() {
        int[] pos = listNextPos.poll();
        if (nextPosX + 2 == pos[0])
            nextMove = Move.RIGHT;
        else if (nextPosX - 2 == pos[0])
            nextMove = Move.LEFT;
        else if (nextPosY + 2 == pos[1])
            nextMove = Move.UP;
        else if (nextPosY - 2 == pos[1])
            nextMove = Move.DOWN;
    }

    private Cord convertMoveToCord(Move move) {
        Cord cord;
        switch (move) {
            case RIGHT:
                cord = Cord.RIGHT;
                break;
            case LEFT:
                cord = Cord.LEFT;
                break;
            case UP:
                cord = Cord.UP;
                break;
            case DOWN:
                cord = Cord.DOWN;
                break;
            default:
                cord = null;
                break;
        }
        return cord;
    }

    private boolean closeToNextPos() {
        double angle = getAngle();
        if (((-0.3 < gpsX - nextPosX && gpsX - nextPosX <= 0.3) && (-0.3 < gpsY - nextPosY && gpsY - nextPosY <= 0.3)) ||
            (irSensor0 >= 2 && (-3 < angle && angle < 3)))
            return true;
        return false;
    }

    /**
     * basic reactive decision algorithm, decides action based on current sensor
     * values
     */
    public void decide() {
        if (cif.IsObstacleReady(0))
            irSensor0 = cif.GetObstacleSensor(0);
        if (cif.IsObstacleReady(1))
            irSensor1 = cif.GetObstacleSensor(1);
        if (cif.IsObstacleReady(2))
            irSensor2 = cif.GetObstacleSensor(2);
        if (cif.IsObstacleReady(2))
            irSensor3 = cif.GetObstacleSensor(3);

        if (cif.IsCompassReady())
            compass = cif.GetCompassSensor();

        if (cif.IsGPSReady()) {
            gpsX = getGpsX();
            gpsY = getGpsY();
        }

        switch (state) {
            case RUN: /* Go */
                if (closeToNextPos() && nextMove.equals(Move.NONE)) {
                    if (listNextPos.isEmpty())
                        // discover map
                        discoverMap();
                    
                    if (!listNextPos.isEmpty())
                        getNextMove();

                    if (!nextMove.equals(Move.NONE)) {    
                        // get next pos in (x, y)
                        getNextPos();
                    }
                }

                // move
                wander();

                // time out
                if (cif.GetTime() >= 5000){
                    state = State.FINISH;
                }
                break;
            case FINISH:
                writeMap();
                System.exit(0);
                break;
        }
    }

    static void print_usage() {
        System.out.println(
                "Usage: java jClient [--robname <robname>] [--pos <pos>] [--host <hostname>[:<port>]] [--map <map_filename>]");
    }

    public void printMap() {
        if (map == null)
            return;

        for (int r = map.labMap.length - 1; r >= 0; r--) {
            System.out.println(map.labMap[r]);
        }
    }

    public void writeMap() {
        for (int i = myMap.labMap.length - 1; i >= 0; i--) {
            for (char c : myMap.labMap[i]) {
                System.out.print(c);
            }
            System.out.println();
        }
    }

    private String robName;
    private double irSensor0, irSensor1, irSensor2, irSensor3, compass;
    private double gpsX, gpsY, initGpsX, initGpsY;
    private int nextPosX, nextPosY;
    private State state;
    private Stack<int[]> posToView;
    private MyMap myMap;
    private Move actualMove, nextMove;
    private Queue<int[]> listNextPos;
}
