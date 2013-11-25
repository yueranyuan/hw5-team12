package edu.cmu.lti.deiis.hw5.answer_selection;

import java.util.ArrayList;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import edu.cmu.lti.qalab.types.Answer;
import edu.cmu.lti.qalab.types.QuestionAnswerSet;
import edu.cmu.lti.qalab.types.TestDocument;
import edu.cmu.lti.qalab.utils.Utils;

public class AnswerSelectionByAnswerConcat extends JCasAnnotator_ImplBase {

	public AnswerSelectionByAnswerConcat() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		// TODO Auto-generated method stub

		TestDocument testDoc = Utils.getTestDocumentFromCAS(aJCas);
		ArrayList<QuestionAnswerSet> qaSet = Utils.fromFSListToCollection(
				testDoc.getQaList(), QuestionAnswerSet.class);
		int matched = 0;
		int total = 0;
		int unanswered = 0;
		
		for (int i = 0; i < qaSet.size(); i++) {
			
			ArrayList<Answer> choiceList = Utils.fromFSListToCollection(qaSet
					.get(i).getAnswerList(), Answer.class);
			
			String correct = "";
			
			for (int j = 0; j < choiceList.size(); j++) {
				Answer answer = choiceList.get(j);
				if (answer.getIsCorrect()) {
					correct = answer.getText();
					break;
				}
			}
			
			
		}
		
	}

}
