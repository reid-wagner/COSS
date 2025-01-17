package com.compomics.coss.controller.matching;

import com.compomics.coss.model.ConfigData;
import com.compomics.ms2io.model.Peak;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Genet
 */
public class CosineSimilarity extends Score {

     /**
     * @param confData
     * @param log
     */
    public CosineSimilarity(ConfigData confData, org.apache.log4j.Logger log) {
        super(confData, log);

    }

    /**
     *
    
     * @param topN Top intense peaks selected from each spectrum
     * @return returns intScore of the comparison
     */
    @Override
    public double calculateScore(ArrayList<Peak> expSpec, ArrayList<Peak> libSpec, int topN, int transform) {

        Map<String, ArrayList<Peak>> map = new TreeMap<>();
        ArrayList<Peak> mPeaksExp;
        ArrayList<Peak> mPeaksLib;
        if (libSpec.size() < expSpec.size()) {
            map = prepareData(expSpec, libSpec);
            mPeaksExp = (ArrayList< Peak>) map.get("Matched Peaks1");
            mPeaksLib = (ArrayList< Peak>) map.get("Matched Peaks2");
      

        } else {

//            double temp = sumTotalIntExp;//swap value if order if spetrua given is reversed
//            sumTotalIntExp = sumTotalIntLib;
//            sumTotalIntLib = temp;

            map = prepareData(libSpec, expSpec);
            mPeaksExp = (ArrayList< Peak>) map.get("Matched Peaks2");
            mPeaksLib = (ArrayList< Peak>) map.get("Matched Peaks1");
     

        }

        matchedNumPeaks = mPeaksExp.size();
        sumMatchedIntExp = getSumIntensity(mPeaksExp);
        sumMatchedIntLib =getSumIntensity(mPeaksLib);

        double intScore=-1;
        try{
            
            intScore=cosineScore(normalizePeaks(mPeaksExp), normalizePeaks(mPeaksLib));// * (mPeaksExp.size()/(double)lenA);
        }catch(ArithmeticException ex){
            System.out.println(ex.toString());
        }
        
        //double finalScore=intScore*matchedNumPeaks;        
        return intScore;
    }

    private double cosineScore(List<Peak> v1, List<Peak> v2) { // parameters vector1 and vector2
        double score = 0;
        if (matchedNumPeaks != 0) {
            double productSum = 0;
            double v1SquareSum = 0;
            double v2SquareSum = 0;

            for (int a = 0; a < matchedNumPeaks; a++) {
                
                productSum += v1.get(a).getIntensity() * v2.get(a).getIntensity(); //summation(vector1*vector2)
                v1SquareSum += v1.get(a).getIntensity() * v1.get(a).getIntensity();//summation of squares of vector1
                v2SquareSum += v2.get(a).getIntensity() * v2.get(a).getIntensity();// summation of squares of vector2       
                
            }
            score = productSum / (Math.sqrt(v1SquareSum) * Math.sqrt(v2SquareSum));

        }
        return score;
    }
}
