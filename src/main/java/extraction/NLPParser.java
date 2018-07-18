package extraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.coref.data.Dictionaries;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;

/**
 * Responsible for sentence splitting, coreference resolution and binary relation detection.
 * @author Nick Düsterhus
 * @author Lukas Blübaum
 * @author Monika Werner
 */

public class NLPParser {
	
	private static final  StanfordCoreNLP PIPELINERELATIONS;
	
	private static final  String ANNOTATORSRELATIONS = "tokenize, ssplit, pos,lemma, depparse, natlog, openie";
	
	private static final  StanfordCoreNLP PIPELINEREF;
	
	private static final  String ANNOTATORSREF = "pos,lemma,ner,parse,coref";
	
	private static final  StanfordCoreNLP PIPELINESENTENCES;
	
	private static final  String ANNOTATORSSENTENCES = "tokenize,ssplit";
	
	private static final List<String> REFERENCES = Arrays.asList("he", "his", "him", "she", "her", "it", "its");
			
	
	static {
		Properties props = new Properties();
	    props.setProperty("annotators",ANNOTATORSRELATIONS);
	    PIPELINERELATIONS = new StanfordCoreNLP(props);
	    
	    Properties props2 = new Properties();
	    props2.setProperty("annotators",ANNOTATORSREF);
	    props2.setProperty("enforceRequirements", "false");
	    PIPELINEREF = new StanfordCoreNLP(props2);
	    
	    Properties props3 = new Properties();
	    props3.setProperty("annotators",ANNOTATORSSENTENCES);
	    PIPELINESENTENCES = new StanfordCoreNLP(props3);
	}

	/** Splits the given string into sentences and finds all binary relations and stores them into a list of
	 * CoreMaps using Stanford OpenIE.
	 * @param article
	 * @return 
	 */
	public List<CoreMap> calculateRelations(String article) {

        Annotation annotation = new Annotation(article);
        PIPELINERELATIONS.annotate(annotation);

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
    
        return sentences;
 	}
	
	
	
	/** Splits the given String into sentences.
	 * @param article
	 * @return List of sentences.
	 */
	public List<CoreMap> getSentences(String article) {
        Annotation annotation = new Annotation(article);
        PIPELINESENTENCES.annotate(annotation);

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
    
        return sentences;
 	}
	
	/**
	 * Checks if the given String is a noun or a verb. 
	 * @param word A String containing a single word.
	 * @return True if the string is a noun or verb, false otherwise.
	 */
	public boolean isNounOrVerb(String word) {
		Annotation annotation = new Annotation(word);
		PIPELINERELATIONS.annotate(annotation);

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        CoreMap sentence = sentences.get(0);
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        CoreLabel token = tokens.get(0);
        return token.tag().startsWith("N") || token.tag().startsWith("V");      
	}
		
	
	/**
	 * Returns all binary relations from a list of sentences.
	 * @param sentences List of CoreMaps.
	 * @return A Map with the index of the sentence as key and a Collection of RelationTriples as values.
	 */
	public Map<Integer, Collection<RelationTriple>> binaryRelation(List<CoreMap> sentences){
		Map<Integer, Collection<RelationTriple>> binaryRelations = new LinkedHashMap<>();

		for(int i = 0; i<sentences.size(); i++) {
		    binaryRelations.put(i, sentences.get(i).get(NaturalLogicAnnotations.RelationTriplesAnnotation.class));
		}
		return binaryRelations;    
	}	
	
	
	
	/**
	 * Replaces all annotated coreferences with their representative mention.
	 * @param article A list of sentences.
	 * @return The given list of sentences as a string with all coreferences replaced by their representative mention.
	 */
	public String coreferenceResolution(List<CoreMap> article) {
		Annotation document = new Annotation(article);
		PIPELINEREF.annotate(document);	    
	    ArrayList<CoreMap> sentences = (ArrayList<CoreMap>) document.get(CoreAnnotations.SentencesAnnotation.class);	      
	    Map<Integer, CorefChain> coreChain = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
	    
	    String newArticle = "";
	    for(CoreMap sentence: sentences) {
	    	List<CoreLabel> tokens = (List<CoreLabel>) sentence.get(CoreAnnotations.TokensAnnotation.class);
	    	
	    	for(int i = 0; i<tokens.size(); i++) {
	    		
	    		Integer id = tokens.get(i).get(CorefCoreAnnotations.CorefClusterIdAnnotation.class);
	    		CorefChain chain = coreChain.get(id);
	    		String value = tokens.get(i).value().toLowerCase();
	    		
	    		// no coreference just use the word as it is
	    		if(chain == null) {
	    			newArticle += tokens.get(i).value() + " ";
	    			continue;
	    		}
	    		if(!REFERENCES.contains(value)) {
	    			if(i < tokens.size()-1 && tokens.get(i+1).word().startsWith("'")) {
	    				newArticle += tokens.get(i).value();
	    			} else {
	    				newArticle += tokens.get(i).value() + " ";
	    			}		    			
	    		} else {		    			
	    			boolean found = false;
	    			for(CorefMention mention: chain.getMentionsInTextualOrder()) {
	    				if(mention.mentionType.equals(Dictionaries.MentionType.PRONOMINAL)) {
	    					found = true;
	    					break;
	    				}
	    			}
	    			if(found) {
	    				String rep = chain.getRepresentativeMention().toString();
	    				if(value.equals("his") || value.equals("its") || value.equals("hers") || isHerGenitive(value,sentence)) {
	    					newArticle += rep.substring(1, rep.lastIndexOf("\"")).replace("'s", "").trim() + "'s ";
	    				} else {
	    					newArticle += rep.substring(1, rep.lastIndexOf("\"")).replace("'s", "").trim() + " ";
	    				}				    		
	    			} else {
	    				newArticle += tokens.get(i).value() + " ";
	    			}	    			
	    		}	
	    	}
	    }	
	    return newArticle;
	}
	
	
	/**
	 * Called by {@link #coreferenceResolution(List)}.
	 * This method determines if her is used as a possessive pronoun in this case. Therefore it looks
	 * for the poss modifier.
	 * @param value 
	 * @param sentence Current sentence.
	 * @return True the given value is used as a possessive pronoun, false otherwise
	 */
	private static boolean isHerGenitive(String value, CoreMap sentence) {
		if(value.equals("her")) {
			SemanticGraph basicDeps = sentence.get(BasicDependenciesAnnotation.class);
            Collection<TypedDependency> typedDeps = basicDeps.typedDependencies();                    
            Iterator<TypedDependency> dependencyIterator = typedDeps.iterator();
            
            while(dependencyIterator.hasNext()) {
            	TypedDependency dependency = dependencyIterator.next();
            	String depString = dependency.reln().toString();		    		            	
            	if(depString.contains("poss")) {
            		String dep = dependency.dep().toString();
            		dep = dep.substring(0, dep.lastIndexOf("/"));
            		String gov = dependency.gov().toString();
            		gov = gov.substring(0, gov.lastIndexOf("/"));            		
            		return dep.equals("her") || gov.equals("her");            		
            	}         	
            }
		}
		return false;
	}
	
	
	/** 
	 * Returns the lemmanization of the given string.
	 * @param input 
	 * @return The lemmanization of the given string.
	 */
	public static String getLemma(String input) {
 		Annotation noun = new Annotation(input);
 		PIPELINERELATIONS.annotate(noun);
 		List<CoreMap> sentences = noun.get(SentencesAnnotation.class);
 		if(sentences.size() > 1) return null;
 		CoreMap sentence = sentences.get(0);
 		List<CoreLabel> token = sentence.get(TokensAnnotation.class);
 		if(token.size() > 1) return null;
 		return token.get(0).get(LemmaAnnotation.class);
 	}
}
