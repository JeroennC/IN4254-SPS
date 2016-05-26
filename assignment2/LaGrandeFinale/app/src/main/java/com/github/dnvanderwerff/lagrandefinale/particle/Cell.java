package com.github.dnvanderwerff.lagrandefinale.particle;

/**
 * Created by Jeroen on 24/05/2016.
 */
public class Cell {
    public float x, y, width, height;
    public int cellNo;

    public Cell(int cellNo, float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.cellNo = cellNo;
    }
}
