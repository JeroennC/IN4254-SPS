package com.github.dnvanderwerff.lagrandefinale.util;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Based on http://www.thousand-thoughts.com/2012/03/android-sensor-fusion-tutorial/
 *
 */
public class DirectionExtractor implements SensorEventListener {
    private static final float ALPHA = 0.8f;
    private static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    public static final float FILTER_COEFFICIENT = 0.98f;

    /* Sensors */
    private Sensor gravity, magnetometer, gyroscope;
    private Sensor accelerometer;

    /* Variables */
    private boolean initState = true;
    private float timestamp;

    // linear acceleration values
    private float[] accelValues = new float[3];
    private float[] earthAccelValues = new float[2];

    // angular speeds from gyro
    private float[] gyroscopeValues = new float[3];

    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];

    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];

    // magnetic field vector
    private float[] magneticValues = new float[3];

    // accelerometer vector
    private float[] gravityValues = new float[3];

    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];

    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];

    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];

    /* Output variables */
    private int degreeNorth = 0;
    private void setDegreeNorth(int deg) { degreeNorth = deg; }
    public int getDegreeNorth() { return degreeNorth; }

    private float radianNorth = 0.0f;
    private void setRadianNorth(float rad) { radianNorth = rad; }
    public float getRadianNorth() { return radianNorth; }

    private int degreeMe = 0;
    private void setDegreeMe(int deg) { degreeMe = deg; }
    public int getDegreeMe() { return degreeMe; }

    private float radianMe = 0.0f;
    private void setRadianMe(float rad) { radianMe = rad; }
    public float getRadianMe() { return radianMe; }

    /* Constructor */
    public DirectionExtractor(SensorManager sensorManager) {
        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;


        /* Get Sensors */
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (gravity == null)
            gravity = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_ACCELEROMETER:
                gravityValues = lowPass(event.values.clone(), gravityValues, ALPHA);
                calculateAccMagOrientation();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticValues = lowPass(event.values.clone(), magneticValues, ALPHA);
                break;
            case Sensor.TYPE_GYROSCOPE:
                processGyroscope(event);
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                // Not being used
                accelValues = lowPass(event.values.clone(), accelValues, ALPHA);
                calculatePlacementOffset();
                break;
        }

         /* Set Outputs */
        //radianNorth = accMagOrientation[0];
        //degreeNorth = (int)(Math.toDegrees(accMagOrientation[0])+360)%360;
        radianNorth = gyroOrientation[0] + (float)(0.5 * Math.PI - 1 / 6 * Math.PI); /* Plus .5PI for phone @ landscape */
        degreeNorth = (int)(Math.toDegrees(gyroOrientation[0])+360 + 90 - 30 /* Because of phone landscape */)%360;
    }

    // Use linear acceleration and the true north to deduce which direction the user is walking in
    // This doesn't really work.. yet?
    private void calculatePlacementOffset() {
        // It also needs the gravity and magnetic values orientation
        float[] inv = new float[9];
        float[] accelEarth = new float[4];

        // Invert the rotation matrix
        //android.opengl.Matrix.invertM(inv, 0, rotationMatrix, 0);
        // Get the accelerator in earths axes
        matrixVectorMultiplication(accelEarth, rotationMatrix, accelValues);

        // We only care about x and y, the first 2 indices
        float minThreshold = .5f;

        // If neither x and y are interesting, dont change anything
        if (Math.abs(accelEarth[0]) < minThreshold
                && Math.abs(accelEarth[1]) < minThreshold)
            return;

        // If it is interesting, update
        if (Math.abs(accelEarth[0]) > minThreshold) {
            earthAccelValues[0] = accelEarth[0];
            // Make sure it is still neg/pos, but not too afflicted by using low pass
            earthAccelValues[1] = .8f * earthAccelValues[1] + .2f * accelEarth[1];
        }
        if (Math.abs(accelEarth[1]) < minThreshold) {
            earthAccelValues[1] = accelEarth[1];
            // Make sure it is still neg/pos, but not too afflicted by using low pass
            earthAccelValues[0] = .8f * earthAccelValues[0] + .2f * accelEarth[0];
        }

        // Calculate the new degree
        radianMe = (float)Math.atan2(earthAccelValues[1], earthAccelValues[0]);
        degreeMe = (int)(Math.toDegrees(radianMe)+360)%360;
    }

    // calculates orientation angles from accelerometer and magnetometer output
    private void calculateAccMagOrientation() {
        if(SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, magneticValues)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    // This function is borrowed from the Android reference
    // at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
    // It calculates a rotation vector from the gyroscope angular speed values.
    private void getRotationVectorFromGyro(float[] gyroValues,
                                           float[] deltaRotationVector,
                                           float timeFactor)
    {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based orientation into gyroOrientation.
    private void processGyroscope(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;

        // initialisation of the gyroscope based rotation matrix
        if(initState) {
            float[] initMatrix;
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyroscopeValues, 0, 3);
            getRotationVectorFromGyro(gyroscopeValues, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);

        /* Calculate azimuth, pitch and roll */
        float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
        // azimuth
        if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
            fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
            fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
            fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
            fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
        }
        else {
            fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
        }

        // pitch
        if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
            fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
            fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
            fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
            fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
        }
        else {
            fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
        }

        // roll
        if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
            fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
            fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
            fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
            fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
        }
        else {
            fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
        }

        // overwrite gyro matrix and orientation with fused orientation
        // to comensate gyro drift
        gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
        System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    private void matrixVectorMultiplication(float[] result, float[] A, float[] B) {
        result[0] = A[0] * B[0] + A[1] * B[1] + A[2] * B[2];
        result[1] = A[3] * B[0] + A[4] * B[1] + A[5] * B[2];
        result[2] = A[6] * B[1] + A[7] * B[1] + A[8] * B[2];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {  }

    public void registerListeners(SensorManager sensorManager) {
        //sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    /* Use a low-pass filter to avoid random high values casued by noise. Taken from https://www.built.io/blog/2013/05/applying-low-pass-filter-to-android-sensors-readings/ with slight adaptation */
    private float[] lowPass(float[] input, float[] output, float alpha) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = alpha * output[i] + (1 - alpha) * input[i];
        }
        return output;
    }
}
