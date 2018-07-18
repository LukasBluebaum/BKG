package extraction;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.shared.Lock;
import org.json.simple.parser.ParseException;

import edu.stanford.nlp.util.CoreMap;

/**
 * Performs relation extraction using the FOX demo for relation extraction.
 * @author Nick Düsterhus
 * @author Lukas Blübaum
 * @author Monika Werner
 */
public class FoxThread implements Runnable {

	private static final FoxWebservice SERVICE = new FoxWebservice();
				
	private Model graph;
	
	private File model;
	
	private BlockingQueue<List<CoreMap>> articles;
	
	/**
	 * Constructor, initializes graph, model and BlockingQueue.
	 * @param graph An Apache Jena model this thread will write to.
	 * @param model A file this thread will write the model to.
	 * @param articles A BlockingQueue containing a List of CoreMaps (Stanford CoreNLP).
	 */
	public FoxThread(Model graph,File model, BlockingQueue<List<CoreMap>> articles) {
		this.graph = graph;
		this.articles = articles;
		this.model = model;
	}
	
	/**
	 * Takes the next line from the blocking queue, then calls {@link #getRelationsFox()} on this line.
	 * Writes the graph to a file.
	 */
	@Override
	public void run() {
		FileWriter writer = null;		
		try {			
			while(true) {
				List<CoreMap> nextLine = articles.take();
				if(nextLine.size() == 0) break;					

				getRelationsFox(nextLine);
				
				graph.enterCriticalSection(Lock.WRITE);
				try {
					writer = new FileWriter(model,false);
					graph.write(writer, "TTL");
				} finally {
					graph.leaveCriticalSection();
				}
			}               
		} catch ( IOException | InterruptedException e) {
			e.printStackTrace();   
		} finally{
			try {
				writer.close();				
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 	
	}
	
	/**
	 * Extracts subject predicate and object from a FOX response and writes the triple to the graph. 
	 * @param statement
	 * @param iterator
	 */
	private void getTriple(Statement statement, StmtIterator iterator) {
		Resource subject = ResourceFactory.createResource(statement.getObject().toString());
		Statement next = iterator.next();
		Property predicate = ResourceFactory.createProperty(next.getObject().toString());
		next = iterator.next();
		
		RDFNode object = null;
		if(next.getObject().isResource()) {
			 object = ResourceFactory.createResource(next.getObject().toString());
		} else {
			object = ResourceFactory.createStringLiteral(next.getObject().toString());
		}	
		Statement triple = ResourceFactory.createStatement(subject, predicate, object);	
		System.out.println(triple);
		graph.enterCriticalSection(Lock.WRITE);
		try {
			graph.add(triple);	
		} finally {
			graph.leaveCriticalSection();
		}
		
	}
	
	/**
	 * Sends each sentence via the FoxWebservice to the FOX online demo and reads the returned model.
	 * Calls {@link #getTriple()} to iterate over the model.
	 * 
	 * @param sentences A List of CoreMaps.
	 * @throws MalformedURLException
	 * @throws ProtocolException
	 * @throws IOException
	 * @throws ParseException
	 */
	private void getRelationsFox(final List<CoreMap> sentences) {	
	    for(CoreMap sentence: sentences) {
	    	Model model = ModelFactory.createDefaultModel() ;
    		try {
				model.read(new ByteArrayInputStream(SERVICE.extract(sentence.toString(), "en" , "re").getBytes()),null, "TTL");
				System.out.println("-");
			} catch (IOException e) {
				e.printStackTrace();
			}
    		StmtIterator iterator = model.listStatements();
    					    		
    		while(iterator.hasNext()) {
    			Statement s = iterator.next();
    			if(s.getPredicate().toString().contains("subject")) {
    				getTriple(s , iterator);
    			}
    		}
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    }	     	 
	}
}
