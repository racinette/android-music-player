package helper;

public class LinearRegression {
    // coefficients of the linear regression
    private double b0;
    private double b1;

    public LinearRegression(final int [] xValues, final int [] yValues){

        double xMean = 0.d;
        double yMean = 0.d;

        for (int i = 0; i < xValues.length && i < yValues.length; i++){
            xMean += xValues[i];
            yMean += yValues[i];
        }

        xMean = xMean / xValues.length;
        yMean = yMean / yValues.length;

        double numerator = 0.d;
        double denominator = 0.d;

        for (int i = 0; i < xValues.length && i < yValues.length; i++){
            final double xMinusMean = xValues[i] - xMean;

            numerator += xMinusMean * (yValues[i] - yMean);
            denominator += xMinusMean * xMinusMean;
        }

        b1 = numerator / denominator;

        b0 = yMean - b1 * xMean;
    }

    public double regress(double x){
        return b0 + b1 * x;
    }
}

