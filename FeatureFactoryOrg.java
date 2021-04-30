
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Scanner;

public class FeatureFactory {

	/**
	 * Add any necessary initialization steps for your features here. Using this
	 * constructor is optional. Depending on your features, you may not need to
	 * intialize anything.
	 */

	public FeatureFactory() {

	}

	/**
	 * Words is a list of the words in the entire corpus, previousLabel is the label
	 * for position-1 (or O if it's the start of a new sentence), and position is
	 * the word you are adding features for. PreviousLabel must be the only label
	 * that is visible to this method.
	 */
	public  List<String> ngrams(int n, String str) {
		List<String> ngrams = new ArrayList<String>();
		for (int i = 0; i < str.length() - n + 1; i++)
			// Add the substring or size n
			ngrams.add(str.substring(i, i + n));
			// In each iteration, the window moves one step forward
			// Hence, each n-gram is added to the list
	
		return ngrams;
	  }
	
	private List<String> computeFeatures(List<String> words, String previousLabel, int position) {

		List<String> features = new ArrayList<String>();

		String currentWord = words.get(position);

		// Baseline Features
		features.add("word=" + currentWord);
		//features.add("prevLabel=" + previousLabel);
		//features.add("word=" + currentWord + ", prevLabel=" + previousLabel);

        // female letter
		if(currentWord.contains("ة"))
		    features.add("femaleLetter");

        // first and last letter
		features.add("firstCar=" + currentWord.charAt(0));
		features.add("lastCar=" + currentWord.charAt(currentWord.length()-1));

	
		if (currentWord.length() > 1) {
        
            // first two letters 
            if(currentWord.substring(0,2)=="ال" || currentWord.substring(0,2)=="لل")
                features.add("definition");

            // check if the previous word is org and the current starts with these sequeces
            if(previousLabel=="PERSON" && (currentWord.substring(0,2)=="ال"
                                         || currentWord.substring(0,2)=="لل"))
                features.add("previousIsOrg");

            // 2-grams
			List<String> ngrams = ngrams(2, currentWord);
			for (String ngram : ngrams)
			    features.add("2ngrams="+ngram);
		
        }
        

		if (currentWord.length() > 2) {

            // 3-grams
			List<String> ngrams = ngrams(3, currentWord);
			for (String ngram : ngrams)
			    features.add("3ngrams="+ngram);

        }
        
		if (currentWord.length() > 3) {
			
            // 4-grams
			List<String> ngrams = ngrams(4, currentWord);
			for (String ngram : ngrams)
			    features.add("4ngrams="+ngram);
			  
		
        }
       
        // word length
		features.add("length=" + currentWord.length());


		if (position > 0) {

            // previous word
            String prevWord = words.get(position - 1);
            features.add("prevWord=" + prevWord);
        

            // previous word length
			features.add("prevWordLength=" + prevWord.length());

            // last char of previous word
            if(prevWord.charAt(prevWord.length()-1) == 'ت')
                features.add("prevCarBeforeLast");

			if (prevWord.length() > 1) 
			    features.add("definetionChar=" + prevWord.contains("ال"));
           
            
        
        if (prevWord.length() > 2) {
            // 3-grams
			List<String> ngrams = ngrams(3, prevWord);
			for (String ngram : ngrams){
			    features.add("3ngramsPrev="+ngram);
			}
		
        }
        
		
        }
    

		
        // 0.4

		return features;
	}

	/** Do not modify this method **/
	public List<Datum> readData(String filename) throws IOException {

		List<Datum> data = new ArrayList<Datum>();
		BufferedReader in = new BufferedReader(new FileReader(filename));

		for (String line = in.readLine(); line != null; line = in.readLine()) {
			if (line.trim().length() == 0) {
				continue;
			}
			String[] bits = line.split("\\s+");
			String word = bits[0];
			String label = bits[1];

			Datum datum = new Datum(word, label);
			data.add(datum);
		}

		return data;
	}

	/** Do not modify this method **/
	public List<Datum> readTestData(String ch_aux) throws IOException {

		List<Datum> data = new ArrayList<Datum>();

		for (String line : ch_aux.split("\n")) {
			if (line.trim().length() == 0) {
				continue;
			}
			String[] bits = line.split("\\s+");
			String word = bits[0];
			String label = bits[1];

			Datum datum = new Datum(word, label);
			data.add(datum);
		}

		return data;
	}

	/** Do not modify this method **/
	public List<Datum> setFeaturesTrain(List<Datum> data) {
		// this is so that the feature factory code doesn't accidentally use the
		// true label info
		List<Datum> newData = new ArrayList<Datum>();
		List<String> words = new ArrayList<String>();

		for (Datum datum : data) {
			words.add(datum.word);
		}

		String previousLabel = "O";
		for (int i = 0; i < data.size(); i++) {
			Datum datum = data.get(i);

			Datum newDatum = new Datum(datum.word, datum.label);
			newDatum.features = computeFeatures(words, previousLabel, i);
			newDatum.previousLabel = previousLabel;
			newData.add(newDatum);

			previousLabel = datum.label;
		}

		return newData;
	}

	/** Do not modify this method **/
	public List<Datum> setFeaturesTest(List<Datum> data) {
		// this is so that the feature factory code doesn't accidentally use the
		// true label info
		List<Datum> newData = new ArrayList<Datum>();
		List<String> words = new ArrayList<String>();
		List<String> labels = new ArrayList<String>();
		Map<String, Integer> labelIndex = new HashMap<String, Integer>();

		for (Datum datum : data) {
			words.add(datum.word);
			if (labelIndex.containsKey(datum.label) == false) {
				labelIndex.put(datum.label, labels.size());
				labels.add(datum.label);
			}
		}

		// compute features for all possible previous labels in advance for
		// Viterbi algorithm
		for (int i = 0; i < data.size(); i++) {
			Datum datum = data.get(i);

			if (i == 0) {
				String previousLabel = "O";
				datum.features = computeFeatures(words, previousLabel, i);

				Datum newDatum = new Datum(datum.word, datum.label);
				newDatum.features = computeFeatures(words, previousLabel, i);
				newDatum.previousLabel = previousLabel;
				newData.add(newDatum);

			} else {
				for (String previousLabel : labels) {
					datum.features = computeFeatures(words, previousLabel, i);

					Datum newDatum = new Datum(datum.word, datum.label);
					newDatum.features = computeFeatures(words, previousLabel, i);
					newDatum.previousLabel = previousLabel;
					newData.add(newDatum);
				}
			}

		}

		return newData;
	}

	/** Do not modify this method **/
	public void writeData(List<Datum> data, String filename) throws IOException {

		FileWriter file = new FileWriter(filename + ".json", false);

		for (int i = 0; i < data.size(); i++) {
			try {
				JSONObject obj = new JSONObject();
				Datum datum = data.get(i);
				obj.put("_label", datum.label);
				obj.put("_word", base64encode(datum.word));
				obj.put("_prevLabel", datum.previousLabel);
				JSONObject featureObj = new JSONObject();

				List<String> features = datum.features;
				for (int j = 0; j < features.size(); j++) {
					String feature = features.get(j).toString();
					featureObj.put("_" + feature, feature);
				}
				obj.put("_features", featureObj);
				obj.write(file);
				file.append("\n");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		file.close();
	}

	/** Do not modify this method **/
	private String base64encode(String str) {
		Base64 base = new Base64();
		byte[] strBytes = str.getBytes();
		byte[] encBytes = base.encode(strBytes);
		String encoded = new String(encBytes);
		return encoded;
	}

}


