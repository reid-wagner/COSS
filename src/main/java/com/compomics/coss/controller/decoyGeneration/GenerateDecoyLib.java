package com.compomics.coss.controller.decoyGeneration;

import com.compomics.ms2io.IndexKey;
import com.compomics.ms2io.Indexer;
import com.compomics.ms2io.MgfReader;
import com.compomics.ms2io.MgfWriter;
import com.compomics.ms2io.MspReader;
import com.compomics.ms2io.MspWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import com.compomics.ms2io.Peak;
import com.compomics.ms2io.SpectraReader;
import com.compomics.ms2io.SpectraWriter;
import java.io.IOException;

/**
 *
 * @author Genet
 */
public abstract class GenerateDecoyLib{

    protected final File file;
    // protected final UpdateListener lstnr;
    protected File decoyFile;
    protected  SpectraReader rd;
    protected  SpectraWriter wr;
    protected List<IndexKey> indxList;
    protected org.apache.log4j.Logger log;
    

    public GenerateDecoyLib(File f, org.apache.log4j.Logger lg) {
        this.log=lg;
        this.file = f;
       // this.lstnr=lr;
    }
    
    public abstract File generate();

    protected String shuffle(String aaSequence) {

        char[] shuffledSequence = aaSequence.toCharArray();        
        ArrayUtils.shuffle(shuffledSequence);
        return new String(shuffledSequence);
    }

    protected Map<Double, Double> shuffle(LinkedHashMap<Double, Double> spectrum) {

        List<Double> list = new ArrayList<>(spectrum.keySet());
        Collections.shuffle(list);

        Map<Double, Double> shuffleMap = new LinkedHashMap<>();
        list.forEach(k -> shuffleMap.put(k, spectrum.get(k)));
        
        
        return shuffleMap;

    }

    /**
     * reverse amino acid sequence
     *
     * @param aaSequence the amino acid sequence to be reversed
     * @return reversed sequence
     */
    protected String reverse(String aaSequence) {
        char[] reversedSeq = aaSequence.toCharArray();
        
        ArrayUtils.reverse(reversedSeq);
        return (new String(reversedSeq));
    }

    /**
     * reverse the spectrum given
     *
     * @param spectrum the spectrum to be reversed
     * @return reversed spectrum 
     */
    protected Map<Double, Double> reverse(Map<Double, Double>spectrum) {

        Map reversedMap=MapUtils.invertMap(spectrum);
        return reversedMap;

    }
    
        /**
     * reverse the spectrum given as peak list
     *
     * @param peakList the spectrum to be reversed
    
     */
    protected void reverse(ArrayList<Peak> peakList) {

        Collections.reverse(peakList);
    

    }
    
     /**
     * shuffles the spectrum given as peak list
     *
     * @param peakList the spectrum to be reversed
    
     */
    protected void shuffle(ArrayList<Peak> peakList) {

        ArrayList<Double> intensity=new ArrayList<>();
        peakList.stream().forEach((pk) -> {
            intensity.add(pk.getIntensity());
        });
        
        Collections.shuffle(intensity);
        int count=0;
        for(Peak pk:peakList){
            pk.setIntensity(intensity.get(count));
            count++;
        }    
    

    }
    
    /**
     *Configures reader and writer for the library and decoy library
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected void configureReadWriter() throws IOException, ClassNotFoundException{
        String fileName = file.getName().substring(0, file.getName().lastIndexOf("."));
        File indxfile = new File(file.getParent(), fileName + ".idx");
        
        
         if (indxfile.exists()) {

            Indexer indxer = new Indexer();
            indxList = indxer.readFromFile(indxfile);

        } else {

            Indexer gi = new Indexer(file);
            indxList = gi.generate();         

        }

        
        if (file.getName().endsWith("mgf")) {
            decoyFile = new File(file.getParent(), fileName + "_shuffledSeq" + ".mgf");
            rd = new MgfReader(file, indxList);
            wr = new MgfWriter(decoyFile);

        } else if (file.getName().endsWith("msp")) {
            decoyFile = new File(file.getParent(), fileName + "shuffledSeq" + ".msp");
            rd = new MspReader(file, indxList);
            wr = new MspWriter(decoyFile);

        }
        
    }
}
