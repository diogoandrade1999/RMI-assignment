import java.io.*;
import java.util.*;

/**
 * My Custom Map
 * 
 */
public class MyMap {

    public static final List<Character> BLOCKERS_ALL = Arrays.asList(' ', '-', '|');
    public static final List<Character> BLOCKERS_WALLS = Arrays.asList('-', '|');
    public static final int CELLROWS = 14;
    public static final int CELLCOLS = 28;
    private char[][] labMap;

   /**
    * Map Constructor
    */
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
     * Update map
     * 
     * @param xPos x position on map
     * @param yPos y position on map
     */
    private void updateMap(int xPos, int yPos, char symbol) {
        if (this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + xPos] == ' ')
            this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + xPos] = symbol;
    }

    /**
     * Insert on a map position a obstacle
     * 
     * @param xPos x position on map
     * @param yPos y position on map
     * @param move agent moviment
     */
    public void addObstacle(int xPos, int yPos, Client.Move move) {
        this.updateMap(xPos, yPos, 'X');
        switch (move) {
        case UP:
            this.updateMap(xPos, yPos + 1, '-');
            break;
        case DOWN:
            this.updateMap(xPos, yPos - 1, '-');
            break;
        case LEFT:
            this.updateMap(xPos - 1, yPos, '|');
            break;
        case RIGHT:
            this.updateMap(xPos + 1, yPos, '|');
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
        this.updateMap(xPos, yPos, 'X');
        switch (move) {
        case UP:
            this.updateMap(xPos, yPos + 1, 'X');
            break;
        case DOWN:
            this.updateMap(xPos, yPos - 1, 'X');
            break;
        case LEFT:
            this.updateMap(xPos - 1, yPos, 'X');
            break;
        case RIGHT:
            this.updateMap(xPos + 1, yPos, 'X');
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
            if (this.labMap[(CELLROWS - 1) + (yPos + 1)][(CELLCOLS - 1) + xPos] == 'X')
                return true;
            break;
        case DOWN:
            if (this.labMap[(CELLROWS - 1) + (yPos - 1)][(CELLCOLS - 1) + xPos] == 'X')
                return true;
            break;
        case LEFT:
            if (this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos - 1)] == 'X')
                return true;
            break;
        case RIGHT:
            if (this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos + 1)] == 'X')
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
            if (this.labMap[(CELLROWS - 1) + (yPos + 1)][(CELLCOLS - 1) + xPos] == '-')
                return true;
            break;
        case DOWN:
            if (this.labMap[(CELLROWS - 1) + (yPos - 1)][(CELLCOLS - 1) + xPos] == '-')
                return true;
            break;
        case LEFT:
            if (this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos - 1)] == '|')
                return true;
            break;
        case RIGHT:
            if (this.labMap[(CELLROWS - 1) + yPos][(CELLCOLS - 1) + (xPos + 1)] == '|')
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
        this.updateMap(xPos, yPos, 'X');
        return true;
    }

    /**
     * Export the map for a file
     * 
     * @param fileName file name
     */
    public void exportMap(String fileName) {
        try {
            File file = new File(fileName);

            // create file if does not exist
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
            }

            // file writer
            FileWriter fileWriter = new FileWriter(fileName);

            // write on a file
            for (int i = this.labMap.length - 1; i >= 0; i--) {
                for (char c : this.labMap[i])
                    fileWriter.write(c);
                fileWriter.write('\n');
            }

            // close file writer
            fileWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            System.exit(1);
        }
    }

    @Override
    public String toString() {
        String draw = "";
        for (int i = this.labMap.length - 1; i >= 0; i--) {
            for (char c : this.labMap[i])
                draw += c;
            draw += '\n';
        }
        return draw;
    }
};
