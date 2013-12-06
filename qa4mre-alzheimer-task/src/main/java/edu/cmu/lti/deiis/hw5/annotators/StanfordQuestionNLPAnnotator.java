package edu.cmu.lti.deiis.hw5.annotators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.qalab.types.Answer;
import edu.cmu.lti.qalab.types.Dependency;
import edu.cmu.lti.qalab.types.NounPhrase;
import edu.cmu.lti.qalab.types.Question;
import edu.cmu.lti.qalab.types.QuestionAnswerSet;
import edu.cmu.lti.qalab.types.Sentence;
import edu.cmu.lti.qalab.types.TestDocument;
import edu.cmu.lti.qalab.types.Token;
import edu.cmu.lti.qalab.utils.Utils;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

public class StanfordQuestionNLPAnnotator extends JCasAnnotator_ImplBase {

	private StanfordCoreNLP stanfordAnnotator;

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");// ,
																			// ssplit
		stanfordAnnotator = new StanfordCoreNLP(props);
	}
	
	public ArrayList<ArrayList<Answer>> annotateAnswerList(JCas jCas, ArrayList<ArrayList<Answer>> answerList) {
	  for (int i = 0; i < answerList.size(); i++) {

      ArrayList<Answer> choiceList = answerList.get(i);
      for (int j = 0; j < choiceList.size(); j++) {
        Answer answer = choiceList.get(j);
        Annotation document = new Annotation(answer.getText());
        try {
          // System.out.println("Entering stanford annotation");
          stanfordAnnotator.annotate(document);
          // System.out.println("Out of stanford annotation");
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }

        List<CoreMap> answers = document.get(SentencesAnnotation.class);

        for (CoreMap ans : answers) {

          String ansText = ans.toString();
          //Answer annAnswer = new Answer(jCas);
          ArrayList<Token> tokenList = new ArrayList<Token>();
          List<CoreLabel> nlpTokens = ans.get(TokensAnnotation.class);

          // Dependency should have Token rather than String
          for (CoreLabel token : nlpTokens) { // order
                                        // needs
                                        // to
                                        // be
                                        // considered
            int begin = token.beginPosition();

            int end = token.endPosition();
            // System.out.println(begin + "\t" + end);
            String orgText = token.originalText();
            // this is the POS tag of the token
            String pos = token.get(PartOfSpeechAnnotation.class);
            // this is the NER label of the token
            String ne = token.get(NamedEntityTagAnnotation.class);
            
            Token annToken = new Token(jCas);
            annToken.setBegin(begin);
            annToken.setEnd(end);
            annToken.setText(orgText);
            annToken.setPos(pos);
            annToken.setNer(ne);
            annToken.addToIndexes();

            tokenList.add(annToken);
          }

          FSList fsTokenList = this.createTokenList(jCas, tokenList);
          fsTokenList.addToIndexes();
          
          // Add noun phrases to index
          Tree tree = ans.get(TreeAnnotation.class);
          
          ArrayList<NounPhrase>phraseList= new ArrayList<NounPhrase>();
          for (Tree subtree : tree) { 
            if (subtree.label().value().equals("NP") || subtree.label().value().equals("VP")) {
              String nounPhrase = "";
              // Find the matching token and get the lemma.  Code based on
              // https://mailman.stanford.edu/pipermail/java-nlp-user/2011-June/001069.html
              for(Tree leaf : subtree.getLeaves()) {
                if(leaf.label() instanceof CoreLabel) {
                  CoreLabel label = (CoreLabel) leaf.label();
                  for(CoreLabel l : nlpTokens) {
                    if(l.beginPosition() == label.beginPosition() &&
                         l.endPosition() == label.endPosition()) {
                      nounPhrase += " " + l.get(LemmaAnnotation.class);
                      break;
                    }
                  }
                }
              }
              
              /*
              String nounPhrase = edu.stanford.nlp.ling.Sentence.listToString(subtree.yield());
              */
              NounPhrase nn=new NounPhrase(jCas);
              nn.setText(nounPhrase);
              phraseList.add(nn);
            }
          }
          FSList fsPhraseList=Utils.createNounPhraseList(jCas, phraseList);
          fsPhraseList.addToIndexes(jCas);

          answer.setId(String.valueOf(j));
          answer.setBegin(tokenList.get(0).getBegin());// begin of
                                  // first
                                  // token
          answer.setEnd(tokenList.get(tokenList.size() - 1)
              .getEnd());// end
                    // of
                    // last
                    // token
          answer.setText(ansText);
          answer.setTokenList(fsTokenList);
          answer.setNounPhraseList(fsPhraseList);
          answer.addToIndexes();
          choiceList.set(j, answer);

          System.out.println("Answer no. " + j + " processed");
        }
        
      }
      answerList.set(i, choiceList);
      
    }
	  return answerList;
	}
	
	public Question parseQuestion(JCas jCas, Question origQuestion, int sentNo) {
	  String questionText = origQuestion.getText();
    Annotation document = new Annotation(questionText);

    try {
      // System.out.println("Entering stanford annotation");
      stanfordAnnotator.annotate(document);
      // System.out.println("Out of stanford annotation");
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    // Although it is defined as list...this list will contain only one
    // question
    List<CoreMap> questions = document.get(SentencesAnnotation.class);

    Question annQuestion = new Question(jCas);
    for (CoreMap question : questions) {
      String qText = question.toString();
      ArrayList<Token> tokenList = new ArrayList<Token>();
      List<CoreLabel> nlpTokens = question.get(TokensAnnotation.class);

      // Dependency should have Token rather than String
      for (CoreLabel token : nlpTokens) { // order
                                      // needs
                                      // to
                                      // be
                                      // considered
        int begin = token.beginPosition();

        int end = token.endPosition();
        // System.out.println(begin + "\t" + end);
        String orgText = token.originalText();
        // this is the POS tag of the token
        String pos = token.get(PartOfSpeechAnnotation.class);
        // this is the NER label of the token
        String ne = token.get(NamedEntityTagAnnotation.class);
        Token annToken = new Token(jCas);
        annToken.setBegin(begin);
        annToken.setEnd(end);
        annToken.setText(orgText);
        annToken.setPos(pos);
        annToken.setNer(ne);
        annToken.addToIndexes();

        tokenList.add(annToken);
      }

      FSList fsTokenList = this.createTokenList(jCas, tokenList);
      fsTokenList.addToIndexes();
      
      // Add noun phrases to index
      Tree tree = question.get(TreeAnnotation.class);
      
      ArrayList<NounPhrase>phraseList= new ArrayList<NounPhrase>();
      for (Tree subtree : tree) { 
        if (subtree.label().value().equals("NP") || subtree.label().value().equals("VP")) {
          String nounPhrase = "";
          // Find the matching token and get the lemma.  Code based on
          // https://mailman.stanford.edu/pipermail/java-nlp-user/2011-June/001069.html
          for(Tree leaf : subtree.getLeaves()) {
            if(leaf.label() instanceof CoreLabel) {
              CoreLabel label = (CoreLabel) leaf.label();
              for(CoreLabel l : nlpTokens) {
                if(l.beginPosition() == label.beginPosition() &&
                     l.endPosition() == label.endPosition()) {
                  nounPhrase += " " + l.get(LemmaAnnotation.class);
                  break;
                }
              }
            }
          }
          
          
          /*
          String nounPhrase = edu.stanford.nlp.ling.Sentence.listToString(subtree.yield());
          */
          NounPhrase nn=new NounPhrase(jCas);
          nn.setText(nounPhrase);
          phraseList.add(nn);
        }
      }
      FSList fsPhraseList=Utils.createNounPhraseList(jCas, phraseList);
      fsPhraseList.addToIndexes(jCas);

      // this is the Stanford dependency graph of the current sentence
      SemanticGraph dependencies = question
          .get(CollapsedCCProcessedDependenciesAnnotation.class);
      List<SemanticGraphEdge> depList = dependencies.edgeListSorted();
      FSList fsDependencyList = this.createDependencyList(jCas,
          depList);
      fsDependencyList.addToIndexes();
      // Dependency dependency = new Dependency(jCas);
      // System.out.println("Dependencies: "+dependencies);

      annQuestion.setId(String.valueOf(sentNo));
      annQuestion.setBegin(tokenList.get(0).getBegin());// begin of
                                // first
                                // token
      annQuestion
          .setEnd(tokenList.get(tokenList.size() - 1).getEnd());// end
                                      // of
                                      // last
                                      // token
      annQuestion.setText(questionText);
      annQuestion.setTokenList(fsTokenList);
      annQuestion.setDependencies(fsDependencyList);
      annQuestion.setNounList(fsPhraseList);
      annQuestion.addToIndexes();
      System.out.println("Question no. " + sentNo + " processed");
      
    }
	  
    return annQuestion;
	}
	
	public ArrayList<Question> parseQuestionList(JCas jCas, ArrayList<Question> questionList){
    for (int i = 0; i < questionList.size(); i++) {
      questionList.set(i, parseQuestion(jCas, questionList.get(i), i));
    }
    return questionList;
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		TestDocument testDoc = (TestDocument) Utils
				.getTestDocumentFromCAS(jCas);

		String id = testDoc.getId();
		ArrayList<Question> questionList = Utils
				.getQuestionListFromTestDocCAS(jCas);
		ArrayList<ArrayList<Answer>> answerList = Utils
				.getAnswerListFromTestDocCAS(jCas);
		
		answerList = annotateAnswerList(jCas, answerList);
		questionList = parseQuestionList(jCas, questionList);

		System.out.println("Total Questions: " + questionList.size());
		
		// FSList fsQuestionList = Utils.createQuestionList(jCas, questionList);
		// fsQuestionList.addToIndexes();
		ArrayList<QuestionAnswerSet> qaSet = Utils
				.getQuestionAnswerSetFromTestDocCAS(jCas);
		for (int i = 0; i < qaSet.size(); i++) {
			questionList.get(i).addToIndexes();
			qaSet.get(i).setQuestion(questionList.get(i));
			FSList fsAnswerList=Utils.fromCollectionToFSList(jCas, answerList.get(i));
			qaSet.get(i).setAnswerList(fsAnswerList);
			
		}

		FSList fsQASet = Utils.createQuestionAnswerSet(jCas, qaSet);

		testDoc.setId(id);
		testDoc.setQaList(fsQASet);
		testDoc.addToIndexes();

	}

	/**
	 * Creates FeatureStructure List from sentenceList
	 * 
	 * @param <T>
	 * 
	 * @param aJCas
	 * @param aCollection
	 * @return FSList
	 */

	public FSList createSentenceList(JCas aJCas,
			Collection<Sentence> aCollection) {
		if (aCollection.size() == 0) {
			return new EmptyFSList(aJCas);
		}

		NonEmptyFSList head = new NonEmptyFSList(aJCas);
		NonEmptyFSList list = head;
		Iterator<Sentence> i = aCollection.iterator();
		while (i.hasNext()) {
			head.setHead(i.next());
			if (i.hasNext()) {
				head.setTail(new NonEmptyFSList(aJCas));
				head = (NonEmptyFSList) head.getTail();
			} else {
				head.setTail(new EmptyFSList(aJCas));
			}
		}

		return list;
	}

	/**
	 * @param aJCas
	 * @param aCollection
	 * @return
	 */
	public FSList createTokenList(JCas aJCas, Collection<Token> aCollection) {
		if (aCollection.size() == 0) {
			return new EmptyFSList(aJCas);
		}

		NonEmptyFSList head = new NonEmptyFSList(aJCas);
		NonEmptyFSList list = head;
		Iterator<Token> i = aCollection.iterator();
		while (i.hasNext()) {
			head.setHead(i.next());
			if (i.hasNext()) {
				head.setTail(new NonEmptyFSList(aJCas));
				head = (NonEmptyFSList) head.getTail();
			} else {
				head.setTail(new EmptyFSList(aJCas));
			}
		}

		return list;
	}

	public FSList createDependencyList(JCas aJCas,
			Collection<SemanticGraphEdge> aCollection) {
		if (aCollection.size() == 0) {
			return new EmptyFSList(aJCas);
		}

		NonEmptyFSList head = new NonEmptyFSList(aJCas);
		NonEmptyFSList list = head;
		Iterator<SemanticGraphEdge> i = aCollection.iterator();
		while (i.hasNext()) {
			SemanticGraphEdge edge = i.next();
			Dependency dep = new Dependency(aJCas);

			Token governorToken = new Token(aJCas);
			governorToken.setText(edge.getGovernor().originalText());
			governorToken.setPos(edge.getGovernor().tag());
			governorToken.setNer(edge.getGovernor().ner());
			governorToken.addToIndexes();
			dep.setGovernor(governorToken);

			Token dependentToken = new Token(aJCas);
			dependentToken.setText(edge.getDependent().originalText());
			dependentToken.setPos(edge.getDependent().tag());
			dependentToken.setNer(edge.getDependent().ner());
			dependentToken.addToIndexes();
			dep.setDependent(dependentToken);

			dep.setRelation(edge.getRelation().toString());
			dep.addToIndexes();

			head.setHead(dep);
			if (i.hasNext()) {
				head.setTail(new NonEmptyFSList(aJCas));
				head = (NonEmptyFSList) head.getTail();
			} else {
				head.setTail(new EmptyFSList(aJCas));
			}
		}

		return list;
	}

}
