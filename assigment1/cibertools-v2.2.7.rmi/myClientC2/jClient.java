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
import java.util.Vector;

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
    static final int CELLROWS = 7 * 2;
    static final int CELLCOLS = 14 * 2;

    /*
     * ! In this map the center of cell (i,j), (i in 0..6, j in 0..13) is mapped to
     * labMap[i*2][j*2]. to know if there is a wall on top of cell(i,j) (i in 0..5),
     * check if the value of labMap[i*2+1][j*2] is space or not
     */
    char[][] labMap;

    public MyMap() {
        labMap = new char[CELLCOLS * 2 - 1][CELLROWS * 2 - 1];

        for (int r = 0; r < labMap.length; r++) {
            Arrays.fill(labMap[r], ' ');
        }
    }

    // The positions must be given between (0 and CELLROWS) and (0 and CELLCOLS)
    public void addObstacle(int xPos, int yPos, Cord cord) {
        switch(cord) {
            case UP:
                labMap[(CELLCOLS - 1) + (yPos - 1)][(CELLROWS - 1) + xPos] = '-'; // Adds '-' to the top of the position (yPos - 1)
                break;
            case DOWN:
                labMap[(CELLCOLS - 1) + (yPos + 1)][(CELLROWS - 1) + xPos] = '-'; // Adds '-' to the bottom of the position (yPos + 1)
                break;
            case LEFT:
                labMap[(CELLCOLS - 1) + yPos][(CELLROWS - 1) + (xPos - 1)] = '|'; // Adds '|' to the left of the position (xPos - 1)
                break;
            case RIGHT:
                labMap[(CELLCOLS - 1) + yPos][(CELLROWS - 1) + (xPos + 1)] = '|'; // Adds '|' to the right of the position (xPos + 1)
                break;
        }
    }

    public void addFree(int xPos, int yPos, Cord cord) {
        switch(cord) {
            case UP:
                labMap[(CELLCOLS - 1) + (yPos - 1)][(CELLROWS - 1) + xPos] = 'X'; // Adds 'X' to the top of the position (yPos - 1)
                break;
            case DOWN:
                labMap[(CELLCOLS - 1) + (yPos + 1)][(CELLROWS - 1) + xPos] = 'X'; // Adds 'X' to the bottom of the position (yPos + 1)
                break;
            case LEFT:
                labMap[(CELLCOLS - 1) + yPos][(CELLROWS - 1) + (xPos - 1)] = 'X'; // Adds 'X' to the left of the position (xPos - 1)
                break;
            case RIGHT:
                labMap[(CELLCOLS - 1) + yPos][(CELLROWS - 1) + (xPos + 1)] = 'X'; // Adds 'X' to the right of the position (xPos + 1)
                break;
        }
    }

    public boolean checkFreePath(int xPos, int yPos, Cord cord){
        boolean response = false;
        switch(cord) {
            case UP:
                if (labMap[Map.CELLCOLS + (yPos - 1)][Map.CELLROWS + xPos] == 'X') // Check if 'X' to the top of the position (yPos - 1)
                    response = true;
                break;
            case DOWN:
                if (labMap[Map.CELLCOLS + (yPos + 1)][Map.CELLROWS + xPos] == 'X') // Check if 'X' to the top of the position (yPos + 1)
                    response = true;
                break;
            case LEFT:
                if (labMap[Map.CELLCOLS + yPos][Map.CELLROWS + (xPos - 1)] == 'X') // Check if 'X' to the top of the position (xPos - 1)
                    response = true;
                break;
            case RIGHT:
                if (labMap[Map.CELLCOLS + yPos][Map.CELLROWS + (xPos + 1)] == 'X') // Check if 'X' to the top of the position (xPos + 1)
                    response = true;
                break;
        }
        return response;
    }

};

/**
 * example of a basic agent implemented using the java interface library.
 */
public class jClient {

    ciberIF cif;
    Map map;
    int orientation = 0;
    boolean inRotation = false;

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
        double[] sensorAngle = {0, -90, 90, 180};
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
        posToView = new LinkedList<>();

        // read first position
        cif.ReadSensors();
        while(!cif.IsGPSReady());
        initGpsX = (int) (cif.GetX());
        initGpsY = (int) (cif.GetY());

        nextMove = Move.NONE;
        gpsX = 0;
        gpsY = 0;

        while (true) {
            cif.ReadSensors();
            decide();
        }
    }

    private int getGpsX() {
        return (int) (cif.GetX()) - initGpsX;
    }

    private int getGpsY() {
        return (int) (cif.GetY()) - initGpsY;
    }

    private void discoverMap() {
        if ((-3 < compass && compass < 3 && irSensor0 >= 1.3) ||
            (87 < compass && compass < 93 && irSensor1 >= 1.3) ||
            (-93 < compass && compass < -87 && irSensor2 >= 1.3) ||
            ((-177 > compass || compass > 177) && irSensor3 >= 1.3)) {
                myMap.addObstacle(gpsX, gpsY, Cord.RIGHT);
        } else if ((-3 < compass && compass < 3 && irSensor0 < 1.3) ||
            (87 < compass && compass < 93 && irSensor1 < 1.3) ||
            (-93 < compass && compass < -87 && irSensor2 < 1.3) ||
            ((-177 > compass || compass > 177) && irSensor3 < 1.3)) {
            if (nextMove.equals(Move.NONE) && !myMap.checkFreePath(gpsX, gpsY, Cord.RIGHT)) {
                nextMove = Move.RIGHT;
                myMap.addFree(gpsX, gpsY, Cord.RIGHT);
            } else {
                double[] pos = {gpsX + 1, gpsY};
                posToView.add(pos);
            }
        }
        if ((-3 < compass && compass < 3 && irSensor1 >= 1.3) ||
            (87 < compass && compass < 93 && irSensor3 >= 1.3) ||
            (-93 < compass && compass < -87 && irSensor0 >= 1.3) ||
            ((-177 > compass || compass > 177) && irSensor2 >= 1.3)) {
                myMap.addObstacle(gpsX, gpsY, Cord.UP);
        } else if ((-3 < compass && compass < 3 && irSensor1 < 1.3) ||
            (87 < compass && compass < 93 && irSensor3 < 1.3) ||
            (-93 < compass && compass < -87 && irSensor0 < 1.3) ||
            ((-177 > compass || compass > 177) && irSensor2 < 1.3)) {
            if (nextMove.equals(Move.NONE) && !myMap.checkFreePath(gpsX, gpsY, Cord.UP)) {
                nextMove = Move.UP;
                myMap.addFree(gpsX, gpsY, Cord.UP);
            } else {
                double[] pos = {gpsX, gpsY + 1};
                posToView.add(pos);
            }
        }
        if ((-3 < compass && compass < 3 && irSensor2 >= 1.3) ||
            (87 < compass && compass < 93 && irSensor0 >= 1.3) ||
            (-93 < compass && compass < -87 && irSensor3 >= 1.3) ||
            ((-177 > compass || compass > 177) && irSensor1 >= 1.3)) {
                myMap.addObstacle(gpsX, gpsY, Cord.DOWN);
        } else if ((-3 < compass && compass < 3 && irSensor2 < 1.3) ||
            (87 < compass && compass < 93 && irSensor0 < 1.3) ||
            (-93 < compass && compass < -87 && irSensor3 < 1.3) ||
            ((-177 > compass || compass > 177) && irSensor1 < 1.3)) {
            if (nextMove.equals(Move.NONE) && !myMap.checkFreePath(gpsX, gpsY, Cord.DOWN)) {
                nextMove = Move.DOWN;
                myMap.addFree(gpsX, gpsY, Cord.DOWN);
            } else {
                double[] pos = {gpsX, gpsY - 1};
                posToView.add(pos);
            }
        }
        if ((-3 < compass && compass < 3 && irSensor3 >= 1.3) ||
            (87 < compass && compass < 93 && irSensor2 >= 1.3) ||
            (-93 < compass && compass < -87 && irSensor1 >= 1.3) ||
            ((-177 > compass || compass > 177) && irSensor0 >= 1.3)) {
                myMap.addObstacle(gpsX, gpsY, Cord.LEFT);
        } else if ((-3 < compass && compass < 3 && irSensor3 < 1.3) ||
            (87 < compass && compass < 93 && irSensor2 < 1.3) ||
            (-93 < compass && compass < -87 && irSensor1 < 1.3) ||
            ((-177 > compass || compass > 177) && irSensor0 < 1.3)) {
            if (nextMove.equals(Move.NONE) && !myMap.checkFreePath(gpsX, gpsY, Cord.LEFT)) {
                nextMove = Move.LEFT;
                myMap.addFree(gpsX, gpsY, Cord.LEFT);
            } else {
                double[] pos = {gpsX - 1, gpsY};
                posToView.add(pos);
            }
        }

        if (nextMove.equals(Move.NONE)) {
            //apply A*
        }
    }

    public void wander() {
        double angle = 0;

        switch (nextMove){
            case UP:
                angle = 90;
                break;
            case DOWN:
                angle = -90;
                break;
            case LEFT:
                angle = 180;
                break;
            case RIGHT:
                angle = 0;
                break;
            default:
                angle = orientation;
                break;
        }

        if (angle != orientation)
        {
            inRotation = true;
            angle = angle - orientation;
            if (angle == -270)
                angle = 90;
            if (angle == 270)
                angle = -90;

            if (angle != (int)compass)
            {
                double rot = (Math.toRadians(angle) - Math.toRadians(compass));
                //System.out.println(rot);
                if (rot < 0) {
                    if(rot > -0.3)
                        cif.DriveMotors(0.15, -0.15);
                    else
                        cif.DriveMotors(+(rot/2), -(rot/2));
                    //cif.DriveMotors(0.15, -0.15);
                } else {
                    if(rot > 0.3)
                        cif.DriveMotors(-0.15, 0.15);
                    else
                        cif.DriveMotors(-(rot/2), +(rot/2));
                    //cif.DriveMotors(-0.15, 0.15);
                }
            }else{
                inRotation = false;
                orientation = (int)angle;
            }
        }else{
            cif.DriveMotors(0.15, 0.15);
        }
    }

    public void getNextPos() {
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

        nextMove = Move.NONE;
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
        if (cif.IsObstacleReady(3))
            irSensor3 = cif.GetObstacleSensor(3);

        if (cif.IsCompassReady())
            compass = cif.GetCompassSensor();

        if (cif.IsGPSReady()) {
            gpsX = getGpsX();
            gpsY = getGpsY();
        }

        switch (state) {
            case RUN: /* Go */
                if (gpsX == nextPosX && gpsY == nextPosY && nextMove.equals(Move.NONE)) {
                    // draw map
                    discoverMap();

                    System.out.println(irSensor0 + " " + irSensor1 + " " + irSensor2 + " " + irSensor3 + " " + compass);
                    System.out.println(gpsX + " " + gpsY + " - " + nextPosX + " " + nextPosY + " " + nextMove);
                }

                // move
                wander();
                // getNextPos
                if (!inRotation)
                    getNextPos();

                // time out
                if (cif.GetTime() >= 200){
                    state = State.FINISH;
                }
                break;
            case FINISH:
                writeMap();
                exportMap("map.out");
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
        for (char[] cs : myMap.labMap) {
            for (char c : cs) {
                System.out.print(c);
            }
            System.out.println();
        }
    }

    public void exportMap(String fileName) {
        try {
            File myObj = new File(fileName);
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
                FileWriter myWriter = new FileWriter(fileName);
                for (char[] cs : myMap.labMap) {
                    for (char c : cs) {
                        myWriter.write(c);
                    }
                    myWriter.write('\n');
                }
                myWriter.close();
                System.out.println("Successfully wrote to the file.");
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private String robName;
    private double irSensor0, irSensor1, irSensor2, irSensor3, compass;
    private int gpsX, gpsY, initGpsX, initGpsY, nextPosX, nextPosY;
    private State state;
    private Queue<double[]> posToView;
    private MyMap myMap;
    private Move nextMove;
}
