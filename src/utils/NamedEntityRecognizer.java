package utils;

import java.io.PrintWriter;
import java.util.Properties;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class NamedEntityRecognizer {
	
	protected void printEntities(String sentence)
	{
	    Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref, sentiment");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    
	    Annotation document = new Annotation(sentence);
	    pipeline.annotate(document);
	
	    PrintWriter out = new PrintWriter(System.out); 
	    pipeline.prettyPrint(document, out);	    
	}
}
