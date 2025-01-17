package com.compomics.coss.controller.decoyGeneration;

import com.compomics.ms2io.model.Peak;
import com.compomics.ms2io.model.Spectrum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 *
 * @author Genet
 */
public class GetDecoySpectrum_RandomPeaks implements Callable<Spectrum> {

    Spectrum spectrum;

    public GetDecoySpectrum_RandomPeaks(Spectrum spec) {
        this.spectrum = spec;
    }

    @Override
    public Spectrum call() throws Exception {
        ArrayList<Peak> peaks = spectrum.getPeakList();    
        ArrayList<Peak> peaks_d = new ArrayList<>();
        
        int peak_size = peaks.size();        
        ArrayList rand_index = new ArrayList();
        for (int i = 0; i < peak_size; i++) {
            rand_index.add(i);
        }
        Collections.shuffle(rand_index);


        Peak pk;
        double newMz=0;
        int index=0;
        for (int i = 0; i< peak_size;i++) {

            index = (int)rand_index.get(i);
            newMz= peaks.get(index).getMz();            
            pk=peaks.get(i);
            pk.setMz(newMz);
            
            peaks_d.add(pk);
            
        }

        this.spectrum.setPeakList(peaks_d);
        //for mgf format
        if(!spectrum.getTitle().equals("") && spectrum.getComment().equals("")){
            spectrum.setTitle(spectrum.getTitle() + " _Decoy");
        }
        //for msp format
        if(spectrum.getTitle().equals("") && !spectrum.getComment().equals("")){
            spectrum.setComment(spectrum.getComment() + " _Decoy");
        }
        
        
        return this.spectrum;

    }
}
