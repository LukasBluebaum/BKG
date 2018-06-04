package extraction;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import utils.Pair;

public class NLPParser {
	private static final  StanfordCoreNLP pipeline;
	
	private static final  String ANNOTATORS = "tokenize, ssplit, pos, parse";
			
	protected ArrayList<String> verbs;
	
	protected ArrayList<String> adjectives;
	
	protected ArrayList<String> nouns;
		
	static {
		Properties props = new Properties();
	    props.setProperty("annotators",ANNOTATORS);
	    pipeline = new StanfordCoreNLP(props);
	}

	public List<CoreMap> getSentences(String question) {
        String content = question;
        System.out.println(content);

        Annotation annotation = new Annotation(content);
        pipeline.annotate(annotation);
//        pipeline.prettyPrint(annotation, System.out);
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

        return sentences;
 	}
	
	public void binary(List<CoreMap> sentences) {
		 ArrayList<Pair[]> compounds = new ArrayList<Pair[]>();
		 ArrayList<Pair[]> subjectsTemp = new ArrayList<Pair[]>();
		 ArrayList<Pair[]> nmod = new ArrayList<Pair[]>();
		 
		 for (CoreMap sentence : sentences) {
	            SemanticGraph basicDeps = sentence.get(BasicDependenciesAnnotation.class);
	            Collection<TypedDependency> typedDeps = basicDeps.typedDependencies();
	         
	            System.out.println("typedDeps: "+ typedDeps);
	            Iterator<TypedDependency> t = typedDeps.iterator();
	            while(t.hasNext()) {
	            	TypedDependency s = t.next();
	            	String c = s.reln().toString();
	            	if(c.equals("compound")) {
	            		String dep = s.dep().toString();
	            		String gov = s.gov().toString();
	            		Pair first = new Pair(dep.substring(0, dep.lastIndexOf("/")),s.dep().index());
	            		Pair second = new Pair(gov.substring(0, gov.lastIndexOf("/")),s.gov().index());
	            		compounds.add(new Pair[] {first,second});
	            	}
	            	if(c.contains("nsubj")) {
	            		String dep = s.dep().toString();
	            		String gov = s.gov().toString();
	            		Pair first = new Pair(dep.substring(0, dep.lastIndexOf("/")),s.dep().index());
	            		Pair second = new Pair(gov.substring(0, gov.lastIndexOf("/")),s.gov().index());
	            		subjectsTemp.add(new Pair[] {first,second});
	            	}
	            	if(c.contains("nmod")) {
	            		String dep = s.dep().toString();
	            		String gov = s.gov().toString();
	            		Pair first = new Pair(gov.substring(0, gov.lastIndexOf("/")),s.gov().index() );
	            		Pair second = new Pair(dep.substring(0, dep.lastIndexOf("/")),s.dep().index());	       
	            		nmod.add(new Pair[] {first,second});
	            	}
	            }
	     }  
		 
		 System.out.println("\nCompounds:");
		 for(Pair[] s: compounds) {
			 System.out.println(s[0] + " " + s[1]);
		 }
		 
		 System.out.println("\nSubjectsTemp:");
		 for(Pair[] s: subjectsTemp) {
			 System.out.println(s[0] + " " + s[1]);
		 }
		 
		 System.out.println("\nNmod: ");
		 for(Pair[] s: nmod) {
			 System.out.println(s[0] + " " + s[1]);
		 }
		 
		 Map<String, String> subjects = new LinkedHashMap<>();
		 boolean found = false;
		 for(Pair[] s: subjectsTemp) {
			 for(Pair[] pair: compounds) {
				 if(pair[1].getSecond().intValue() == s[0].getSecond().intValue()) {
					 subjects.put(s[0].getFirst(), pair[0].getFirst() + " " +  pair[1].getFirst());
					 found = true;
				 }
			 }
			 if(!found) {
				 subjects.put(s[0].getFirst(), s[0].getFirst());
				 found = false;
			 }
		 }
		 
		 System.out.println("\nSubjects:");
		 for(String keyword: subjects.keySet()) {
	 			System.out.println(subjects.get(keyword));
	 	 }
		 
		 Map<String, String> objects = new LinkedHashMap<>();
		 found = false;
		 for(Pair[] s: nmod) {
			 for(Pair[] pair: compounds) {			 
				 if(pair[1].getSecond().intValue() == s[1].getSecond().intValue()) {
					 objects.put(s[1].getFirst(), pair[0].getFirst() + " " +  pair[1].getFirst());
					 found = true;
				 }
			 }
			 if(!found) {
				 objects.put(s[1].getFirst(), s[1].getFirst());
			 }
			 found = false;
		 }
		 
		 System.out.println("\nObjects:");
		 for(String keyword: objects.keySet()) {
	 			System.out.println(objects.get(keyword));
	 	 }
		 
		 Map<String, List<String>> relations = new LinkedHashMap<>();
		 for(int i = 0; i<subjectsTemp.size(); i++) {
			 relations.put(subjects.get(subjectsTemp.get(i)[0].getFirst()), new ArrayList<String>());
			 for(Pair[] s: nmod) {
				 if(subjects.size() == 1 || (i < subjectsTemp.size()-1 && s[0].getSecond().intValue() < subjectsTemp.get(i+1)[0].getSecond().intValue()) 
						 || (i == subjectsTemp.size()-1 && subjectsTemp.get(i)[0].getSecond().intValue() < s[0].getSecond().intValue())) {
					 List<String> list = relations.get(subjects.get(subjectsTemp.get(i)[0].getFirst()));
					 list.add(s[0].getFirst() + " " + objects.get(s[1].getFirst()));
				 }			 
			 }
		 }
		 		 
		 System.out.println("\nRelations: ");
		 	
		 for(String keyword: relations.keySet()) {
	 			for(String property :  relations.get(keyword)) {
		 			System.out.println(keyword + " " + property);
	 			}	
	 	}
 	}
	
	public Map<Integer, Collection<RelationTriple>> binaryRelation(List<CoreMap> sentences){
		Map<Integer, Collection<RelationTriple>> binaryRelations = new LinkedHashMap<>();
		
		Document doc = new Document("Barack Obama is 6 feet tall.");
		
		int i = 0;  
		for (Sentence sent : doc.sentences()) {
		    binaryRelations.put(i, sent.openieTriples());
		}
		return binaryRelations;    
	}	
}
