package extraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class BinaryTest {
	private static final StanfordCoreNLP pipeline;
	
	private static final String ANNOTATORS = "tokenize,ssplit,pos,lemma,depparse,natlog,openie";
	
	
	
	static {
		Properties props = new Properties();
	    props.setProperty("annotators",ANNOTATORS);
	    pipeline = new StanfordCoreNLP(props);
	}
	public static void main(String[] args) {	
			System.out.println("------");
		 	Annotation doc = new Annotation("Who was the doctoral supervisor of Albert Einstein");
		 	
			pipeline.annotate(doc);
			for (CoreMap sentence : doc.get(CoreAnnotations.SentencesAnnotation.class)) {
			      // Get the OpenIE triples for the sentence
			      Collection<RelationTriple> triples =
				          sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
			      // Print the triples
			      for (RelationTriple triple : triples) {
			        System.out.println(triple.confidence + "\t" +
			            triple.subjectLemmaGloss() + "\t" +
			            triple.relationLemmaGloss() + "\t" +
			            triple.objectLemmaGloss());
			      }
			    }

	}}
