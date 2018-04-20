package com.compomics.matching;

import com.compomics.featureExtraction.DivideAndTopNPeaks;
import com.compomics.featureExtraction.TopNPeaks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.util.Precision;
import com.compomics.coss.Controller.UpdateListener;
import com.compomics.ms2io.Peak;
import com.compomics.ms2io.Spectrum;
import com.compomics.coss.Model.ComparisonResult;
import java.util.Collections;
import com.compomics.coss.Model.ConfigData;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import uk.ac.ebi.pride.tools.jmzreader.JMzReader;
import uk.ac.ebi.pride.tools.jmzreader.model.*;

/**
 *
 *
 * @author Genet
 */
public class UseMsRoben extends Matching {

    //private final double msRobinScore = 0;
    private final double massWindow = 100;
    private final ConfigData confData;

    boolean stillReading;
    UpdateListener listener;

    String resultType;

    int MsRobinOption;
    int IntensityOption;
    double fragTolerance;
    double precTlerance;
    boolean cancelled = false;
    int TaskCompleted;

    public UseMsRoben(UpdateListener lstner, ConfigData cnfData, String resultType) {
        this.listener = lstner;
        this.resultType = resultType;
        cancelled = false;
        this.TaskCompleted = 0;
        this.confData = cnfData;
    }

    @Override
    public void InpArgs(String... args) {

        this.MsRobinOption = Integer.parseInt(args[0]);
        this.IntensityOption = Integer.parseInt(args[1]);
        this.fragTolerance = Double.parseDouble(args[2]);
        this.precTlerance = Double.parseDouble(args[3]);

    }

    @Override
    public void stopMatching() {

        cancelled = true;

    }

    @Override
    public List<ArrayList<ComparisonResult>> dispatcher(org.apache.log4j.Logger log) {

        List<ArrayList<ComparisonResult>> simResult = new ArrayList<>();
        try {

            this.stillReading = true;
            ArrayBlockingQueue<Spectrum> expspec = new ArrayBlockingQueue<>(10, true);
            ArrayBlockingQueue<ArrayList<Spectrum>> libSelected = new ArrayBlockingQueue<>(10, true);
            TheData data = new TheData(expspec, libSelected);

            DoMatching match1 = new DoMatching(data, resultType, "First Matcher", log);
            //DoMatching match2 = new DoMatching(data, resultType, "Second Matcher", log);
            DataProducer producer1 = new DataProducer(data);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            //executor.execute(match2);
            Future future1 = executor.submit(producer1);
            Future<List<ArrayList<ComparisonResult>>> future = executor.submit(match1);

            future1.get();
            simResult = future.get();

            executor.shutdown();

        } catch (InterruptedException ex) {
            Logger.getLogger(UseMsRoben.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(UseMsRoben.class.getName()).log(Level.SEVERE, null, ex);
        }

        return simResult;
    }

    /**
     * This class creates and holds blocking queues for experimental spectrum
     * and selected library spectrum based on precursor mass. The comparison is
     * done between experimental spectrum against corresponding spectra at the
     * same location in their respective queue.
     *
     */
    private class TheData {

        private BlockingQueue<Spectrum> expSpec = null;
        private BlockingQueue<ArrayList<Spectrum>> selectedLibSpec = null;

        public TheData(ArrayBlockingQueue<Spectrum> expS, ArrayBlockingQueue<ArrayList<Spectrum>> libS) {

            this.expSpec = expS;
            this.selectedLibSpec = libS;

        }

        private void putExpSpec(Spectrum s) throws InterruptedException {
            this.expSpec.put(s);
        }

        private void putLibSpec(ArrayList<Spectrum> s) throws InterruptedException {
            this.selectedLibSpec.put(s);
        }

        private Spectrum pollExpSpec() throws InterruptedException {
            return this.expSpec.poll(1, TimeUnit.SECONDS);
        }

        private ArrayList<Spectrum> pollLibSpec() throws InterruptedException {
            return this.selectedLibSpec.poll(1, TimeUnit.SECONDS);
        }

    }

    /**
     * this class puts spectra that are going to be compared into queue as it is
     * blocking Queue it blocks until there is free space.
     */
    private class DataProducer implements Runnable { //procucer thread

        TheData data;

        public DataProducer(TheData data) {
            this.data = data;

        }

        @Override
        public void run() {
            try {
                Spectrum expSpec = new Spectrum();
                uk.ac.ebi.pride.tools.jmzreader.model.Spectrum jmzSpec;
                double massErrorFraction = precTlerance / 1000000.0;

                if (confData.getExpSpectraIndex() == null && confData.getEbiReader() != null) {

                    /**
                     * if comparison between Decoy database .... allow to take
                     * decoy db reader from configdata*
                     * *****************************************************************************
                     * ******************************************************************************
                     */
                    
                    double mz, intensity;
                    ArrayList<Peak> peakList = new ArrayList<>();  
                    Map map;
                    Iterator entriesIterator;
                    double da_error;
                    
                    JMzReader redr = confData.getEbiReader();
                    Iterator<uk.ac.ebi.pride.tools.jmzreader.model.Spectrum> ebiSpecIterator = redr.getSpectrumIterator();

                    while (ebiSpecIterator.hasNext()) {
                        
                        jmzSpec = ebiSpecIterator.next();
                        expSpec.setPCMass(jmzSpec.getPrecursorMZ());                                              
                        map = jmzSpec.getPeakList();
                        Set entries = map.entrySet();
                        entriesIterator = entries.iterator();

                        while (entriesIterator.hasNext()) {

                            Map.Entry mapping = (Map.Entry) entriesIterator.next();
                            mz = (double) mapping.getKey();
                            intensity = (double) mapping.getValue();
                            peakList.add(new Peak(mz, intensity));
                        }                        
                        expSpec.setPeakList(peakList);
                        peakList.clear();
                        
                        da_error = expSpec.getPCMass() * massErrorFraction;
                        ArrayList libSpec = confData.getLibSpecReader().readPart(expSpec.getPCMass(), da_error);
                        data.putExpSpec(expSpec);
                        data.putLibSpec(libSpec);

                    }

                } else {

                    /**
                     * if comparison between Decoy database .... allow to take
                     * decoy db reader from configdata*
                     * *****************************************************************************
                     * ******************************************************************************
                     */
                    double numTasks = confData.getExpSpectraIndex().size();
                    for (int a = 0; a < numTasks; a++) {

                        expSpec = confData.getExpSpecReader().readAt(confData.getExpSpectraIndex().get(a).getPos());
                        double mass = expSpec.getPCMass();

                        double da_error = mass * massErrorFraction;// (10 * mass) / 1000000.0;
                        ArrayList libSpec = confData.getLibSpecReader().readPart(mass, da_error);

                        data.putExpSpec(expSpec);
                        data.putLibSpec(libSpec);

                    }
                }
            } catch (Exception e) {
                System.out.println(e.toString());

            } finally {
                stillReading = false;
            }

        }
    }

    private class DoMatching implements Callable<List<ArrayList<ComparisonResult>>> {

        String resType = "";
        TheData data;
        final String threadName;
        org.apache.log4j.Logger log;

        public DoMatching(TheData data, String restype, String matcherName, org.apache.log4j.Logger log) {
            this.data = data;
            this.resType = restype;
            this.threadName = matcherName;
            this.log = log;

        }

        double intensity_part = 0, probability_part = 0;

        @Override
        public List<ArrayList<ComparisonResult>> call() {

            List<ArrayList<ComparisonResult>> simResult = new ArrayList<>();

            while (stillReading || (!data.expSpec.isEmpty() && !data.selectedLibSpec.isEmpty())) {

                ArrayList<ComparisonResult> compResult = new ArrayList<>();
                Spectrum sp1 = new Spectrum();
                ArrayList sb = new ArrayList();

                try {

                    if (data.expSpec.isEmpty() || data.selectedLibSpec.isEmpty()) {
                        continue;
                    }

                    sp1 = data.pollExpSpec();
                    sb = data.pollLibSpec();
                } catch (InterruptedException ex) {
                    Logger.getLogger(UseMsRoben.class.getName()).log(Level.SEVERE, null, ex);
                }

                InnerIteratorSync<Spectrum> iteratorSpectra = new InnerIteratorSync(sb.iterator());

                while (iteratorSpectra.iter.hasNext()) {
                    Spectrum sp2 = (Spectrum) iteratorSpectra.iter.next();
                    ComparisonResult res = new ComparisonResult();
                    try {

                        //Computing all topN scores omited as the all 10 picks score more than the others - topN changed by 10
                        //for (int topN = 1; topN < 11; topN++) {
                        TopNPeaks filterA = new DivideAndTopNPeaks(sp1, 10, massWindow);
                        TopNPeaks filterB = new DivideAndTopNPeaks(sp2, 10, massWindow);
                        double probability = (double) 10 / (double) massWindow;
                        ArrayList<Peak> fP_spectrumA = filterA.getFilteredPeaks(),
                                fP_spectrumB = filterB.getFilteredPeaks();
                        double[] results = new double[4];
                        if (fP_spectrumB.size() < fP_spectrumA.size()) {
                            results = prepareData(fP_spectrumA, fP_spectrumB);
                        } else {
                            results = prepareData(fP_spectrumB, fP_spectrumA);
                        }
                        int totalN = (int) results[0],
                                n = (int) results[1];
                        double tmp_intensity_part = results[2];
                        MSRobin object = new MSRobin(probability, totalN, n, tmp_intensity_part, MsRobinOption);
                        double score = object.getScore();
                        Precision.round(score, 2);
                        res.setCharge(sp2.getCharge());
                        res.setPrecMass(sp2.getPCMass());
                        res.setScanNum(sp2.getScanNumber());
                        res.setScore(score);
                        res.setTitle(sp2.getTitle());
                        res.setSpecPosition(sp2.getIndex().getPos());
                        res.setResultType(this.resType);
                        compResult.add(res);
                        intensity_part = object.getIntensity_part();
                        probability_part = object.getProbability_part();
                    } catch (Exception ex) {
                        Logger.getLogger(UseMsRoben.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

                TaskCompleted++;
                log.info("Number of match completed " + Integer.toString(TaskCompleted));
                Collections.sort(compResult, Collections.reverseOrder());
                //only top 5 scores returned 
                if (compResult.size() > 5) {
                    compResult.subList(5, compResult.size()).clear();
                }

                simResult.add(compResult);
                compResult.clear();
            }

            return simResult;
        }

        public double getIntensity_part() {
            return intensity_part;
        }

        public void setIntensity_part(double intensity_part) {
            this.intensity_part = intensity_part;
        }

        public double getProbability_part() {
            return probability_part;
        }

        public void setProbability_part(double probability_part) {
            this.probability_part = probability_part;
        }

        private class InnerIteratorSync<T> {

            private Iterator<T> iter = null;

            public InnerIteratorSync(Iterator<T> aIterator) {
                iter = aIterator;
            }

            public synchronized T next() {
                T result = null;
                if (iter.hasNext()) {
                    result = iter.next();
                }
                return result;
            }
        }

        private double[] prepareData(ArrayList<Peak> filteredExpMS2_1, ArrayList<Peak> filteredExpMS2_2) {

            double[] results = new double[4];
            HashSet<Peak> mPeaks_2 = new HashSet<Peak>(); //matched peaks from filteredExpMS2_2
            double intensities_1 = 0,
                    intensities_2 = 0,
                    explainedIntensities_1 = 0,
                    explainedIntensities_2 = 0;
            double alpha_alpha = 0,
                    beta_beta = 0,
                    alpha_beta = 0;
            boolean is_intensities2_ready = false;
            for (int i = 0; i < filteredExpMS2_1.size(); i++) {
                Peak p1 = filteredExpMS2_1.get(i);
                double mz_p1 = p1.getMz(),
                        intensity_p1 = p1.getIntensity(),
                        diff = fragTolerance,// Based on Da.. not ppm...
                        foundInt_1 = 0,
                        foundInt_2 = 0;
                intensities_1 += intensity_p1;
                Peak matchedPeak_2 = null;
                for (Peak peak_expMS2_2 : filteredExpMS2_2) {
                    double tmp_mz_p2 = peak_expMS2_2.getMz(),
                            tmp_diff = (tmp_mz_p2 - mz_p1),
                            tmp_intensity_p2 = peak_expMS2_2.getIntensity();
                    if (!is_intensities2_ready) {
                        intensities_2 += tmp_intensity_p2;
                    }
                    if (Math.abs(tmp_diff) < diff) {
                        matchedPeak_2 = peak_expMS2_2;
                        diff = Math.abs(tmp_diff);
                        foundInt_1 = intensity_p1;
                        foundInt_2 = tmp_intensity_p2;
                    } else if (tmp_diff == diff) {
                        // so this peak is indeed in between of two peaks
                        // So, just the one on the left side is being chosen..
                    }
                }
                is_intensities2_ready = true;
                if (foundInt_1 != 0 && !mPeaks_2.contains(matchedPeak_2)) {
                    mPeaks_2.add(matchedPeak_2);
                    alpha_alpha += foundInt_1 * foundInt_1;
                    beta_beta += foundInt_2 * foundInt_2;
                    alpha_beta += foundInt_1 * foundInt_2;

                    explainedIntensities_1 += foundInt_1;
                    explainedIntensities_2 += foundInt_2;
                }
            }
            // double dot_score_intensities = calculateDot(filteredExpMS2_1, filteredExpMS2_2);
            int totalN = filteredExpMS2_1.size(),
                    n = mPeaks_2.size();
            double intensityPart = 0;
            if (IntensityOption == 3) {
                //Making sure that not to have NaN due to zero!
                if (n != 0) {
                    intensityPart = calculateIntensityPart(alpha_alpha, beta_beta, alpha_beta);
//                System.out.println(n + "\t" + totalN + "\t" + intensityPart);
                }
            } else {
                intensityPart = calculateIntensityPart(explainedIntensities_1, intensities_1, explainedIntensities_2, intensities_2, IntensityOption);
            }
            results[0] = totalN;
            results[1] = n;
            results[2] = intensityPart;
            return results;
        }

        private double calculateIntensityPart(double explainedIntensities_1, double intensities_1, double explainedIntensities_2, double intensities_2, int intensityOption) {
            double int_part = 0;
            double tmp_part_1 = explainedIntensities_1 / intensities_1,
                    tmp_part_2 = explainedIntensities_2 / intensities_2;
            switch (intensityOption) {
                case 0:
                    int_part = (0.5 * tmp_part_1) + (0.5 * tmp_part_2);
                    break;
                case 1:
                    int_part = tmp_part_1 * tmp_part_2;
                    break;
                case 2:
                    int_part = Math.pow(10, (1 - (tmp_part_1 * tmp_part_2)));
                    break;
                default:
                    break;
            }
            return int_part;
        }

        private double calculateIntensityPart(double alpha_alpha, double beta_beta, double alpha_beta) {
            double intensityPart = alpha_beta / (Math.sqrt(alpha_alpha * beta_beta));
            return intensityPart;
        }

    }
}
