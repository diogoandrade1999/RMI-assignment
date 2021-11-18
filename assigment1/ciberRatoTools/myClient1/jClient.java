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

        ground = -1;
        lastTime = 0;
        inGround = true;
        countLaps = 0;
        state = State.RUN;
    }

    /**
     * reads a new message, decides what to do and sends action to simulator
     */
    public void mainLoop() {

        while (true) {
            cif.ReadSensors();
            decide();
        }
    }

    public void wander() {
        // rotate with motors going inverse
        if (irSensor0 > 1.3) {
            boolean rLeft = false;
            double rot = 0.15;
            for (int i = 0; i < 3; i++) {
                if (i == 0)
                    if (irSensor1 > irSensor2)
                        rLeft = false;
                    else
                        rLeft = true;
                else if (i == 3)
                    rot = 0.05;

                if (!rLeft)
                    cif.DriveMotors(+rot, -rot);
                else
                    cif.DriveMotors(-rot, +rot);
            }
        }

        // warning fix position
        else if(irSensor1 > 5.0 || irSensor2 > 5.0)
            if (irSensor1 > irSensor2)
                cif.DriveMotors(+0.15, +0.05);
            else
                cif.DriveMotors(+0.05, +0.15);
            
        // fix position
        else if (irSensor1 > 3.3 || irSensor2 > 3.3) {
            if (irSensor1 > irSensor2)
                cif.DriveMotors(+0.15, +0.12);
            else
                cif.DriveMotors(+0.12, +0.15);
        }

        // in front
        else
            cif.DriveMotors(0.15, 0.15);
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

        if (cif.IsGroundReady())
            ground = cif.GetGroundSensor();

        switch (state) {
        case RUN:
            // visit first target
            if (ground == 0) {
                if (!inGround) {
                    countLaps++;
                    double time = cif.GetTime();
                    System.out.println("Lap: " + countLaps + " - Lap Time: " + (time - lastTime));
                    lastTime = time;
                }
                inGround = true;

                // complete goal
                if (countLaps == 10) {
                    state = State.FINISH;
                    break;
                } 
            } else
                inGround = false;

            // move
            wander();

            // time out
            if (cif.GetTime() >= 5000)
                state = State.FINISH;
            break;
        case FINISH:
            cif.Finish();
            System.out.println("Complete " + countLaps + " laps - Total Time: " + lastTime + " - Average Time: " + (lastTime / countLaps));
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

    private String robName;
    private double irSensor0, irSensor1, irSensor2, lastTime;
    private int ground, countLaps;
    private boolean inGround;
    private State state;
};
