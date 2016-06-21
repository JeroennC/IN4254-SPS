package com.github.dnvanderwerff.lagrandefinale.particle;

/**
 * Contains the data for the entire map. Particles use this to check for collision.
 */
public class CollisionMap {
    // Every tile represents 10 cm x 10 cm
    public static final int LSHAPE = 1, FLOOR9 = 2;
    public int height, width;
    private int[][] map;
    private Cell[] cells;
    private int cellCount;

    public CollisionMap(int maptype) {
        switch (maptype) {
            case LSHAPE:
                initializeLShape();
                break;
            case FLOOR9:
                initialize9thFloor();
                break;
        }
    }

    private void initialize9thFloor() {
        height = 143;
        width = 720;
        map = new int[height][width];

        // Top layer
        for (int y = 0; y < 61; y++) {
            for (int x = 0; x < width; x++) {
                if (x >= 120 && x < 160) {
                    map[y][x] = 17;
                } else if (x >= 240 && x < 280) {
                    map[y][x] = 13;
                } else if  (x >= 520 && x < 560) {
                    map[y][x] = 4;
                }
            }
        }

        // Hall layer
        for (int y = 61; y < 82; y++) {
            for (int x = 0; x < width; x++) {
                if (x < 120)
                    map[y][x] = 19;
                else if (x < 160)
                    map[y][x] = 16;
                else if (x < 200)
                    map[y][x] = 15;
                else if (x < 240)
                    map[y][x] = 14;
                else if (x < 280)
                    map[y][x] = 12;
                else if (x < 320)
                    map[y][x] = 11;
                else if (x < 360)
                    map[y][x] = 10;
                else if (x < 400)
                    map[y][x] = 9;
                else if (x < 440)
                    map[y][x] = 7;
                else if (x < 480)
                    map[y][x] = 6;
                else if (x < 520)
                    map[y][x] = 5;
                else if (x < 560)
                    map[y][x] = 3;
                else if (x < 600)
                    map[y][x] = 2;
                else
                    map[y][x] = 20;
            }
        }

        // Bottom layer
        for (int y = 82; y < 143; y++) {
            for (int x = 0; x < width; x++) {
                if ((x >= 147 && x < 160)
                        || (x >= 120 && x < 147 && y > 113)) {
                    map[y][x] = 18;
                } else if (x >= 400 && x < 440) {
                    map[y][x] = 8;
                } else if (x >= 560 && x < 600) {
                    map[y][x] = 1;
                }
            }
        }

        // Cells
        cellCount = 21;
        cells = new Cell[20];
        cells[0] = new Cell(1, 56, 8.2f, 4, 6.1f);
        cells[3] = new Cell(4, 52, 0, 4, 6.1f);
        cells[7] = new Cell(8, 40, 8.2f, 4, 6.1f);
        cells[12] = new Cell(13, 24, 0, 4, 6.1f);
        cells[16] = new Cell(17, 12, 0, 4, 6.1f);
        cells[17] = new Cell(18, 12, 8.2f, 4, 6.1f);

        // Hallway cells
        cells[1] = new Cell(2, 56, 6.1f, 4, 2.1f);
        cells[2] = new Cell(3, 52, 6.1f, 4, 2.1f);
        cells[4] = new Cell(5, 48, 6.1f, 4, 2.1f);
        cells[5] = new Cell(6, 44, 6.1f, 4, 2.1f);
        cells[6] = new Cell(7, 40, 6.1f, 4, 2.1f);
        cells[8] = new Cell(9, 36, 6.1f, 4, 2.1f);
        cells[9] = new Cell(10, 32, 6.1f, 4, 2.1f);
        cells[10] = new Cell(11, 28, 6.1f, 4, 2.1f);
        cells[11] = new Cell(12, 24, 6.1f, 4, 2.1f);
        cells[13] = new Cell(14, 20, 6.1f, 4, 2.1f);
        cells[14] = new Cell(15, 16, 6.1f, 4, 2.1f);
        cells[15] = new Cell(16, 12, 6.1f, 4, 2.1f);
        cells[18] = new Cell(19, 0, 6.1f, 12, 2.1f);
        cells[19] = new Cell(20, 60, 6.1f, 12, 2.1f);
    }

    private void initializeLShape() {
        height = 40;
        width = 40;
        map = new int[height][width];

        // Top layer
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < width; x++) {
                if (x < 20) {
                    map[y][x] = 1;
                } else {
                    map[y][x] = 2;
                }
            }
        }

        // Bottom layer
        for (int y = 20; y < 40; y++) {
            for (int x = 0; x < width; x++) {
                if (x >= 30)
                    map[y][x] = 3;
            }
        }

        // Cells
        cellCount = 4;
        cells = new Cell[3];
        cells[0] = new Cell(1, 0, 0, 2, 2);
        cells[1] = new Cell(2, 2, 0, 2, 2);
        cells[2] = new Cell(3, 3, 2, 1, 2);
    }

    public int[][] getMap() {
        return map;
    }

    public Cell[] getCells() {
        return cells;
    }

    public boolean isValidLocation(double x, double y) {
        // x and y in meters
        int tileX = (int)(x * 10);
        int tileY = (int)(y * 10);
        if (tileX < 0 || tileX >= this.width || tileY < 0 || tileY >= this.height)
            return false;
        return map[tileY][tileX] != 0;
    }

    public int getCell(double x, double y) {
        // x and y in meters
        int tileX = (int)(x * 10);
        int tileY = (int)(y * 10);
        if (tileX < 0 || tileX >= this.width || tileY < 0 || tileY >= this.height)
            return 0;
        return map[tileY][tileX];
    }

    public int getCellCount() {
        return cellCount;
    }
}
