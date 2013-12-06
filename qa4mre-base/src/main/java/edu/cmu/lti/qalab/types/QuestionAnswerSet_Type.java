
/* First created by JCasGen Fri Feb 22 06:13:28 EST 2013 */
package edu.cmu.lti.qalab.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Thu Dec 05 21:30:27 EST 2013
 * @generated */
public class QuestionAnswerSet_Type extends Annotation_Type {
  /** @generated */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (QuestionAnswerSet_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = QuestionAnswerSet_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new QuestionAnswerSet(addr, QuestionAnswerSet_Type.this);
  			   QuestionAnswerSet_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new QuestionAnswerSet(addr, QuestionAnswerSet_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = QuestionAnswerSet.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.lti.qalab.types.QuestionAnswerSet");
 
  /** @generated */
  final Feature casFeat_question;
  /** @generated */
  final int     casFeatCode_question;
  /** @generated */ 
  public int getQuestion(int addr) {
        if (featOkTst && casFeat_question == null)
      jcas.throwFeatMissing("question", "edu.cmu.lti.qalab.types.QuestionAnswerSet");
    return ll_cas.ll_getRefValue(addr, casFeatCode_question);
  }
  /** @generated */    
  public void setQuestion(int addr, int v) {
        if (featOkTst && casFeat_question == null)
      jcas.throwFeatMissing("question", "edu.cmu.lti.qalab.types.QuestionAnswerSet");
    ll_cas.ll_setRefValue(addr, casFeatCode_question, v);}
    
  
 
  /** @generated */
  final Feature casFeat_answerList;
  /** @generated */
  final int     casFeatCode_answerList;
  /** @generated */ 
  public int getAnswerList(int addr) {
        if (featOkTst && casFeat_answerList == null)
      jcas.throwFeatMissing("answerList", "edu.cmu.lti.qalab.types.QuestionAnswerSet");
    return ll_cas.ll_getRefValue(addr, casFeatCode_answerList);
  }
  /** @generated */    
  public void setAnswerList(int addr, int v) {
        if (featOkTst && casFeat_answerList == null)
      jcas.throwFeatMissing("answerList", "edu.cmu.lti.qalab.types.QuestionAnswerSet");
    ll_cas.ll_setRefValue(addr, casFeatCode_answerList, v);}
    
  
 
  /** @generated */
  final Feature casFeat_candidateSentenceList;
  /** @generated */
  final int     casFeatCode_candidateSentenceList;
  /** @generated */ 
  public int getCandidateSentenceList(int addr) {
        if (featOkTst && casFeat_candidateSentenceList == null)
      jcas.throwFeatMissing("candidateSentenceList", "edu.cmu.lti.qalab.types.QuestionAnswerSet");
    return ll_cas.ll_getRefValue(addr, casFeatCode_candidateSentenceList);
  }
  /** @generated */    
  public void setCandidateSentenceList(int addr, int v) {
        if (featOkTst && casFeat_candidateSentenceList == null)
      jcas.throwFeatMissing("candidateSentenceList", "edu.cmu.lti.qalab.types.QuestionAnswerSet");
    ll_cas.ll_setRefValue(addr, casFeatCode_candidateSentenceList, v);}
    
  
 
  /** @generated */
  final Feature casFeat_preAnswer;
  /** @generated */
  final int     casFeatCode_preAnswer;
  /** @generated */ 
  public String getPreAnswer(int addr) {
        if (featOkTst && casFeat_preAnswer == null)
      jcas.throwFeatMissing("preAnswer", "edu.cmu.lti.qalab.types.QuestionAnswerSet");
    return ll_cas.ll_getStringValue(addr, casFeatCode_preAnswer);
  }
  /** @generated */    
  public void setPreAnswer(int addr, String v) {
        if (featOkTst && casFeat_preAnswer == null)
      jcas.throwFeatMissing("preAnswer", "edu.cmu.lti.qalab.types.QuestionAnswerSet");
    ll_cas.ll_setStringValue(addr, casFeatCode_preAnswer, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public QuestionAnswerSet_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_question = jcas.getRequiredFeatureDE(casType, "question", "edu.cmu.lti.qalab.types.Question", featOkTst);
    casFeatCode_question  = (null == casFeat_question) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_question).getCode();

 
    casFeat_answerList = jcas.getRequiredFeatureDE(casType, "answerList", "uima.cas.FSList", featOkTst);
    casFeatCode_answerList  = (null == casFeat_answerList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_answerList).getCode();

 
    casFeat_candidateSentenceList = jcas.getRequiredFeatureDE(casType, "candidateSentenceList", "uima.cas.FSList", featOkTst);
    casFeatCode_candidateSentenceList  = (null == casFeat_candidateSentenceList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_candidateSentenceList).getCode();

 
    casFeat_preAnswer = jcas.getRequiredFeatureDE(casType, "preAnswer", "uima.cas.String", featOkTst);
    casFeatCode_preAnswer  = (null == casFeat_preAnswer) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_preAnswer).getCode();

  }
}



    