package edu.cmu.lti.deiis.hw5.annotators;

import edu.smu.tspell.wordnet.*;

public class SynonymExpander {

	public static void main(String[] args) {
		// set dict path
		String del = System.getProperty("file.separator");
		String proj_dir = System.getProperty("user.dir");
		String path = proj_dir + del + "lib" + del + "WordNet-3.0" + del + "dict";
		
		System.setProperty("wordnet.database.dir", path);
		
		// TODO Auto-generated method stub
		if (args.length > 0)
		{
			//  Concatenate the command-line arguments
			StringBuffer buffer = new StringBuffer();
//			for (int i = 0; i < args.length; i++)
//			{
//				buffer.append((i > 0 ? " " : "") + args[i]);
//			}
			
			
			String wordForm = buffer.toString();
			//  Get the synsets containing the wrod form
			WordNetDatabase database = WordNetDatabase.getFileInstance();
			Synset[] synsets = database.getSynsets(wordForm, SynsetType.VERB);
			//  Display the word forms and definitions for synsets retrieved
			if (synsets.length > 0)
			{
				System.out.println("The following synsets contain '" +
						wordForm + "' or a possible base form " +
						"of that text:");
				for (int i = 0; i < synsets.length; i++)
				{
					System.out.println("\nsynset-"+i);
					String[] wordForms = synsets[i].getWordForms();
					for (int j = 0; j < wordForms.length; j++)
					{
						System.out.print((j > 0 ? ", " : "") +
								wordForms[j]);
					}
					//System.out.println(": " + synsets[i].getDefinition());
				}
			}
			else
			{
				System.err.println("No synsets exist that contain " +
						"the word form '" + wordForm + "'");
			}
		}
		else
		{
			System.err.println("You must specify " +
					"a word form for which to retrieve synsets.");
		}
	}

}
