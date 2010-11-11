/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.analysis.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.ala.spatial.util.Grid;
import org.ala.spatial.util.Layer;
import org.ala.spatial.util.SimpleShapeFile;
import org.ala.spatial.util.TabulationSettings;

/**
 *
 * @author Adam
 */
class LoadedPointsSamplingThread extends Thread {

    LinkedBlockingQueue<Integer> lbq;
    ConcurrentHashMap<String, String[]> output;
    CountDownLatch cdl;
    Layer[] layers;
    double[][] points;

    public LoadedPointsSamplingThread(LinkedBlockingQueue<Integer> lbq_, CountDownLatch cdl_, Layer[] layers_, double[][] points_, ConcurrentHashMap<String, String[]> output_) {
        lbq = lbq_;
        cdl = cdl_;
        layers = layers_;
        points = points_;
        output = output_;
    }

    public void run() {
        setPriority(MIN_PRIORITY);
        try {
            while (true) {
                Integer i = lbq.take();
                try {
                    Layer l = layers[i];
                    String[] sampling;
                    if (l.type.equalsIgnoreCase("environmental")) {
                        sampling = gridFileSampling(l);
                    } else {
                        sampling = shapeFileSampling(l);
                    }
                    output.put(l.name, sampling);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                cdl.countDown();
            }
        } catch (InterruptedException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] gridFileSampling(Layer l) {
        //use random access to grid file if small, so most cases.
        Grid grid = new Grid(TabulationSettings.environmental_data_path + l.name);
        float[] d = grid.getValues(points);
        String[] output = new String[d.length];
        for (int i = 0; i < d.length; i++) {
            output[i] = String.valueOf(d[i]);
        }
        return output;
    }

    private String[] shapeFileSampling(Layer l) {
        SimpleShapeFile ssf = new SimpleShapeFile(TabulationSettings.index_path
                + l.name);
        if (ssf == null) {
            ssf = new SimpleShapeFile(TabulationSettings.environmental_data_path
                    + l.name);
        }

        //dynamic thread count
        int thread_count = points.length / 20000;
        if (thread_count > TabulationSettings.analysis_threads) {
            thread_count = TabulationSettings.analysis_threads;
        } else if (thread_count == 0) {
            thread_count = 1;
        }
        int[] intersection = ssf.intersect(points, thread_count);
        String[] output = new String[intersection.length];
        for (int i = 0; i < intersection.length; i++) {
            output[i] = ssf.getValueString(intersection[i]);
        }
        return output;
    }
}