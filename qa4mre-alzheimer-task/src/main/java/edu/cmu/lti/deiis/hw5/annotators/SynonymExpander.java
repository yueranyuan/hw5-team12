package edu.cmu.lti.deiis.hw5.annotators;

import java.util.ArrayList;

import edu.smu.tspell.wordnet.*;

public class SynonymExpander {

	public static ArrayList<String> getSynonyms(String str, String type){
		String del = System.getProperty("file.separator");
		String proj_dir = System.getProperty("user.dir");
		String path = proj_dir + del + "lib" + del + "WordNet-3.0" + del + "dict";
		
		ArrayList<String> result = new ArrayList<String>();
		System.setProperty("wordnet.database.dir", path);
		
		WordNetDatabase database = WordNetDatabase.getFileInstance();
		
		Synset[] synsets;
		if (type == "VB") 
			synsets = database.getSynsets(str, SynsetType.VERB);
		else if (type == "NN")
			synsets = database.getSynsets(str, SynsetType.NOUN);
		else
			synsets = database.getSynsets(str);
		if (synsets == null)
			return null;
		else{
			for (int i = 0; i < synsets.length; i++)
			{
				String syn = "";
				String[] wordForms = synsets[i].getWordForms();
				for (int j = 0; j < wordForms.length; j++)
				{
					syn+=((j > 0 ? "-" : "") +
							wordForms[j]);
				}
				result.add(syn);
				//System.out.println(": " + synsets[i].getDefinition());
			}
			return result;
		}
		
	}
	
	
	public static void main(String[] args) {
		// set dict path
		ArrayList<String> result = getSynonyms("go into", "VB");
		System.out.println(result);
	}
}
