package edu.cmu.lti.deiis.hw5.annotators;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.qalab.types.Sentence;
import edu.cmu.lti.qalab.types.TestDocument;
import edu.cmu.lti.qalab.utils.Utils;
import edu.stanford.nlp.util.StringUtils;

public class NoiseFilter extends JCasAnnotator_ImplBase {

	double QUALITY_THRESHOLD=0.75;
	int MIN_WORDS=5;
	int MIN_LENGTH=25;
	Pattern authorPattern=Pattern.compile("[A-Z][a-z]+[ ]+[A-Z]{1,3}[, ]");
	Pattern yearPattern=Pattern.compile("[(][0-9]{4}[)]");
	
	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
		
		QUALITY_THRESHOLD=(Float)context.getConfigParameterValue("QUALITY_THRESHOLD");
		MIN_WORDS=(Integer)context.getConfigParameterValue("MIN_WORDS");
		MIN_LENGTH=(Integer)context.getConfigParameterValue("MIN_LENGTH");
	}
	/**
	private ArrayList<Integer> readFakeAnnotations(String docId) {
	  ArrayList<Integer> ids = new ArrayList<Integer>();
	  
	  // based on http://www.mkyong.com/java/how-to-read-and-parse-csv-file-in-java/
	  String file = "/home/yueran/git/hw5-team12/qa4mre-alzheimer-task/src/main/java/edu/cmu/lti/deiis/hw5/annotators/test.csv";
	  BufferedReader br = null;
	  String line = "";
	 
	  try {
	    br = new BufferedReader(new FileReader(file));
	    while ((line = br.readLine()) != null) {
	      String[] id = line.split(" ");
	 
	      ids.add(Integer.parseInt(id[0]));
	    }
	  } catch (FileNotFoundException e) {
	    System.out.println("can't find file " + file);
	  } catch (IOException e) {
	    e.printStackTrace();
	  } finally {
	    if (br != null) {
	      try {
	        br.close();
	      } catch (IOException e) {
	        e.printStackTrace();
	      }
	    }
	  }
	  
	  return ids;
	}
	*/
	
	// don't do any filtering because I noise filtered by hand
	 @Override
	 public void process(JCas jCas) throws AnalysisEngineProcessException {
	    TestDocument testDoc = (TestDocument) Utils.getTestDocumentFromCAS(jCas);

	    String id = testDoc.getId();
	    ArrayList<Sentence> sentenceList = new ArrayList<Sentence>();
	    //ArrayList<Integer> ids = readFakeAnnotations(id);
	    //System.out.println(ids);
	    FSList sentList=testDoc.getSentenceList();
      String filteredText = "";
      int i=0;
      while (true) {
        i++;
        Sentence sentence = null;
        try {
          sentence = (Sentence) sentList.getNthElement(i);
        } catch (Exception e) {
          break;
        }
        
        //System.out.println(sentence.getStart());
        //System.out.println(sentence.getText() + "\n");
        
        String sentText=sentence.getText().trim();
        sentenceList.add(sentence);
        filteredText+=sentText+"\n";
      }
	    
	    FSList modifiedSentList=Utils.createSentenceList(jCas, sentenceList);
      //annotation.setId(id);
      testDoc.setSentenceList(modifiedSentList);
      testDoc.setFilteredText(filteredText);
      testDoc.addToIndexes();
	  }
	
	public void processOriginal(JCas jCas) throws AnalysisEngineProcessException {
		System.out.println("******Entered into process of NoiseFilter");
		TestDocument testDoc=Utils.getTestDocumentFromCAS(jCas);
		//String id = srcDoc.getId();
		String docText = testDoc.getText();
		ArrayList<Sentence>sentenceList=new ArrayList<Sentence>();
		try {
			//String lines[] = docText.split("[\\n]");
			FSList sentList=testDoc.getSentenceList();
			String filteredText = "";
			int i=0;
			while (true) {
				
				i++;
				Sentence sentence = null;
				try {
					sentence = (Sentence) sentList.getNthElement(i);
				} catch (Exception e) {
					break;
				}
				
				String sentText=sentence.getText().trim();
				//System.out.println("Processing sentence "+i+"\t"+sentText);
				if(sentText.equals("")){
					continue;
				}
				
				double qualityScore = this.getSentQuality(sentText);
				//System.out.println("****Quality Score: "+qualityScore+"\t"+sentText);
				
				if(qualityScore<QUALITY_THRESHOLD){
					//sentence.removeFromIndexes();
					sentence.setBFilter(true);
					continue;
				}
				
				sentence.setQualityScore(qualityScore);
				//sentence.addToIndexes();
				sentenceList.add(sentence);
				filteredText+=sentText+"\n";
				
			}
						
			//System.out.println("Difference between size of (SourceDocument - FilteredDocument): "+(docText.length()-filteredText.length()));
		
			FSList modifiedSentList=Utils.createSentenceList(jCas, sentenceList);
			//annotation.setId(id);
			testDoc.setSentenceList(modifiedSentList);
			testDoc.setFilteredText(filteredText);
			testDoc.addToIndexes();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
	public double getSentQuality(String sent) throws Exception {
		String words[] = sent.split("[\\W]");

		if (words.length <MIN_WORDS || sent.length() < MIN_LENGTH) {
			return 0.0;
		}

		HashSet<String> lowQualityWord = new HashSet<String>();
		lowQualityWord.add("Abstract");
		lowQualityWord.add("References");
		lowQualityWord.add("Medline");
		lowQualityWord.add("pp.");
		lowQualityWord.add("See also");

		int numericWords = 0;
		int abbrWords = 0;
		int lowQualityWords = 0;
		int authorCount=0;
		Matcher authorMatcher=authorPattern.matcher(sent);
		while(authorMatcher.find()){
			authorCount++;
		}
		
		/*if(authorCount>2){
			System.out.println("########Authors: "+authorCount+"\t"+sent);
		}*/
		
		int yearCount=0;
		Matcher yearMatcher=yearPattern.matcher(sent);
		while(yearMatcher.find()){
			yearCount++;
		}
		int totalWords = 0;
		for (int i = 0; i < words.length; i++) {
			if (StringUtils.isNumeric(words[i])) {
				numericWords++;
			}
			if (StringUtils.isAcronym(words[i])) {
				abbrWords++;
			}
			if (lowQualityWord.contains(words[i])) {
				lowQualityWords++;
			}

			totalWords++;
		}

		double noiseScore = (numericWords + abbrWords + lowQualityWords+authorCount*1.2+yearCount)
				/ (double) totalWords;

		double score = 1.0 - noiseScore;
		return score;

	}
	
}
