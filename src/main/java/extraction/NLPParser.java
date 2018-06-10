package extraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.CoreMap;

public class NLPParser {
	private static final  StanfordCoreNLP pipeline;
	
	private static final  String ANNOTATORS = "tokenize, ssplit, pos,lemma, depparse, natlog, openie";
			
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

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
    
        return sentences;
 	}
		
	public Map<Integer, Collection<RelationTriple>> binaryRelation(List<CoreMap> sentences){
		Map<Integer, Collection<RelationTriple>> binaryRelations = new LinkedHashMap<>();

		for(int i = 0; i<sentences.size(); i++) {
		    binaryRelations.put(i, sentences.get(i).get(NaturalLogicAnnotations.RelationTriplesAnnotation.class));
		}
		return binaryRelations;    
	}	
	
	public Map<Integer, Collection<RelationTriple>> binaryRelation2(List<CoreMap> sentences){
		Map<Integer, Collection<RelationTriple>> binaryRelations = new LinkedHashMap<>();

		 Document doc = new Document("In late August 1961, Barack and his mother moved to the University of Washington in Seattle, where they lived for a year.");
		 
		for (Sentence sent : doc.sentences()) {		    
		      for (RelationTriple triple : sent.openieTriples()) {
		        // Print the triple
		    	
		        System.out.println(triple.confidence + "\t" +
		            triple.subjectLemmaGloss() + "\t" +
		            triple.relationLemmaGloss() + "\t" +
		            triple.objectLemmaGloss());
		      }
		    }
		return binaryRelations;    
	}
}
