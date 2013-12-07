package edu.cmu.lti.deiis.hw5.candidate_sentence;

import java.util.ArrayList;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.oaqa.core.provider.solr.SolrWrapper;
import edu.cmu.lti.qalab.types.Answer;
import edu.cmu.lti.qalab.types.CandidateSentence;
import edu.cmu.lti.qalab.types.Dependency;
import edu.cmu.lti.qalab.types.NER;
import edu.cmu.lti.qalab.types.NounPhrase;
import edu.cmu.lti.qalab.types.Question;
import edu.cmu.lti.qalab.types.QuestionAnswerSet;
import edu.cmu.lti.qalab.types.Sentence;
import edu.cmu.lti.qalab.types.TestDocument;
import edu.cmu.lti.qalab.utils.Utils;

public class QuestionCandSentSimilarityMatcher extends JCasAnnotator_ImplBase {

	SolrWrapper solrWrapper = null;
	String serverUrl;
	// IndexSchema indexSchema;
	String coreName;
	String schemaName;
	int TOP_SEARCH_RESULTS = 10;

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
		serverUrl = (String) context.getConfigParameterValue("SOLR_SERVER_URL");
		coreName = (String) context.getConfigParameterValue("SOLR_CORE");
		schemaName = (String) context.getConfigParameterValue("SCHEMA_NAME");
		TOP_SEARCH_RESULTS = (Integer) context
				.getConfigParameterValue("TOP_SEARCH_RESULTS");
		try {
			this.solrWrapper = new SolrWrapper(serverUrl + coreName);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		TestDocument testDoc = Utils.getTestDocumentFromCAS(aJCas);
		String testDocId = testDoc.getId();
		ArrayList<Sentence> sentenceList = Utils
				.getSentenceListFromTestDocCAS(aJCas);
		ArrayList<QuestionAnswerSet> qaSet = Utils
				.getQuestionAnswerSetFromTestDocCAS(aJCas);
		System.out.println(sentenceList.size() + "******************");

		for (int i = 0; i < qaSet.size(); i++) {
			try {
				Question question = qaSet.get(i).getQuestion();
				ArrayList<Answer> answers = Utils.fromFSListToCollection(qaSet
						.get(i).getAnswerList(), Answer.class);
				ArrayList<Answer> longAnswers = Utils.fromFSListToCollection(qaSet
		            .get(i).getLongAnswerList(), Answer.class);
				System.out
						.println("========================================================");
				System.out.println("Question: " + question.getText());

				// String searchQuery = this.formSolrQuery(question, answer);
				String searchQuery = this.formSolrQuery(question);
				if (searchQuery.trim().equals("")) {
					continue;
				}
				ArrayList<CandidateSentence> candidateSentList = new ArrayList<CandidateSentence>();
				SolrQuery solrQuery = new SolrQuery();
				solrQuery.add("fq", "docid:" + testDocId);
				solrQuery.add("q", searchQuery);
				// prioritize
				//solrQuery.add("defType", "dismax");
				//solrQuery.add("qf", "features^20.0+text^0.3");
				
				solrQuery.add("rows", String.valueOf(TOP_SEARCH_RESULTS));
				solrQuery.setFields("*", "score");

				SolrDocumentList results = solrWrapper.runQuery(solrQuery,
						TOP_SEARCH_RESULTS);
				for (int j = 0; j < results.size(); j++) {
					SolrDocument doc = results.get(j);
					String sentId = doc.get("id").toString();
					String docId = doc.get("docid").toString();
					if (!testDocId.equals(docId)) {
						continue;
					}
					String sentIdx = sentId.replace(docId, "").replace("_", "")
							.trim();
					int idx = Integer.parseInt(sentIdx);
					System.out.println(idx + "*********************");
					Sentence annSentence = sentenceList.get(idx);

					String sentence = doc.get("text").toString();
					double relScore = Double.parseDouble(doc.get("score")
							.toString());
					CandidateSentence candSent = new CandidateSentence(aJCas);
					candSent.setSentence(annSentence);
					candSent.setRelevanceScore(relScore);
					candidateSentList.add(candSent);
					System.out.println(relScore + "\t" + sentence);
				}
				/*
				// pre-select top answer
				searchQuery = this.formSolrQuery(longAnswers.get(0));
				solrQuery = new SolrQuery();
				solrQuery.add("fq", "docid:" + testDocId);
				solrQuery.add("q", searchQuery);
				solrQuery.add("rows", String.valueOf(TOP_SEARCH_RESULTS));
				solrQuery.setFields("*", "score");
				results = solrWrapper.runQuery(solrQuery, TOP_SEARCH_RESULTS);
				if (results.size() > 0) {
  				SolrDocument doc = results.get(0);
  				double preScore = Double.parseDouble(doc.get("score")
  						.toString());
  				int count = 1;
  				String preChoice = answers.get(0).getText();
  				System.out.println("---------------");
  				System.out.println("0: "+preChoice+", "+preScore);
  				System.out.println("-- "+doc.get("text").toString());
  				for (int j = 1; j < answers.size(); j++) {
  					searchQuery = this.formSolrQuery(longAnswers.get(j));
  					solrQuery = new SolrQuery();
  					solrQuery.add("fq", "docid:" + testDocId);
  					solrQuery.add("q", searchQuery);
  					solrQuery.add("rows", String.valueOf(TOP_SEARCH_RESULTS));
  					solrQuery.setFields("*", "score");
  					results = solrWrapper.runQuery(solrQuery,
  							TOP_SEARCH_RESULTS);
  					if (results == null || results.size() == 0){
  						continue;
  					}
  					
  					doc = results.get(0);
  					double tempScore = Double.parseDouble(doc.get("score")
  							.toString());
  					System.out.println(j+": "+answers.get(j).getText()+", "+tempScore);
  					System.out.println("-- "+doc.get("text").toString());
  					if (tempScore > preScore) {
  						preScore = tempScore;
  						count = 1;
  						preChoice = answers.get(j).getText();
  					} else if (tempScore == preScore)
  						count++;
  				}
  
  				// if only one has a high score, prioritize it
  				if (count == 1)
  					qaSet.get(i).setPreAnswer(preChoice);
				}

				*/
				
				FSList fsCandidateSentList = Utils.fromCollectionToFSList(
						aJCas, candidateSentList);
				fsCandidateSentList.addToIndexes();
				qaSet.get(i).setCandidateSentenceList(fsCandidateSentList);
				qaSet.get(i).addToIndexes();

			} catch (SolrServerException e) {
				e.printStackTrace();
			}

			FSList fsQASet = Utils.fromCollectionToFSList(aJCas, qaSet);
			testDoc.setQaList(fsQASet);

			System.out
					.println("=========================================================");
		}

	}

	public String formSolrQuery(Question question, Answer answer) {
		String solrQuery = "";

		ArrayList<NounPhrase> nounPhrases = Utils.fromFSListToCollection(
				question.getNounList(), NounPhrase.class);

		for (int i = 0; i < nounPhrases.size(); i++) {
			solrQuery += "nounphrases:\"" + nounPhrases.get(i).getText()
					+ "\" ";
		}

		// Add choice NER
		ArrayList<NounPhrase> choiceNouns = Utils.fromFSListToCollection(
				answer.getNounPhraseList(), NounPhrase.class);
		System.out.println("adding nounphrase to solr");
		for (int i = 0; i < choiceNouns.size(); i++) {
			solrQuery += "nounphrases:\"" + choiceNouns.get(i).getText()
					+ "\" ";
		}
		System.out.println(solrQuery);

		ArrayList<NER> neList = Utils.fromFSListToCollection(
				question.getNerList(), NER.class);
		for (int i = 0; i < neList.size(); i++) {
			solrQuery += "namedentities:\"" + neList.get(i).getText() + "\" ";
		}

		// Add dependency
		ArrayList<Dependency> dependencies = Utils.fromFSListToCollection(
				question.getDependencies(), Dependency.class);

		for (int j = 0; j < dependencies.size(); j++) {
			String rel = dependencies.get(j).getRelation();
			String gov = dependencies.get(j).getGovernor().getText();
			String dep = dependencies.get(j).getDependent().getText();
			String depText = rel + "(" + gov + "," + dep + ")";
			solrQuery += "dependencies:\"" + depText + "\" ";
		}

		solrQuery = solrQuery.trim();

		return solrQuery;
	}
	
	public String formSolrQuery(Answer answer) {
	  String solrQuery = "";

    ArrayList<NounPhrase> nounPhrases = Utils.fromFSListToCollection(
        answer.getNounPhraseList(), NounPhrase.class);

    for (int i = 0; i < nounPhrases.size(); i++) {
      solrQuery += "nounphrases:\"" + nounPhrases.get(i).getText()
          + "\" ";
    }

    // Add dependency
    ArrayList<Dependency> dependencies = Utils.fromFSListToCollection(
            answer.getDependencies(), Dependency.class);

    for (int j = 0; j < dependencies.size(); j++) {
      String rel = dependencies.get(j).getRelation();
      String gov = dependencies.get(j).getGovernor().getText();
      String dep = dependencies.get(j).getDependent().getText();
      String depText = rel + "(" + gov + "," + dep + ")";
      solrQuery += "dependencies:\"" + depText + "\" ";
    }

    solrQuery = solrQuery.trim();
    System.out.println("return solrQuery:");
    System.out.println(solrQuery);
    return solrQuery;
	}
	
	public String formSolrQuery(Question question) {
		String solrQuery = "";

		ArrayList<NounPhrase> nounPhrases = Utils.fromFSListToCollection(
				question.getNounList(), NounPhrase.class);

		for (int i = 0; i < nounPhrases.size(); i++) {
			solrQuery += "nounphrases:\"" + nounPhrases.get(i).getText()
					+ "\" ";
		}

		ArrayList<NER> neList = Utils.fromFSListToCollection(
				question.getNerList(), NER.class);
		for (int i = 0; i < neList.size(); i++) {
			solrQuery += "namedentities:\"" + neList.get(i).getText() + "\" ";
		}

		// Add dependency
		ArrayList<Dependency> dependencies = Utils.fromFSListToCollection(
				question.getDependencies(), Dependency.class);

		for (int j = 0; j < dependencies.size(); j++) {
			String rel = dependencies.get(j).getRelation();
			String gov = dependencies.get(j).getGovernor().getText();
			String dep = dependencies.get(j).getDependent().getText();
			String depText = rel + "(" + gov + "," + dep + ")";
			solrQuery += "dependencies:\"" + depText + "\" ";
		}

		solrQuery = solrQuery.trim();
		System.out.println("return solrQuery:");
		System.out.println(solrQuery);
		return solrQuery;
	}

}
