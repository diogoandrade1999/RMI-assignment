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

/**
 * example of a basic agent implemented using the java interface library.
 */
public class jClient {

    ciberIF cif;
    Map map;

    enum State {
        RUN, FINISH
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
        client.cif.InitRobot(robName, pos, host);
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
        myMap = new char[27][55];
        for (int i = 0; i < myMap.length; i++) {
            for (int j = 0; j < myMap.length; j++) {
                myMap[i][j] = '!';
            }
        }
        posToView = new LinkedList<>();

        // read first position
        cif.ReadSensors();
        while(!cif.IsGPSReady());
        initGpsX = (int) (cif.GetX());
        initGpsY = (int) (cif.GetY());

        nextPosX = 14;
        nextPosY = 26;

        while (true) {
            cif.ReadSensors();
            decide();
        }
    }

    private int getGpsX() {
        return (int) (cif.GetX()) - initGpsX + 13;
    }

    private int getGpsY() {
        return (int) (cif.GetY()) - initGpsY + 26;
    }

    private void discoverMap() {
        int moveX = 0, moveY = 0;

        if (-3 < compass && compass < 3) {
            if(irSensor0 > 2) {
                myMap[gpsX++][gpsY] = '|';
            } else {
                // move in front :DEFAULT
                moveX++;
                myMap[gpsX++][gpsY] = ' ';
            }
            if(irSensor1 > 2) {
                myMap[gpsX][gpsY++] = '-';
            } else {
                if (moveX == 0 && moveY == 0)
                    moveY++;
                else {
                    double[] pos = {gpsX, gpsY++};
                    posToView.add(pos);
                }
                myMap[gpsX][gpsY++] = ' ';
            }
            if(irSensor2 > 2) {
                myMap[gpsX][gpsY--] = '-';
            } else {
                if (moveX == 0 && moveY == 0)
                    moveY--;
                else {
                    double[] pos = {gpsX, gpsY--};
                    posToView.add(pos);
                }
                myMap[gpsX][gpsY--] = ' ';
            }
            if(irSensor3 > 2) {
                myMap[gpsX--][gpsY] = '|';
            } else {
                if (moveX == 0 && moveY == 0)
                    moveX--;
                else {
                    double[] pos = {gpsX--, gpsY};
                    posToView.add(pos);
                }
                myMap[gpsX--][gpsY] = ' ';
            }
        } else if (87 < compass && compass < 93) {
            if(irSensor0 > 2) {
                myMap[gpsX][gpsY--] = '-';
            } else {
                // move in front :DEFAULT
                moveY--;
                myMap[gpsX][gpsY--] = ' ';
            }
            if(irSensor1 > 2) {
                myMap[gpsX++][gpsY] = '|';
            } else {
                if (moveX == 0 && moveY == 0)
                    moveX++;
                else {
                    double[] pos = {gpsX++, gpsY};
                    posToView.add(pos);
                }
                myMap[gpsX++][gpsY] = ' ';
            }
            if(irSensor2 > 2) {
                myMap[gpsX--][gpsY] = '|';
            } else {
                if (moveX == 0 && moveY == 0)
                    moveX--;
                else {
                    double[] pos = {gpsX--, gpsY};
                    posToView.add(pos);
                }
                myMap[gpsX--][gpsY] = ' ';
            }
            if(irSensor3 > 2) {
                myMap[gpsX][gpsY++] = '-';
            } else {
                if (moveX == 0 && moveY == 0)
                    moveY++;
                else {
                    double[] pos = {gpsX, gpsY++};
                    posToView.add(pos);
                }
                myMap[gpsX][gpsY++] = ' ';
            }
        } else if (-93 < compass && compass < -87) {
            if(irSensor0 > 2) {
                myMap[gpsX][gpsY++] = '-';
            } else {
                // move in front :DEFAULT
                moveY++;
                myMap[gpsX][gpsY++] = ' ';
            }
            if(irSensor1 > 2) {
                myMap[gpsX--][gpsY] = '|';
            } else {
                if (moveX == 0 && moveY == 0)
                    moveX--;
                else {
                    double[] pos = {gpsX--, gpsY};
                    posToView.add(pos);
                }
                myMap[gpsX--][gpsY] = ' ';
            }
            if(irSensor2 > 2) {
                myMap[gpsX++][gpsY] = '|';
            } else {
                if (moveX == 0 && moveY == 0)
                    moveX++;
                else {
                    double[] pos = {gpsX++, gpsY};
                    posToView.add(pos);
                }
                myMap[gpsX++][gpsY] = ' ';
            }
            if(irSensor3 > 2) {
                myMap[gpsX][gpsY--] = '-';
            } else {
                if (moveX == 0 && moveY == 0)
                    moveY--;
                else {
                    double[] pos = {gpsX, gpsY--};
                    posToView.add(pos);
                }
                myMap[gpsX][gpsY--] = ' ';
            }
        } else if(-177 > compass || compass > 177){
            if(irSensor0 > 2) {
                myMap[gpsX--][gpsY] = '|';
            } else {
                // move in front :DEFAULT
                moveX--;
                myMap[gpsX--][gpsY] = ' ';
            }
            if(irSensor1 > 2) {
                myMap[gpsX][gpsY--] = '-';
            } else {
                if (moveX == 0 && moveY == 0)
                    moveY--;
                else {
                    double[] pos = {gpsX, gpsY--};
                    posToView.add(pos);
                }
                myMap[gpsX][gpsY--] = ' ';
            }
            if(irSensor2 > 2) {
                myMap[gpsX][gpsY++] = '-';
            } else {
                if (moveX == 0 && moveY == 0)
                    moveY++;
                else {
                    double[] pos = {gpsX, gpsY++};
                    posToView.add(pos);
                }
                myMap[gpsX][gpsY++] = ' ';
            }
            if(irSensor3 > 2) {
                myMap[gpsX++][gpsY] = '|';
            } else {
                if (moveX == 0 && moveY == 0)
                    moveX++;
                else {
                    double[] pos = {gpsX++, gpsY};
                    posToView.add(pos);
                }
                myMap[gpsX++][gpsY] = ' ';
            }
        }
    
        if (moveX == 0 && moveY == 0) {
            //apply A*
        }

        nextPosX = gpsX + moveX;
        nextPosY = gpsY + moveY;
    }

    public void wander() {
        int moveX = nextPosX - gpsX - 13;
        int moveY = nextPosY - gpsY - 26;
        double angle = compass;

        // rotate 180
        if ((moveX == 1 && -177 > compass) || 
            (moveX == -1 && (-3 < compass && compass <= 0)) ||
            (moveY == 1 && (-93 < compass && compass < -87))) {
            angle += 180;
            double rot = Math.toRadians(angle);
            while (rot <= 0) {
                if(rot > 0.3)
                    cif.DriveMotors(-0.15, +0.15);
                else
                    cif.DriveMotors(-(rot/2), +(rot/2));
                rot -= 0.3;
            }
        }
        // rotate -180
        else if ((moveX == 1 && compass > 177) || 
            (moveX == -1 && (0 < compass && compass < 3)) ||
            (moveY == -1 && (87 < compass && compass < 93))) {
            angle -= 180;
            double rot = Math.toRadians(angle);
            while (rot <= 0) {
                if(rot > 0.3)
                    cif.DriveMotors(-0.15, +0.15);
                else
                    cif.DriveMotors(-(rot/2), +(rot/2));
                rot -= 0.3;
            }
        }
        // rotate 90
        else if ((moveX == 1 && (-93 < compass && compass < -87)) || 
            (moveX == -1 && (87 < compass && compass < 93)) ||
            (moveY == 1 && (-3 < compass && compass < 3)) ||
            (moveY == -1 && (-177 > compass || compass > 177))) {
            angle += 90;
            double rot = Math.toRadians(angle);
            while (rot <= 0) {
                if(rot > 0.3)
                    cif.DriveMotors(+0.15, -0.15);
                else
                    cif.DriveMotors(+(rot/2), -(rot/2));
                rot -= 0.3;
            }
        }
        // rotate -90
        else if ((moveX == 1 && (87 < compass && compass < 93)) || 
            (moveX == -1 && (-93 < compass && compass < -87)) ||
            (moveY == 1 && (-177 > compass || compass > 177)) ||
            (moveY == -1 && (-3 < compass && compass < 3))) {
                angle -= 90;
            double rot = Math.toRadians(angle);
            while (rot >= 0) {
                if(rot > 0.3)
                    cif.DriveMotors(+0.15, -0.15);
                else
                    cif.DriveMotors(+(rot/2), -(rot/2));
                rot += 0.3;
            }
        }

        if (87 < angle && angle < 93) {
            angle -= 90;
        } else if (-93 < angle && angle < -87) {
            angle += 90;
        } else if (-177 > angle) {
            angle += 180;
        } else if (angle > 177) {
            angle -= 180;
        }

        double rot = Math.toRadians(angle);
        if (rot < 0) {
            cif.DriveMotors(0.15, 0.15 - 2*rot);
        } else if (rot > 0) {
            cif.DriveMotors(0.15 - 2*rot, 0.15);
        } else {
            cif.DriveMotors(0.15, 0.15);
        }
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

        System.out.println(gpsX + " " + gpsY + " - " + nextPosX + " " + nextPosY);

        switch (state) {
        case RUN: /* Go */
            // draw map
            discoverMap();

            // move
            wander();

            // time out
            if (cif.GetTime() >= 5000){
                state = State.FINISH;
                writeMap();
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
        for (char[] cs : myMap) {
            for (char c : cs) {
                System.out.print(c);
            }
            System.out.println();
        }
    }

    private String robName;
    private double irSensor0, irSensor1, irSensor2, irSensor3, compass;
    private int gpsX, gpsY, initGpsX, initGpsY, nextPosX, nextPosY;
    private State state;
    private Queue<double[]> posToView;
    private char[][] myMap;
}
