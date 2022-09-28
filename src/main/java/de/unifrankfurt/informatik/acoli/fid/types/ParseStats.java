package de.unifrankfurt.informatik.acoli.fid.types;

import java.io.Serializable;
import java.util.HashMap;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

/**
 * General purpose evaluation function for counting positive and negative results
 * Usecases :
 * 1) Take samples until thresholdForGood or thresholdForBad is reached
 * 2) Take as many as maxSamples. In this case thresholdForGood or thresholdForBad may not be reached. Use #successfullTries and
 *    #unSuccessfullTries instead for evaluation
 * @author frank
 *
 */
public class ParseStats implements Serializable {
	
	private static final long serialVersionUID = 1146600687267519497L;
	public static final int NO_SAMPLING = 1000000000;	
	private HashMap <String, Integer> successfullTries=new HashMap<String,Integer>();
	private HashMap <String, Integer> unSuccessfullTries=new HashMap <String, Integer>();
	private int thresholdForGood=50;	// absolute number of positive results to indicate a positive volume
	private int thresholdForBad=100;	// absolute number of negative results to indicate a negative volume
	private int maxSamples=200;         // can be used together with hasSamples() to evaluate at most maxSamples results
	private int activationThreshold=100;// only if volumeSize is >= activationThreshold the methods isGood,isBad, etc. are active !
	private int volumeSize=0;			// size of the whole volume (can be #count files, #sum bytes etc.)
	

	
	/**
	 * General purpose evaluation function counts positive and negative results
	 */
	public ParseStats () {}
	
	
	public ParseStats (int volumeSize) {
		this.volumeSize = volumeSize;
	}
	
	
	/**
	 * Keep track of succesfull/unsucessfull examples and provides evaluation methods for the sampling process
	 * <p>
	 * Parameter usage example :<br>
	 * Take at max. 1000 samples and stop sampling afterwards => maxSamples=1000<br>
	 * After 50 negative samples for a certain key the sampling process should stop => thresholdForBad=50<br>
	 * After 100 positive samples for a certain key the sampling process should stop => thresholdForGood=100<br>
	 * If the volume from where samples should be taken is relatively small (< 500) do not sample at all => activationThreshold=500<br>
	 * If the volume has 10000 items => volumeSize=10000<br>
	 * The samplingIsDecided(key) takes control of the duration of the sampling process<br>
	 * 
	 * @param thresholdForGood Good samples to be taken for key (The key is the argument of the samplingIsDecided(key) function)
	 * @param thresholdForBad Bad samples to be taken for key   (The key is the argument of the samplingIsDecided(key) function)
	 * @param activationThreshold If the size of the resource is smaller than the activationThreshold no sampling will take place
	 * @param maxSamples (set -1 for unlimited samples) Maximum number of samples to be taken (for all keys)
	 * @param volumeSize The size of the set were samples are taken from  
	 */
	public ParseStats (int thresholdForGood, int thresholdForBad, int activationThreshold, int maxSamples, int volumeSize) {
		this.thresholdForGood = thresholdForGood;
		this.thresholdForBad = thresholdForBad;
		this.activationThreshold = activationThreshold;
		this.volumeSize = volumeSize;
		this.maxSamples = maxSamples;
		if (maxSamples == -1) {
			this.maxSamples = 1000000000;
		}
		
		Utils.debug("Using sampling parameters :");
		Utils.debug("Activation threshold : "+activationThreshold+"  volumeSize : "+volumeSize);
		Utils.debug("thresholdForGood : "+thresholdForGood+"  thresholdForBad : "+thresholdForBad);
		Utils.debug("maxSamples : "+maxSamples);
	}
	
	
	/**
	 * The sampling process is finished iff
	 * A) At least 'thresholdForGood' samples where found
	 * B) At least 'thresholdForBad' samples where found
	 * C) At most 'maxSamples' have been taken
	 * @return true iff 'thresholdForBad' negative samples OR 'thresholdForGood' positive samples where found OR maxSamples have been taken
	 */
	public boolean samplingIsDecided(String key) {
		
		//Utils.debug("Activation threshold : "+activationThreshold+"  volumeSize : "+volumeSize);
		
		// This rule deactivates sampling completely if the amount of data is below the activation threshold
		if (activationThreshold > volumeSize) return false;
		
		if (isGood(key)) {
			goodOutput(key);
			return true;
		}
		
		if (isBad(key)) {
			badOutput(key);
			return true;
		}
		
		if (!hasSamplesLeft()) {
			maxSamplesOutput();
			return true;
		}

		return false;
	}
	
	
	/**
	 * Count a good example (for key)
	 * @param key
	 * @return
	 */
	public boolean incrGood(String key) {
		if (successfullTries.containsKey(key)) {
			successfullTries.put(key, successfullTries.get(key)+1);
		} else {
			successfullTries.put(key,1);
		}
		return isGood();
	}
	
	/**
	 * Count a bad example (for key)
	 * @param key
	 * @return
	 */
	public boolean incrBad(String key) {
		if (unSuccessfullTries.containsKey(key)) {
			unSuccessfullTries.put(key, unSuccessfullTries.get(key)+1);
		} else {
			unSuccessfullTries.put(key,1);
		}
		return isBad();
	}

	/** At least 'thresholdForGood' positive examples have been found
	 * 
	 * @return true if good else false
	 */
	public boolean isGood() {
				
		int sum=0;
		for (int x : successfullTries.values()) {
			sum+=x;
		}
		if (sum > thresholdForGood) {
			return true;
		}
		else {
			return false;
		}
	}

	
	/** At least 'thresholdForGood' positive examples have been found (for key)
	 * @return true if good else false
	 */
	public boolean isGood(String key) {
		
		if (!successfullTries.containsKey(key)) return false; 

		if (successfullTries.get(key) >= thresholdForGood) {
			return true;
		} else {
			return false;
		}
	}
	
	/** At least 'thresholdForBad' negative examples have been found.
	 * 
	 * 
	 * @return true if bad else false
	 */
	public boolean isBad() {
				
		int sum=0;
		for (int x : unSuccessfullTries.values()) {
			sum+=x;
		}
		if (sum > thresholdForBad) {
			return true;
		} else {
			return false;
		}
	}
	
	
	/** At least 'thresholdForBad' negative examples have been found (for key)
	 * 
	 * @return true if bad else false
	 */
	public boolean isBad(String key) {
		
		if (!unSuccessfullTries.containsKey(key)) return false;

		if (unSuccessfullTries.get(key) > thresholdForBad) {
			return true;
		} else {
			return false;
		}
	}
	
	
	/** 
	 * 
	 * @return true if (sampleCount for that key) < 'maxSamples'
	 */
	public boolean hasSamplesLeft(String key) {
		int success = 0;
		int noSuccess = 0;
		if (successfullTries.containsKey(key)) {
			success = successfullTries.get(key);
		}
		if (unSuccessfullTries.containsKey(key)) {
			noSuccess = unSuccessfullTries.get(key);
		}
		if (success + noSuccess < maxSamples) {
			return true;
		} else {
			return false;
		}	
	}

	
	/** 
	 * 
	 * @return true if sampleCount < 'maxSamples'
	 */
	public boolean hasSamplesLeft() {
		
		Utils.debug("good : "+getGoodSum()+ " bad : "+getBadSum()+ " maxSamples "+maxSamples);
		if (getGoodSum()+getBadSum() < maxSamples)
			return true;
			else return false;
	}
	
	/**
	 * Return overall number of successful tries
	 * @return successful tries
	 */
	public int getSuccessfullTries() {
		return getGoodSum();
	}

	/**
	 * Return overall number of unsuccessful tries
	 * @return unsuccessful tries
	 */
	public int getUnSuccessfullTries() {
		return getBadSum();
	}
	
	
	private int getGoodSum() {
		int sum=0;
		for (int x : successfullTries.values()) {
			sum+=x;
		}
		return sum;
	}
	
	
	private int getBadSum() {
		int sum=0;
		for (int x : unSuccessfullTries.values()) {
			sum+=x;
		}
		return sum;
	}
	

	/**
	 * Get value of thresholdForGood
	 * @return
	 */
	public float getThresholdForGood() {
		return thresholdForGood;
	}

	/**
	 * Set value of thresholdForGood
	 * @param thresholdForGood
	 */

	public void setThresholdForGood(int thresholdForGood) {
		this.thresholdForGood = thresholdForGood;
	}

	/**
	 * Get value of maxSamples
	 * @return maxSamples
	 */
	public int getMaxSamples() {
		return maxSamples;
	}


	public void setMaxSamples(int maxSamples) {
		this.maxSamples = maxSamples;
	}
	
	/**
	 * Reset successfull tries and unsuccessfull tries
	 */
	public void reset() {
		successfullTries.clear();
		unSuccessfullTries.clear();
	}

	/**
	 * Get value of activationThreshold
	 * @return activationThreshold
	 */
	public int getActivationThreshold() {
		return activationThreshold;
	}

	/**
	 * Set value of activationThreshold
	 * @param activationThreshold
	 */
	public void setActivationThreshold(int activationThreshold) {
		this.activationThreshold = activationThreshold;
		Utils.debug("ParseStats : set activation threshold = "+activationThreshold);
	}

	/**
	 * Get value of volume size
	 * @return
	 */
	public int getVolumeSize() {
		return volumeSize;
	}

	/**
	 * Is the size of the set of evaluations (e.g. testing 100000 files) requires setting volumeSize to 
	 * 100000. In case volumeSize < minSamples the set will be considered as marginal s.t. isGood() will
	 * always return true and isBad() will always return false.
	 * @param volumeSize
	 */
	public void setVolumeSize(int volumeSize) {
		this.volumeSize = volumeSize;
	}
	
	/**
	 * For a different output override this function in an extended class
	 * @param key
	 */
	private void badOutput(String key) {
		Utils.debug("skipping useless folder "+ key +" with "+this.volumeSize+" files ");
		Utils.debug("after "+getUnSuccessfullTries()+" unsuccessfull\n"
				+ "and "+getSuccessfullTries()+" successfull tries !");
	}
	
	/**
	 * For a different output override this function in an extended class
	 * @param key
	 */
	private void goodOutput(String key) {
		Utils.debug("skipping folder : "+ key);
		//Utils.debug("skipping successful folder "+ key +" with "+this.volumeSize+" files ");
		Utils.debug("after "+getSuccessfullTries()+" successfull samples !");
	}
	
	/**
	 * For a different output override this function in an extended class
	 */
	private void maxSamplesOutput() {
		Utils.debug("skipping resource because maximal amount of samples ("+maxSamples+") were taken !");
	}	
}
