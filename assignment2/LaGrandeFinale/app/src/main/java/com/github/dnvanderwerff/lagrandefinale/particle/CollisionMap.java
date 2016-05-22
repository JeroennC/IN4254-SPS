package com.github.dnvanderwerff.lagrandefinale.particle;

/**
 * Created by Jeroen on 21/05/2016.
 */
public class CollisionMap {
    // Every tile represents 10 cm x 10 cm
    public static final int LSHAPE = 1, FLOOR9 = 2;
    public int height, width;
    private boolean[][] map;

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
        map = new boolean[height][width];

        // Top layer
        for (int y = 0; y < 61; y++) {
            for (int x = 0; x < width; x++) {
                if (       (x >= 120 && x < 160)
                        || (x >= 240 && x < 280)
                        || (x >= 520 && x < 560))
                    map[y][x] = true;
            }
        }

        // Hall layer
        for (int y = 61; y < 82; y++) {
            for (int x = 0; x < width; x++) {
                map[y][x] = true;
            }
        }

        // Bottom layer
        for (int y = 82; y < 143; y++) {
            for (int x = 0; x < width; x++) {
                if (       (x >= 147 && x < 160)
                        || (x >= 120 && x < 147 && y > 113)
                        || (x >= 400 && x < 440)
                        || (x >= 560 && x < 600))
                    map[y][x] = true;
            }
        }
    }

    private void initializeLShape() {
        height = 40;
        width = 40;
        map = new boolean[height][width];

        // Top layer
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < width; x++) {
                map[y][x] = true;
            }
        }

        // Bottom layer
        for (int y = 20; y < 40; y++) {
            for (int x = 0; x < width; x++) {
                if (x >= 30)
                    map[y][x] = true;
            }
        }
    }

    public boolean[][] getMap() {
        return map;
    }

    public boolean isValidLocation(double x, double y) {
        // x and y in meters
        int tileX = (int)(x * 10);
        int tileY = (int)(y * 10);
        if (tileX < 0 || tileX >= this.width || tileY < 0 || tileY >= this.height)
            return false;
        return map[tileY][tileX];
    }
}
