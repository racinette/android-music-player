package helper;

import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;

/**
 * Imagine you have a set of numbers from 0 to n:
 * {0, 1, 2, 3, 4, ... , n}
 * which is randomly ordered:
 * {14, 1, 0, 23, 7, ...}
 * and you remove/add some numbers from/to the set.
 * Now, there occurs an index shift: removing a 7, 8 becomes 7, 9 becomes 8 and so on.
 * This class helps to efficiently deal with this kind of problem using linear regression.
 */

public class IndexTransformer {

    private LinearRegression regression;
    private Interval [] intervals;

    @Override
    public String toString(){
        return TextUtils.join("; ", intervals);
    }

    // indices are removed or added indices
    // flag is used to notify constructor if they were added or removed
    public IndexTransformer(final int [] indices, final boolean sorted){

        final int [] sortedIndices = new int[indices.length];
        System.arraycopy(indices, 0, sortedIndices, 0, indices.length);

        if (!sorted) Arrays.sort(sortedIndices);

        final int MINUS_INFINITY = Integer.MIN_VALUE;
        final int PLUS_INFINITY = Integer.MAX_VALUE;

        intervals = new Interval[sortedIndices.length + 1];

        // two intervals are special: they go from inf to a number and vice versa
        intervals[0] = new Interval(MINUS_INFINITY, sortedIndices[0]);

        // shift is 0 here because -inf wasn't removed
        intervals[0].setShift(0);

        final int last = intervals.length - 1;
        intervals[last] = new Interval(sortedIndices[sortedIndices.length - 1], PLUS_INFINITY);

        final int [] intervalIndices = new int[intervals.length];

        // create intervals
        int i;
        int j = 1;

        for (i = 0; i < sortedIndices.length - 1; i++){
            intervals[j] = new Interval(sortedIndices[i], sortedIndices[i + 1]);
            intervals[j].setShift(-j);
            // this is because interval by the 0th index is from minus inf to the first removed index
            intervalIndices[i] = j;
            j++;
        }

        intervalIndices[i] = j;
        intervals[last].setShift(-j);

        regression = new LinearRegression(sortedIndices, intervalIndices);
    }

    public int transform(int index){
        // test with parsing approximation and round approximation
        int intervalApproximation = (int)(regression.regress(index));

        // interval approximation correction
        if (intervalApproximation < 0) intervalApproximation = 0;
        final int last = intervals.length - 1;
        if (intervalApproximation > last) intervalApproximation = last;

        return transform(intervalApproximation, index);
    }

    private int transform(int intervalNumber, int index){
        final Interval interval = intervals[intervalNumber];
        final int inclusion = interval.includes(index);

        if (inclusion == 0) return index + interval.getShift();
        else if (inclusion < 0) return transform(intervalNumber - 1, index);
        else return transform(intervalNumber + 1, index);
    }

    private class Interval{
        private int shift;
        private int leftBound;
        private int rightBound;

        private Interval(int leftBound, int rightBound){
            this.leftBound = leftBound;
            this.rightBound = rightBound;
        }

        public int getShift() {
            return shift;
        }

        // returns 0 if i is inside the interval
        // returns a negative number if i is on the left of the interval
        // returns a positive number if i is on the right of the interval
        public int includes(int i){
            if (i < leftBound) return -1;
            else if (i >= rightBound) return 1;
            return 0;
        }

        public void setShift(int shift) {
            this.shift = shift;
        }

        @Override
        public String toString(){
            return "[" + leftBound + ", " + rightBound + ")";
        }
    }
}