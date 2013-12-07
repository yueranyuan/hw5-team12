package edu.cmu.lti.deiis.hw5.annotators;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.resource.ResourceInitializationException;

import com.googlecode.clearnlp.dependency.DEPNode;
import com.googlecode.clearnlp.dependency.DEPParser;
import com.googlecode.clearnlp.dependency.DEPTree;
import com.googlecode.clearnlp.dependency.srl.SRLabeler;
import com.googlecode.clearnlp.engine.EngineGetter;
import com.googlecode.clearnlp.engine.EngineProcess;
import com.googlecode.clearnlp.morphology.AbstractMPAnalyzer;
import com.googlecode.clearnlp.pos.POSTagger;
import com.googlecode.clearnlp.predicate.PredIdentifier;
import com.googlecode.clearnlp.reader.AbstractReader;
import com.googlecode.clearnlp.tokenization.AbstractTokenizer;
import com.googlecode.clearnlp.util.pair.Pair;

import edu.cmu.lti.qalab.types.Dependency;
import edu.cmu.lti.qalab.types.Sentence;
import edu.cmu.lti.qalab.types.TestDocument;
import edu.cmu.lti.qalab.types.Token;
import edu.cmu.lti.qalab.utils.Utils;

public class ClearNLPAnnotator extends JCasAnnotator_ImplBase {

  private AbstractTokenizer tokenizer;
  private AbstractMPAnalyzer analyzer;
  private Pair<POSTagger[], Double> taggers;
  private DEPParser parser;
  private PredIdentifier preder;
  private SRLabeler labeler;

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
		try {
      //LoadModels();
    } catch (Exception e) {
      throw new ResourceInitializationException();
    }
	}
  
  public InputStream getStream(String file) {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream(file);
    return is;
  }
  
  public void LoadModels() throws Exception
  {
    InputStream dictStream = getStream("cleartk/dictionary-1.4.0.zip");
    InputStream posStream = getStream("cleartk/medical-en-pos-1.1.0g.jar");
    InputStream depStream = getStream("cleartk/medical-en-dep-1.1.0b3.jar");
    InputStream predStream = getStream("cleartk/medical-en-pred-1.2.0.jar");
    InputStream srlStream = getStream("cleartk/medical-en-srl-1.2.0b1.jar");
    
    tokenizer = EngineGetter.getTokenizer(AbstractReader.LANG_EN, dictStream);
    dictStream = this.getClass().getClassLoader().getResourceAsStream("cleartk/dictionary-1.4.0.zip");
    analyzer = EngineGetter.getMPAnalyzer(AbstractReader.LANG_EN, dictStream);
    taggers = EngineGetter.getPOSTaggers(posStream);
    parser = (DEPParser) EngineGetter.getDEPParser(depStream);
    preder = (PredIdentifier) EngineGetter.getPredIdentifier(predStream);
    labeler = (SRLabeler) EngineGetter.getSRLabeler(srlStream);
  }
  
  public FSList annotate(JCas aJCas, String sentence) {
    DEPTree tree = EngineProcess.getDEPTree(tokenizer, taggers, analyzer, parser, preder, labeler, sentence);
    return createDependencyList(aJCas, tree.toArray(new DEPNode[tree.size()]));
  }
  
  public FSList createDependencyList(JCas aJCas,
      DEPNode[] nodes) {
    
    if (nodes.length == 0) {
      return new EmptyFSList(aJCas);
    }

    NonEmptyFSList head = new NonEmptyFSList(aJCas);
    NonEmptyFSList list = head;
    
    for (int i = 0; i < nodes.length; i++) {
      DEPNode n = nodes[i];
      DEPNode head_n = n.getHead();
      if (head_n == null || n.getLabel() == null) {
        continue;
      }

      Dependency dep = new Dependency(aJCas);
      Token governorToken = new Token(aJCas);
      governorToken.setText(head_n.lemma);
      governorToken.setPos(head_n.pos);
      governorToken.setNer(head_n.nament);
      governorToken.addToIndexes();
      dep.setGovernor(governorToken);
      
      Token dependentToken = new Token(aJCas);
      dependentToken.setText(n.lemma);
      dependentToken.setPos(n.pos);
      dependentToken.setNer(n.nament);
      dependentToken.addToIndexes();
      dep.setDependent(dependentToken);
      
      dep.setRelation(n.getLabel());
      dep.addToIndexes();
      /*
      String rel1 = dep.getRelation();
      String gov1 = dep.getGovernor().getText();
      String dep1 = dep.getDependent().getText();
      String depText = rel1 + "(" + gov1 + "," + dep1 + ")";
      System.out.println("CLEAR1 " + depText);
      */
      head.setHead(dep);
      if (i < nodes.length - 1) {
        head.setTail(new NonEmptyFSList(aJCas));
        head = (NonEmptyFSList) head.getTail();
      } else {
        head.setTail(new EmptyFSList(aJCas));
      }
    }

    return list;
  }

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {

	  TestDocument testDoc=Utils.getTestDocumentFromCAS(jCas);
	  
	  ArrayList<Sentence>sentenceList=Utils.getSentenceListFromTestDocCAS(jCas);
    
    for(int i=0;i<sentenceList.size();i++){
      Sentence annSentence=sentenceList.get(i);
      
      // get old stuff
      FSList fsDependencies = annSentence.getDependencyList();
      if (fsDependencies == null) {
        continue;
      }
      ArrayList<Dependency> stanDependencies = Utils
              .fromFSListToCollection(fsDependencies,
                      Dependency.class);
      
      // get new stuff
      FSList clearFsDependencies = annotate(jCas, annSentence.getText());
      
      // combine old and new stuff
      ArrayList<Dependency> clearDependencies = Utils
              .fromFSListToCollection(clearFsDependencies,
                      Dependency.class);
      
      // get overlap
      FSList fsDependencyList = createDependencyList(jCas,
              getDependencyOverlap(clearDependencies, stanDependencies));
      
      // push to index
      fsDependencyList.addToIndexes(jCas);
      annSentence.setDependencyList(fsDependencyList);
      annSentence.addToIndexes();
      sentenceList.set(i, annSentence);
		}
    
    FSList fsSentList=Utils.createSentenceList(jCas, sentenceList);
    testDoc.setSentenceList(fsSentList);

	}
	
	private Collection<Dependency> getDependencyOverlap(
	        ArrayList<Dependency> dependencies1,
          ArrayList<Dependency> dependencies2) {
	  ArrayList<Dependency> overlap = new ArrayList<Dependency>();
	  
	  /*
	  System.out.println("sentence");
	  for (int i = 0; i < dependencies1.size(); i++) {
      Dependency dependency1 = dependencies1.get(i);
      String rel1 = dependency1.getRelation();
      String gov1 = dependency1.getGovernor().getText();
      String dep1 = dependency1.getDependent().getText();
      String depText = rel1 + "(" + gov1 + "," + dep1 + ")";
      System.out.println("CLEAR " + depText);
	  }
	  
	  for (int i = 0; i < dependencies2.size(); i++) {
      Dependency dependency1 = dependencies2.get(i);
      String rel1 = dependency1.getRelation();
      String gov1 = dependency1.getGovernor().getText();
      String dep1 = dependency1.getDependent().getText();
      String depText = rel1 + "(" + gov1 + "," + dep1 + ")";
      System.out.println("STANFORD " + depText);
    }
	  */
	  for (int i = 0; i < dependencies1.size(); i++) {
	    Dependency dependency1 = dependencies1.get(i);
      String rel1 = dependency1.getRelation();
      String gov1 = dependency1.getGovernor().getText();
      String dep1 = dependency1.getDependent().getText();
  	  for (int j = 0; j < dependencies2.size(); j++) {
  	    Dependency dependency2 = dependencies2.get(j);
        String rel2 = dependency2.getRelation();
        String gov2 = dependency2.getGovernor().getText();
        String dep2 = dependency2.getDependent().getText();
        if (rel1.equals(rel2) && gov1.equals(gov2) && dep1.equals(dep2)) {
          overlap.add(dependencies2.get(j));
          String depText = rel2 + "(" + gov2 + "," + dep2 + ")";
          //System.out.println("OVERLAP " + depText);
          continue;
        }
      }
	  }
	  
	  return overlap;
  }

  /**
   * @param aJCas
   * @param aCollection
   * @return
   */
  public FSList createDependencyList(JCas aJCas, Collection<Dependency> aCollection) {
    if (aCollection.size() == 0) {
      return new EmptyFSList(aJCas);
    }

    NonEmptyFSList head = new NonEmptyFSList(aJCas);
    NonEmptyFSList list = head;
    Iterator<Dependency> i = aCollection.iterator();
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

}
