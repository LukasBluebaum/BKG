package extraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.coref.data.Dictionaries;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;

public class CorefTest {
	private static final StanfordCoreNLP pipeline;
	
	private static final String ANNOTATORS = "tokenize,ssplit,pos,lemma,ner,parse,coref";
	
	private static final List<String> REFERENCES = Arrays.asList("he", "his", "him", "she", "her", "it", "its");
	
	static {
		Properties props = new Properties();
	    props.setProperty("annotators",ANNOTATORS);
	    pipeline = new StanfordCoreNLP(props);
	}
	public static void main(String[] args) {	
			System.out.println("------");
		 	Annotation document = new Annotation("Michelle met Obama in 1960. She married him during her college year.");
		 	
			pipeline.annotate(document);
		    
		    ArrayList<CoreMap> sentences = (ArrayList<CoreMap>) document.get(CoreAnnotations.SentencesAnnotation.class);
		      
		    Map<Integer, CorefChain> coreChain = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
		    System.out.println(coreChain);
		    
		
		    for(CoreMap sentence: sentences) {
		    	List<CoreLabel> tokens = (List<CoreLabel>) sentence.get(CoreAnnotations.TokensAnnotation.class);
		    	
		    	for(int i = 0; i<tokens.size(); i++) {
		    		
		    		Integer id= tokens.get(i).get(CorefCoreAnnotations.CorefClusterIdAnnotation.class);
		    		CorefChain chain = coreChain.get(id);
		    		String value = tokens.get(i).value().toLowerCase();
		    			 		    		
		    		if(chain == null) {
		    			System.out.print(tokens.get(i).value() + " ");
		    			continue;
		    		}
		    		if(!REFERENCES.contains(value)) {
		    			if(i < tokens.size()-1 && tokens.get(i+1).word().startsWith("'")) {
		    				System.out.print(tokens.get(i).value());
		    			} else {
		    				System.out.print(tokens.get(i).value() + " ");
		    			}		    			
		    		} else {		    			
		    			boolean found = false;
		    			for(CorefMention mention: chain.getMentionsInTextualOrder()) {
		    				if(mention.mentionType.equals(Dictionaries.MentionType.PRONOMINAL)) found = true;
		    			}
		    			if(found) {
		    				String rep = chain.getRepresentativeMention().toString();
		    				if(value.equals("his") || value.equals("its") || value.equals("hers") || herGenitive(value,sentence,rep)) {
		    					System.out.print(rep.substring(1, rep.lastIndexOf("\"")).replace("'s", "").trim() + "'s ");
		    				} else {
		    					System.out.print(rep.substring(1, rep.lastIndexOf("\"")).replace("'s", "").trim() + " ");
		    				}				    		
		    			} else {
		    				System.out.print(tokens.get(i).value() + " ");
		    			}
		    			
		    		}	
		    	}
		    	System.out.println();
		    }		   		  	
	}
	
	private static boolean herGenitive(String value, CoreMap sentence, String rep) {
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
}