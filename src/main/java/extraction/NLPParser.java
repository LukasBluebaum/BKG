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

public class NLPParser {
	private static final  StanfordCoreNLP pipeline;
	
	private static final  String ANNOTATORS = "tokenize, ssplit, pos,lemma, depparse, natlog, openie";
	
	private static final  StanfordCoreNLP PIPELINEREF;
	
	private static final  String ANNOTATORSREF = "tokenize,ssplit,pos,lemma,ner,parse,coref";
	
	private static final List<String> REFERENCES = Arrays.asList("he", "his", "him", "she", "her", "it", "its");
			
	static {
		Properties props = new Properties();
	    props.setProperty("annotators",ANNOTATORS);
	    pipeline = new StanfordCoreNLP(props);
	    
	    Properties props2 = new Properties();
	    props2.setProperty("annotators",ANNOTATORSREF);
	    PIPELINEREF = new StanfordCoreNLP(props2);
	}

	public List<CoreMap> getSentences(String article) {
        System.out.println(article);

        Annotation annotation = new Annotation(article);
        pipeline.annotate(annotation);

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
    
        return sentences;
 	}
	
	public boolean isNounVerb(String word) {
		Annotation annotation = new Annotation(word);
        pipeline.annotate(annotation);

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        CoreMap sentence = sentences.get(0);
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        CoreLabel token = tokens.get(0);
        return token.tag().startsWith("N") || token.tag().startsWith("V");      
	}
		
	public Map<Integer, Collection<RelationTriple>> binaryRelation(List<CoreMap> sentences){
		Map<Integer, Collection<RelationTriple>> binaryRelations = new LinkedHashMap<>();

		for(int i = 0; i<sentences.size(); i++) {
		    binaryRelations.put(i, sentences.get(i).get(NaturalLogicAnnotations.RelationTriplesAnnotation.class));
		}
		return binaryRelations;    
	}	
	
	public String coreferenceResolution(String article) {
		Annotation document = new Annotation(article);
	 	System.out.println(article);
		PIPELINEREF.annotate(document);	    
	    ArrayList<CoreMap> sentences = (ArrayList<CoreMap>) document.get(CoreAnnotations.SentencesAnnotation.class);	      
	    Map<Integer, CorefChain> coreChain = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
	    
	    String newArticle = "";
	    for(CoreMap sentence: sentences) {
	    	List<CoreLabel> tokens = (List<CoreLabel>) sentence.get(CoreAnnotations.TokensAnnotation.class);
	    	
	    	for(int i = 0; i<tokens.size(); i++) {
	    		
	    		Integer id= tokens.get(i).get(CorefCoreAnnotations.CorefClusterIdAnnotation.class);
	    		CorefChain chain = coreChain.get(id);
	    		String value = tokens.get(i).value().toLowerCase();
	    			 		    		
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
	    				if(value.equals("his") || value.equals("its") || value.equals("hers") || isHerGenitive(value,sentence,rep)) {
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
	
	private static boolean isHerGenitive(String value, CoreMap sentence, String rep) {
		if(value.equals("her")) {
			SemanticGraph basicDeps = sentence.get(BasicDependenciesAnnotation.class);
            Collection<TypedDependency> typedDeps = basicDeps.typedDependencies();
                    
            Iterator<TypedDependency> t = typedDeps.iterator();
            while(t.hasNext()) {
            	TypedDependency s = t.next();
            	String c = s.reln().toString();		    		            	
            	if(c.contains("poss")) {
            		String dep = s.dep().toString();
            		dep = dep.substring(0, dep.lastIndexOf("/"));
            		String gov = s.gov().toString();
            		gov = gov.substring(0, gov.lastIndexOf("/"));            		
            		return dep.equals("her") || gov.equals("her");            		
            	}         	
            }
		}
		return false;
	}
	
	public static String getLemma(String input) {
 		Annotation noun = new Annotation(input);
 		pipeline.annotate(noun);
 		List<CoreMap> sentences = noun.get(SentencesAnnotation.class);
 		if(sentences.size() > 1) return null;
 		CoreMap sentence = sentences.get(0);
 		List<CoreLabel> token = sentence.get(TokensAnnotation.class);
 		return token.get(0).get(LemmaAnnotation.class);
 	}
}
