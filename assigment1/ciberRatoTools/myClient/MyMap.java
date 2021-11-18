import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * My Custom Map
 * 
 */
public class MyMap {

    public static final int CELLROWS = 14;
    public static final int CELLCOLS = 28;
    private char[][] labMap;

    // Constructor
    public MyMap() {
        this.labMap = new char[CELLROWS * 2 - 1][CELLCOLS * 2 - 1];

        for (int r = 0; r < this.labMap.length; r++)
            Arrays.fill(this.labMap[r], ' ');

        this.labMap[(CELLROWS - 1)][(CELLCOLS - 1)] = 'I';
    }

    public char[][] getLabMap() {
        return this.labMap;
    }

    /**
     * Insert on a map position a obstacle
     * 
     * @param xPos x position on map
     * @param yPos y position on map
     * @param move agent moviment
     */
    public void addObstacle(int xPos, int yPos, Client.Move move) {
        switch (move) {
        case UP:
            this.labMap[(CELLROWS - 1) + (yPos + 1)][(CELLCOLS - 1) + xPos] = '-'; // Adds '-' to the top of the
                                                                                   // position (yPos + 1)
            break;
        case DOWN:
            this.labMap[(CELLROWS - 1) + (yPos - 1)][(CELLCOLS - 1) + xPos] = '-'; // Adds '-' to the bottom of the
                                                                                   // position (yPos - 1)
            break;
        case LEFT:
            this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos - 1)] = '|'; // Adds '|' to the left of the
                                                                                   // position (xPos - 1)
            break;
        case RIGHT:
            this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos + 1)] = '|'; // Adds '|' to the right of the
                                                                                   // position (xPos + 1)
            break;
        default:
            break;
        }
    }

    /**
     * Insert on a map position a free path
     * 
     * @param xPos x position on map
     * @param yPos y position on map
     * @param move agent moviment
     */
    public void addFree(int xPos, int yPos, Client.Move move) {
        if (this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + xPos] == ' ')
            this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + xPos] = 'X';
        switch (move) {
        case UP:
            this.labMap[(CELLROWS - 1) + (yPos + 1)][(CELLCOLS - 1) + xPos] = 'X'; // Adds 'X' to the top of the
                                                                                   // position (yPos + 1)
            break;
        case DOWN:
            this.labMap[(CELLROWS - 1) + (yPos - 1)][(CELLCOLS - 1) + xPos] = 'X'; // Adds 'X' to the bottom of the
                                                                                   // position (yPos - 1)
            break;
        case LEFT:
            this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos - 1)] = 'X'; // Adds 'X' to the left of the
                                                                                   // position (xPos - 1)
            break;
        case RIGHT:
            this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos + 1)] = 'X'; // Adds 'X' to the right of the
                                                                                   // position (xPos + 1)
            break;
        default:
            break;
        }
    }

    /**
     * Check if position on map is a free path
     * 
     * @param xPos x position on map
     * @param yPos y position on map
     * @param move agent moviment
     * @return true if is a free path, otherwise false
     */
    public boolean checkFreePos(int xPos, int yPos, Client.Move move) {
        switch (move) {
        case UP:
            if (this.labMap[(CELLROWS - 1) + (yPos + 1)][(CELLCOLS - 1) + xPos] == 'X') // Check if 'X' to the top of
                                                                                        // the position (yPos + 1)
                return true;
            break;
        case DOWN:
            if (this.labMap[(CELLROWS - 1) + (yPos - 1)][(CELLCOLS - 1) + xPos] == 'X') // Check if 'X' to the bottom of
                                                                                        // the position (yPos - 1)
                return true;
            break;
        case LEFT:
            if (this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos - 1)] == 'X') // Check if 'X' to the left of
                                                                                        // the position (xPos - 1)
                return true;
            break;
        case RIGHT:
            if (this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos + 1)] == 'X') // Check if 'X' to the right of
                                                                                        // the position (xPos + 1)
                return true;
            break;
        default:
            break;
        }
        return false;
    }

    /**
     * Check if position on map is a obstacle
     * 
     * @param xPos x position on map
     * @param yPos y position on map
     * @param move agent moviment
     * @return true if is a obstacle, otherwise false
     */
    public boolean checkObstaclePos(int xPos, int yPos, Client.Move move) {
        switch (move) {
        case UP:
            if (this.labMap[(CELLROWS - 1) + (yPos + 1)][(CELLCOLS - 1) + xPos] == '-') // Check if '-' to the top of
                                                                                        // the position (yPos + 1)
                return true;
            break;
        case DOWN:
            if (this.labMap[(CELLROWS - 1) + (yPos - 1)][(CELLCOLS - 1) + xPos] == '-') // Check if '-' to the bottom of
                                                                                        // the position (yPos - 1)
                return true;
            break;
        case LEFT:
            if (this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos - 1)] == '|') // Check if '|' to the left of
                                                                                        // the position (xPos - 1)
                return true;
            break;
        case RIGHT:
            if (this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos + 1)] == '|') // Check if '|' to the right of
                                                                                        // the position (xPos + 1)
                return true;
            break;
        default:
            break;
        }
        return false;
    }

    /**
     * Check if position on map was mapped
     * 
     * @param xPos x position on map
     * @param yPos y position on map
     * @return true if was mapped, otherwise false
     */
    public boolean ckeckedPos(int xPos, int yPos) {
        if (this.labMap[(CELLROWS - 1) + (yPos + 1)][(CELLCOLS - 1) + xPos] == ' '
                || this.labMap[(CELLROWS - 1) + (yPos - 1)][(CELLCOLS - 1) + xPos] == ' '
                || this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos - 1)] == ' '
                || this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos + 1)] == ' ')
            return false;
        if (this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + xPos] == ' ')
            this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + xPos] = 'X';
        return true;
    }

    /**
     * Export the map for a file
     * 
     * @param fileName file name
     */
    public void exportMap(String fileName) {
        try {
            File myObj = new File(fileName);
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            }
            FileWriter myWriter = new FileWriter(fileName);
            for (int i = this.labMap.length - 1; i >= 0; i--) {
                for (char c : this.labMap[i]) {
                    myWriter.write(c);
                }
                myWriter.write('\n');
            }
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
};
