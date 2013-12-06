package edu.cmu.lti.deiis.hw5.annotators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.qalab.types.Dependency;
import edu.cmu.lti.deiis.hw5.annotators.SynonymExpander;
import edu.cmu.lti.qalab.types.NounPhrase;
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
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

public class StanfordNLPAnnotator extends JCasAnnotator_ImplBase {

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

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

		TestDocument testDoc = (TestDocument) Utils
				.getTestDocumentFromCAS(jCas);

		String id = testDoc.getId();
		String filteredText = testDoc.getFilteredText();
		// System.out.println("===============================================");
		// System.out.println("DocText: " + docText);
		String filteredSents[] = filteredText.split("[\\n]");
		System.out.println("Total sentences: " + filteredSents.length);
		ArrayList<Sentence> sentList = new ArrayList<Sentence>();
		int sentNo = 0;
		for (int i = 0; i < filteredSents.length; i++) {

			Annotation document = new Annotation(filteredSents[i]);

			try {
				// System.out.println("Entering stanford annotation");
				stanfordAnnotator.annotate(document);
				// System.out.println("Out of stanford annotation");
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			// SourceDocument sourcecDocument=(SourceDocument)
			// jCas.getAnnotationIndex(SourceDocument.type);

			// FSList sentenceList = srcDoc.getSentenceList();

			// Moved to the front - to add single verbs
			ArrayList<NounPhrase> phraseList = new ArrayList<NounPhrase>();
			
			for (CoreMap sentence : sentences) {

				String sentText = sentence.toString();
				Sentence annSentence = new Sentence(jCas);
				ArrayList<Token> tokenList = new ArrayList<Token>();

				// Dependency should have Token rather than String
				for (int j = 0; j < sentence.get(TokensAnnotation.class).size(); j++) { // order
																				// needs
																				// to
																				// be
																				// considered
					CoreLabel token = sentence.get(TokensAnnotation.class).get(j);
					int begin = token.beginPosition();

					int end = token.endPosition();
					// System.out.println(begin + "\t" + end);
					String orgText = token.originalText();
					// this is the POS tag of the token
					String pos = token.get(PartOfSpeechAnnotation.class);
					// this is the NER label of the token
					String ne = token.get(NamedEntityTagAnnotation.class);
					// this is lemma of the token
					String lemma = token.getString(LemmaAnnotation.class);
					Token annToken = new Token(jCas);
					annToken.setBegin(begin);
					annToken.setEnd(end);
					annToken.setText(lemma);
					annToken.setPos(pos);
					annToken.setNer(ne);
					annToken.addToIndexes();
					
					
					// add verbs
					if (pos.startsWith("VB")){
						// ignore 'was' before the passive verb
						if (j!= sentence.get(TokensAnnotation.class).size()-1)
							if (sentence.get(TokensAnnotation.class).get(j+1).get(PartOfSpeechAnnotation.class).startsWith("VBN"))
								continue;
						NounPhrase sn = new NounPhrase(jCas);
						sn.setText(lemma);
						phraseList.add(sn);
						
						ArrayList<String>syn = SynonymExpander.getSynonyms(lemma, "");
						for (String str : syn){
							if (str == "be")
								continue;
							sn = new NounPhrase(jCas);
							sn.setText(str);
							phraseList.add(sn);
						}
					}
					
					//System.out.println(orgText+"-"+pos+"-"+lemma);
					tokenList.add(annToken);
				}

				FSList fsTokenList = this.createTokenList(jCas, tokenList);
				fsTokenList.addToIndexes();

				// this is the Stanford dependency graph of the current sentence
				SemanticGraph dependencies = sentence
						.get(CollapsedCCProcessedDependenciesAnnotation.class);
				List<SemanticGraphEdge> depList = dependencies.edgeListSorted();
				FSList fsDependencyList = this.createDependencyList(jCas,
						depList);
				fsDependencyList.addToIndexes();
				// Dependency dependency = new Dependency(jCas);
				// System.out.println("Dependencies: "+dependencies);

				// experimental parser
				// Add noun phrases to index
				Tree tree = sentence.get(TreeAnnotation.class);

				/*
				for (Tree subtree : tree) {
					if (subtree.label().value().equals("NP")
							|| subtree.label().value().equals("VP")) {
						String nounPhrase = edu.stanford.nlp.ling.Sentence
								.listToString(subtree.yield());
						// System.out.println(edu.stanford.nlp.ling.Sentence.listToString(subtree.yield()));
						if (nounPhrase.length() > 20)
							continue;	// skip too long phrases
						NounPhrase nn = new NounPhrase(jCas);
						nn.setText(nounPhrase);
						phraseList.add(nn);
					}
				}*/
				FSList fsPhraseList = Utils.createNounPhraseList(jCas,
						phraseList);
				fsPhraseList.addToIndexes(jCas);

				/*
				 * Set<Tree> subtrees = tree.subTrees(); Iterator<Tree> tIter =
				 * subtrees.iterator(); while (tIter.hasNext()) { Tree t =
				 * tIter.next(); Collection<Label> labels = t.labels();
				 * Iterator<Label> lIter = labels.iterator(); while
				 * (lIter.hasNext()) { Label l = lIter.next(); l.value(); } }
				 */

				annSentence.setId(String.valueOf(sentNo));
				annSentence.setBegin(tokenList.get(0).getBegin());// begin of
																	// first
																	// token
				annSentence
						.setEnd(tokenList.get(tokenList.size() - 1).getEnd());// end
																				// of
																				// last
																				// token
				annSentence.setText(sentText);
				annSentence.setTokenList(fsTokenList);
				annSentence.setDependencyList(fsDependencyList);
				annSentence.setPhraseList(fsPhraseList);
				annSentence.addToIndexes();
				sentList.add(annSentence);
				sentNo++;
				System.out.println("Sentence no. " + sentNo + " processed");
			}
		}
		FSList fsSentList = this.createSentenceList(jCas, sentList);

		// this.iterateFSList(fsSentList);
		fsSentList.addToIndexes();

		testDoc.setId(id);
		testDoc.setSentenceList(fsSentList);
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
