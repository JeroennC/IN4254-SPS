package com.github.dnvanderwerff.lagrandefinale.particle;

/**
 * Created by Jeroen on 24/05/2016.
 */
public class Cell {
    public float x, y, width, height;   // x and y are coordinates of left upper corner of the cell
    private float xCentre, yCentre;
    public int cellNo;

    public Cell(int cellNo, float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.cellNo = cellNo;

        this.xCentre = x + (width/2);
        this.yCentre = y - (height/2);
    }

    public float getXCentre() {
        return this.xCentre;
    }

    public float getYCentre() {
        return this.yCentre;
    }
}
