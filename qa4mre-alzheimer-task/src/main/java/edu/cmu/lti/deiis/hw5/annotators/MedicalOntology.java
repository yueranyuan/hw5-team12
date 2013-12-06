package edu.cmu.lti.deiis.hw5.annotators;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Pattern;

public class MedicalOntology {
  
  private HashMap<String, String> ontoMapNumToName;
  private HashMap<String, String> ontoMapNameToNum;
  
  public MedicalOntology() {
    loadOntology();
  }
  
  public InputStream getStream(String file) {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream(file);
    return is;
  }
  
  /**
   * 
   * @param name
   * @return An array of parents in ascending order of relevance
   */
  public String[] FindParents(String name) {
    name = name.toLowerCase();
    if (!ontoMapNameToNum.containsKey(name)) {
      return null;
    }
    String num = ontoMapNameToNum.get(name);
    String[] toks = num.split(Pattern.quote("."));
    
    String[] parents = new String[toks.length - 1];
    for (int i = 0; i < toks.length - 1; i++) {
      String parentNum = "";
      for (int j = 0; j < i; j++) {
        parentNum += toks[j] + ".";
      }
      parentNum += toks[i];
      parents[i] = ontoMapNumToName.get(parentNum);
    }
    return parents;
  }
  
  protected void loadOntology() {
    System.out.println("loading ontology...");
    ontoMapNumToName = new HashMap<String, String>();
    ontoMapNameToNum = new HashMap<String, String>();
    InputStream is = getStream("ontology/parsed_onto.xml");
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String line;
    try {
      while ((line = reader.readLine()) != null) { 
        String[] toks = line.split(":");
        if (toks.length == 2) {
          String num = toks[0].toLowerCase();
          String name = toks[1].toLowerCase();
          if (!ontoMapNumToName.containsKey(num)) {
            ontoMapNumToName.put(num, name);
          }
          ontoMapNameToNum.put(name, num);
        }
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      is.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    System.out.println(ontoMapNameToNum.size() + " entries loaded");
  }
}
