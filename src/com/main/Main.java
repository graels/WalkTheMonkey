package com.main;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/**
 *  PROBLEM:
 *
 * There is a monkey which can walk around on a planar grid. The monkey can move one space at a time left, right, up 
 * or down. That is, from (x, y) the monkey can go to (x+1, y), (x-1, y), (x, y+1), and (x, y-1). Points where the 
 * sum of the digits of the absolute value of the x coordinate plus the sum of the digits of the absolute value of 
 * the y coordinate are lesser than or equal to 25 are accessible to the monkey. For example, the point (59, 79) is
 * inaccessible because 5 + 9 + 7 + 9 = 30, which is greater than 25. Another example: the point (-5, -7) is 
 * accessible because abs(-5) + abs(-7) = 5 + 7 = 12, which is less than 25.  How many points can the monkey
 * access if it starts at (0, 0), including (0, 0) itself? 
 * 
 * SOLUTION: 
 * 
 * The monkey can reach 1033841 points.
 * 
 * Note: 
 *     Per observation, each quadrant contains the same number of points.  An optimization was made to simply
 *     compute the number of points in a single quadrant, multiplying the result by 4 and add the origin to
 *     the product.
 *     
 */
 
public class Main extends Activity implements View.OnClickListener
{

    private static final int PROBLEM_SUM = 25;
    private static final Boolean OPTIMIZE = true;

    /*
     * Segment the computation into quadrants to minimize memory requirements.  
     * Starting point for each segment should be within 1 of the origin and allow traversal 
     * of the set in column major order. Don't forget to account for the origin in the total.
     */ 
    private final Quadrant[] quadrants = {
        new Quadrant("Quadrant 1", 1, 0, 1, 1),
        new Quadrant("Quadrant 2", 0, 1, -1, 1),
        new Quadrant("Quadrant 3", -1, 0, -1, -1),
        new Quadrant("Quadrant 4", 0, -1, 1, -1)
    };

    private int mPointTotal = 0;
    private ProgressDialog mDialog;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        findViewById(R.id.solve_button).setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        new countPoints().execute(PROBLEM_SUM);
        mDialog = ProgressDialog.show(this, null, getString(R.string.solution_progress_label));
        v.setVisibility(View.GONE);
    }

    /**
     * An immutable object that represents a quadrant on a grid:
     *
     *  xStart - starting x position of the quadrant (positive or negative)
     *  yStart - starting y position of the quadrant (positive or negative)
     *  xIncrement - x step offset (positive or negative)
     *  yIncrement - y step offset (positive or negative)
     *
     */
    private final class Quadrant {
        
        private final String name;
        private final int xStart;
        private final int yStart;
        private final int xIncrement;
        private final int yIncrement;

        public Quadrant(String name, int xStart, int yStart, int xIncrement, int yIncrement) {
            this.name = name;
            this.xStart = xStart;
            this.yStart = yStart;
            this.xIncrement = xIncrement;
            this.yIncrement = yIncrement;
        }
        
        public int getStartX()
        {
            return xStart;
        }

        public int getStartY()
        {
            return yStart;

        }

        public int getIncrementX()
        {
            return xIncrement;
        }

        public int getIncrementY()
        {
            return yIncrement;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    
    /**
     * Compute the sum of the digits of the absolute value of the supplied number/s
     * 
     * @param value - the value to sum
     * @return - integer sum of digits
     */
    private static int sum(int value) {
        int rem;
        int sum = 0;
        int num = Math.abs(value);

        while (num > 0) {
            rem = num % 10;
            num = num / 10;
            sum = sum + rem;
        }

        return sum;
    }

    /**
     * Compute the sum the digits in the supplied coordinate
     *
     * @param x -   the x coordinate value to sum
     * @param y -   the y coordinate value to sum
     * @return - integer sum of digits
     */
    private static int sum(int x, int y) {
        return sum(x) + sum(y);
    }
    
    /**
     * Determine if the point described by (x,y) is unique (not currently in the map) and connected to
     * an adjoining point in the map in the xOffset or yOffset direction.  Note that if the point is
     * within 1 unit of an axis, it is considered to be connected to the adjacent point on the axis.
     *
     * @param x - x position of point to analyze
     * @param y - y position of point to analyze
     * @param xOffset - horrizontal direction to connection
     * @param yOffset - vertical direction to connection
     * @param map - map of connected points
     * @return - true if the point is not already in the map and it is connected in the
     *           horrizontal or vertical to another point in the map, of false
     */
    private static boolean pointConnected(int x, int y, int xOffset, int yOffset, Map<Point, Boolean> map) {
        boolean connected = false;
         
        if (map != null) {
            if (!map.containsKey(new Point(x, y))) {
                if (xOffset != 0 && map.containsKey(new Point(x - xOffset, y))) {
                    connected = true; // connect to the point in the map
                }
                else if (yOffset != 0 && map.containsKey(new Point(x, y - yOffset))) {
                    connected = true; // connect to the point in the map
                }
                else if (x - xOffset == 0 && !map.containsKey(new Point(0, y))) {
                    connected = true; // connect to the x axis
                }
                else if (y - yOffset == 0 && !map.containsKey(new Point(x, 0))) {
                    connected = true; // connect to the y axis
                }
            }
        }    
        return connected;
    }

    /**
     * Traverse the supplied quadrant in column major order, accumulating the total
     * number of "connected" points who's absolute digit sum is less than or equal to maxSum.
     *
     * @param quadrant
     *          xStart - starting x position (positive or negative) of the quadrant
     *          yStart - starting y position (positive or negative) of the quadrant
     *          xIncrement - x step offset (positive or negative)
     *          yIncrement - y step offset (positive or negative)
     * @param maxPoint - absolute maximum (x and y) point value
     * @param maxSum - absolute maximum sum of the digits in an x and y coordinate
     * @param map - map of existing points in the quadrant
     * @return - number of points in the set that sum to less than or equal to maxSum
     */
    private static int mapPoints(Quadrant quadrant, int maxSum, int maxPoint, Map<Point, Boolean> map) {
        int validCount;
        int invalidCount = 0;
        int total = 0;
        int skipThreshold = 99; // TODO - works for 25, should compute based on maxSum.

        for (int x = quadrant.getStartX(); Math.abs(x) <= maxPoint; x += quadrant.getIncrementX()) {
            validCount = 0;
            for (int y = quadrant.getStartY(); Math.abs(y) <= maxPoint; y += quadrant.getIncrementY()) {
                if (sum(x, y) <= maxSum) {
                    if (pointConnected(x, y, quadrant.getIncrementX(), quadrant.getIncrementY(), map)) {
                        map.put(new Point(x, y), true);
                        validCount++;
                        invalidCount = 0;
                    }
                }
                else {
                    invalidCount++;
                    if (invalidCount > skipThreshold) break; // optimization
                }
            }
            
            total += validCount;
        }

        return total;
    }
    
    /**
     * Find the last number, in a consecutive sequence from 0, who's absolute digit sum is less
     * than or equal to the value of that supplied.
     * 
     * For example:
     * 
     *   The limit of 5 = 5
     *   The limit of 10 = 28 (2 + 8) because 29 (2 + 9) ends the sequence
     *   the limit of 25 = 898
     *   
     * @param value - maximum sum
     * @return - max positive int that sums to the supplied value
     */
    private static int pointLimit(int value) {
        int limit = 0;
        int num = Math.abs(value);

        while (sum(limit + 1) <= num) {
           limit++;
        }

        return limit;
    }

    /**
     * Offload to a background task
     */
    private class countPoints extends AsyncTask<Integer, Void, Void> {
        
        @Override
        protected Void doInBackground(Integer... sums) {
            Map<Point, Boolean> map = new HashMap<Point, Boolean>();
            
            for (Integer maxSum: sums) {
                mPointTotal = 1;  // DON'T FORGET TO INCLUDE THE ORIGIN TO THE FINAL COUNT.
                int maxPoint = pointLimit(maxSum);

                if (OPTIMIZE) {
                    map.clear();
                    mPointTotal += mapPoints(quadrants[0], maxSum, maxPoint, map) * 4;
                }
                else {
                    for (Quadrant quadrant : quadrants) {
                        map.clear();
                        mPointTotal += mapPoints(quadrant, maxSum, maxPoint, map);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mDialog.dismiss();
            TextView solution = (TextView)findViewById(R.id.solution_view);
            solution.setText(getString(R.string.solution_string) + "  " + mPointTotal + ".");
            solution.setVisibility(View.VISIBLE);
        }
    }

}
