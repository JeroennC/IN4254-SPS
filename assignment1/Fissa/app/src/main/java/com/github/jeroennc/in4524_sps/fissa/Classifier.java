package com.github.jeroennc.in4524_sps.fissa;

import java.util.ArrayList;
import java.util.List;

// TODO Momenteel wordt alleen accdata geclassified, breidt dit uit naar ook de wifipunten
public class Classifier {
    int k; 						// Nr of neighbours used for kNN algorithm
    List<Feature> training;		// Training set to find kNN

    public Classifier (int k) {
        // Check if value of k is valid (=uneven)
        if (k%2 == 0) {
            // TODO raise an error that an invalid k has been provided
        }

        // Set class attributes
        this.k = k;
        this.training = new ArrayList<Feature>();
    }

    /* Set value of k */
    void setK(int k){
        // Check if value of k is valid (=uneven)
        if (k%2 == 0) this.k = k;
    }

    /* Add training data to the classifier */
    void trainClassifier(List<Feature> training) {
        (this.training).addAll(training);
    }

    /* Reset (remove) training data of classifier */
    void resetClassifier() {
        this.training = new ArrayList<Feature>();
    }

    /*	Returns the label corresponding to a value, using the kNN method */
    String classify(double value) {
        int k = this.k;
        List<Feature> training = this.training;

        // Make arrays that will contain the labels and distances of the k nearest neighbours
        String[] labels = new String[k];
        double[] distances = new double[k];

        int i;
        for (i = 0; i < k; i++) {
            distances[i] = Double.MAX_VALUE;
            labels[i] = "undefined";
        }

        // Compare every datapoint from the features list with the given test value
        for (Feature f : training){

            // If feature is one of the NN, add the feature somewhere in neighbours array
            if (Math.abs(f.value - value) < distances[k-1]) {

                // Find index to place the neighbour at
                i = 0;
                while (Math.abs(f.value - value)  >= distances[i]) {
                    i++;
                }

                // Store the distance and label of the new neighbour in the kNN arrays
                for (int j = k-1; j > i; j-- ) {
                    distances[j] = distances[j-1];
                    labels[j] = labels[j-1];
                }
                distances[i] = Math.abs(f.value - value);
                labels[i] = f.label;
            }
        }

        // Classify value by checking classification of the k nearest neighbours
        int walkcount = 0, stillcount = 0;

        for (i = 0; i < k-1; i++) {
            if (labels[i] == "walk") {
                walkcount++;
            } else if (labels[i] == "still") {
                stillcount++;
            } else {
                // TODO error meegeven dat er niet genoeg neighbours gevonden zijn?
            }
        }

        // Give the label corresponding to the k nearest neighbours
        String label;
        if (walkcount > stillcount) {
            label = "walk";
        } else {
            label = "still";
        }

        // TODO datapunt meteen toevoegen aan de training set?
        return label;
    }

    double testData (List<Feature> test) {
        int correct = 0;
        String res;
        for (Feature f : test) {
            res = this.classify(f.value);
            if (res.equals(f.label)) {
                correct++;
            }
        }
        return correct * 1.0 / test.size();
    }

    /**
     * Randomly splits feature set into a training set and a test set
     */
    static public void SplitFeatureSet(List<Feature> features
            , List<Feature> training, List<Feature> test
            , double testFrac) {
        for (Feature feat : features) {
            if (Math.random() <= testFrac) {
                test.add(feat);
            } else {
                training.add(feat);
            }
        }
    }
}
