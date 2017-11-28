/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2017 Fabian Prasser, Florian Kohlmayer and contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deidentifier.arx.aggregates.classification;

import java.util.ArrayList;
import java.util.List;

import org.apache.mahout.math.Matrix;
import org.apache.mahout.math.OrderedIntDoubleMapping;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.function.DoubleDoubleFunction;
import org.apache.mahout.math.function.DoubleFunction;
import org.apache.mahout.vectorizer.encoders.ConstantValueEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;
import org.deidentifier.arx.DataHandleInternal;
import org.deidentifier.arx.aggregates.ClassificationConfigurationRandomForest;

import smile.classification.RandomForest;

import com.carrotsearch.hppc.IntArrayList;

/**
 * Implements a classifier
 * 
 * @author Fabian Prasser
 */
public class MultiClassRandomForest implements ClassificationMethod {
    
    /**
     * Wrapper around vector to expose the underlying array for compatibility between Mahout and Smile
     * 
     * @author Fabian Prasser
     */
    private static class NBVector implements Vector {

        private final double[] array;
        private NBVector(int size) { array = new double[size]; }
        @Override public double aggregate(DoubleDoubleFunction arg0, DoubleFunction arg1) { throw new UnsupportedOperationException(); }
        @Override public double aggregate(Vector arg0, DoubleDoubleFunction arg1, DoubleDoubleFunction arg2) { throw new UnsupportedOperationException(); }
        @Override public Iterable<Element> all() { throw new UnsupportedOperationException(); }
        @Override public String asFormatString() { throw new UnsupportedOperationException(); }
        @Override public Vector assign(double arg0) { throw new UnsupportedOperationException(); }
        @Override public Vector assign(double[] arg0) { throw new UnsupportedOperationException(); }
        @Override public Vector assign(Vector arg0) { throw new UnsupportedOperationException(); }
        @Override public Vector assign(DoubleFunction arg0) { throw new UnsupportedOperationException(); }
        @Override public Vector assign(Vector arg0, DoubleDoubleFunction arg1) { throw new UnsupportedOperationException(); }
        @Override public Vector assign(DoubleDoubleFunction arg0, double arg1) { throw new UnsupportedOperationException(); }
        @Override public Matrix cross(Vector arg0) { throw new UnsupportedOperationException(); }
        @Override public Vector divide(double arg0) { throw new UnsupportedOperationException(); }
        @Override public double dot(Vector arg0) { throw new UnsupportedOperationException(); }
        @Override public double get(int arg0) { return array[arg0]; }
        @Override public double getDistanceSquared(Vector arg0) { throw new UnsupportedOperationException(); }
        @Override public Element getElement(int arg0) { throw new UnsupportedOperationException(); }
        @Override public double getIteratorAdvanceCost() { throw new UnsupportedOperationException(); }
        @Override public double getLengthSquared() { throw new UnsupportedOperationException(); }
        @Override public double getLookupCost() { throw new UnsupportedOperationException(); }
        @Override public int getNumNonZeroElements() { throw new UnsupportedOperationException(); }
        @Override public int getNumNondefaultElements() { throw new UnsupportedOperationException(); }
        @Override public double getQuick(int arg0) { throw new UnsupportedOperationException(); }
        @Override public void incrementQuick(int arg0, double arg1) { throw new UnsupportedOperationException(); }
        @Override public boolean isAddConstantTime() { throw new UnsupportedOperationException(); }
        @Override public boolean isDense() { throw new UnsupportedOperationException(); }
        @Override public boolean isSequentialAccess() { throw new UnsupportedOperationException(); }
        @Override public Vector like() { throw new UnsupportedOperationException(); }
        @Override public Vector like(int arg0) { throw new UnsupportedOperationException(); }
        @Override public Vector logNormalize() { throw new UnsupportedOperationException(); }
        @Override public Vector logNormalize(double arg0) { throw new UnsupportedOperationException(); }
        @Override public double maxValue() { throw new UnsupportedOperationException(); }
        @Override public int maxValueIndex() { throw new UnsupportedOperationException(); }
        @Override public void mergeUpdates(OrderedIntDoubleMapping arg0) { throw new UnsupportedOperationException(); }
        @Override public double minValue() { throw new UnsupportedOperationException(); }
        @Override public int minValueIndex() { throw new UnsupportedOperationException(); }
        @Override public Vector minus(Vector arg0) { throw new UnsupportedOperationException(); }
        @Override public Iterable<Element> nonZeroes() { throw new UnsupportedOperationException(); }
        @Override public double norm(double arg0) { throw new UnsupportedOperationException(); }
        @Override public Vector normalize() { throw new UnsupportedOperationException(); }
        @Override public Vector normalize(double arg0) { throw new UnsupportedOperationException(); }
        @Override public Vector plus(double arg0) { throw new UnsupportedOperationException(); }
        @Override public Vector plus(Vector arg0) { throw new UnsupportedOperationException(); }
        @Override public void set(int position, double value) { array[position] = value; }
        @Override public void setQuick(int arg0, double arg1) { throw new UnsupportedOperationException(); }
        @Override public int size() { return array.length; }
        @Override public Vector times(double arg0) { throw new UnsupportedOperationException(); }
        @Override public Vector times(Vector arg0) { throw new UnsupportedOperationException(); }
        @Override public Vector viewPart(int arg0, int arg1) { throw new UnsupportedOperationException(); }
        @Override public double zSum() { throw new UnsupportedOperationException(); }
        @Override public NBVector clone() {throw new UnsupportedOperationException(); }
    }

    /** Config */
    private final ClassificationConfigurationRandomForest config;
    /** Encoder */
    private final ConstantValueEncoder                    interceptEncoder;
    /** Instance */
    private RandomForest                                  rm;
    /** Specification */
    private final ClassificationDataSpecification         specification;
    /** Encoder */
    private final StaticWordValueEncoder                  wordEncoder;

    /** Data */
    private List<double[]>                                features = new ArrayList<double[]>();
    /** Data */
    private IntArrayList                                  classes  = new IntArrayList();

    /**
     * Creates a new instance
     * @param specification
     * @param config
     */
    public MultiClassRandomForest(ClassificationDataSpecification specification,
                                  ClassificationConfigurationRandomForest config) {

        // Store
        this.config = config;
        this.specification = specification;
        
        // Prepare encoders
        this.interceptEncoder = new ConstantValueEncoder("intercept");
        this.wordEncoder = new StaticWordValueEncoder("feature");
    }

    @Override
    public ClassificationResult classify(DataHandleInternal features, int row) {
        double[] probabilities = new double[specification.classMap.size()];
        int result = rm.predict(encodeFeatures(features, row), probabilities);
        return new MultiClassRandomForestClassificationResult(result, probabilities, specification.classMap);
    }

    @Override
    public void close() {
        
        // Learn now
        rm = new RandomForest(features.toArray(new double[features.size()][]), classes.toArray(), config.getNumberOfTrees());
        
        // Clear
        features.clear();
        classes.clear();
        features = new ArrayList<double[]>();
        classes = new IntArrayList();
    }

    @Override
    public void train(DataHandleInternal features, DataHandleInternal clazz, int row) {
        // The Random Forst does not support online learning, so we have to cache data
        this.features.add(encodeFeatures(features, row));
        this.classes.add(encodeClass(clazz, row));
    }

    /**
     * Encodes a class
     * @param handle
     * @param row
     * @return
     */
    private int encodeClass(DataHandleInternal handle, int row) {
        return specification.classMap.get(handle.getValue(row, specification.classIndex, true));
    }

    /**
     * Encodes a feature
     * @param handle
     * @param row
     * @return
     */
    private double[] encodeFeatures(DataHandleInternal handle, int row) {

        // Prepare
        NBVector vector = new NBVector(config.getVectorLength());
        interceptEncoder.addToVector("1", vector);
        
        // Special case where there are no features
        if (specification.featureIndices.length == 0) {
            wordEncoder.addToVector("Feature:1", 1, vector);
            return vector.array;
        }
        
        // For each attribute
        int count = 0;
        for (int index : specification.featureIndices) {
            
            // Obtain data
            ClassificationFeatureMetadata metadata = specification.featureMetadata[count];
            String value = handle.getValue(row, index, true);
            Double numeric = metadata.getNumericValue(value);
            if (Double.isNaN(numeric)) {    
                wordEncoder.addToVector("Attribute-" + index + ":" + value, 1, vector);
            } else {
                wordEncoder.addToVector("Attribute-" + index, numeric, vector);
            }
            count++;
        }
        
        // Return
        return vector.array;
    }
}