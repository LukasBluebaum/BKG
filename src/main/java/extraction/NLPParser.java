package extraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;

public class NLPParser {
	private static final  StanfordCoreNLP pipeline;
	
	private static final  String ANNOTATORS = "tokenize, ssplit, pos, depparse";
			
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
        pipeline.prettyPrint(annotation, System.out);
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

        verbs = getWords(sentences, "V");
        nouns = getWords(sentences, "N");
        adjectives = getWords(sentences, "JJ");
        return sentences;
 	}
	
	private ArrayList<String> getWords(List<CoreMap> sentences, String tag) {
 		ArrayList<String> words = new ArrayList<String>();
 		
 		for (CoreMap sentence : sentences) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for(CoreLabel t: tokens) {
               	if(t.tag().startsWith(tag)){
            		String word = t.toString();
            		words.add(word.substring(0, word.lastIndexOf("-")));
            	}
            }
        }       	
 		return words;
 	}
	
	public void getDependencies(List<CoreMap> sentences) {
        for (CoreMap sentence : sentences) {
            SemanticGraph basicDeps = sentence.get(BasicDependenciesAnnotation.class);
            Collection<TypedDependency> typedDeps = basicDeps.typedDependencies();   
            System.out.println(typedDeps);
        }
        
 	}
}
