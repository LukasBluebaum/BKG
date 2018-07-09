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
 * @author Nick Düsterhus
 * @author Lukas Blübaum
 *
 */
public class FoxThread implements Runnable {

	private static final FoxWebservice SERVICE = new FoxWebservice();
				
	private Model graph;
	
	private File model;
	
	private BlockingQueue<List<CoreMap>> articles;
	
	/**Class Constructor 
	 * 
	 * 
	 * @param graph an Apache Jena model this thread will write to
	 * @param model a file this thread will write the model in
	 * @param articles a BlockingQueue containing a List of CoreMap (Stanford CoreNLP)
	 */
	public FoxThread(Model graph,File model, BlockingQueue<List<CoreMap>> articles) {
		this.graph = graph;
		this.articles = articles;
		this.model = model;
	}
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	
	public void run() {
		FileWriter writer = null;		
		try {
			writer = new FileWriter(model);
			
			int currentLine = 0;
			while(true) {
				currentLine++;
				List<CoreMap> article = articles.take();
				if(article.size() == 0) {					
					break;
				}
				try {
					getRelationsFox(article);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				
				if(currentLine >= RelationExtraction.ARTICLESPERWRITE) {
					graph.enterCriticalSection(Lock.WRITE);
					try {
						System.out.println("Fox Enter Critical. Write.");
						graph.write(writer, "TTL");
					} finally {
						System.out.println("Fox Leave Critical. Write.");
						graph.leaveCriticalSection();
					}
					currentLine = 0;
				}
			}               
 	 		
			System.out.println("-----");
			graph.enterCriticalSection(Lock.WRITE);
			try {
				System.out.println("Fox Enter Critical. Write.");
				graph.write(writer, "TTL");
			} finally {
				System.out.println("Fox Leave Critical. Write.");
				graph.leaveCriticalSection();
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

	
	/**Extracts subject predicate and object from a Fox response and writes the triple to the graph
	 * 
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
		//System.out.println("Fox:" + triple);		
		
		graph.enterCriticalSection(Lock.WRITE);
		try {
			System.out.println("Fox Enter Critical. Add triple.");
			graph.add(triple);	
			graph.write(System.out, "TTL");
		} finally {
			System.out.println("Fox Leave Critical. Add triple.");
			graph.leaveCriticalSection();
		}
		
	}
	
	/**
	 * Sends each sentence to via FoxWebservice to the Fox demo and reads the returned model.
	 * Calls @see getTriple to iterate over the model.
	 * 
	 * @param sentences a List of CoreMap 
	 * @throws MalformedURLException
	 * @throws ProtocolException
	 * @throws IOException
	 * @throws ParseException
	 */
	private void getRelationsFox(final List<CoreMap> sentences) throws MalformedURLException, ProtocolException, IOException, ParseException {	
	    for(CoreMap sentence: sentences) {
	    	Model model = ModelFactory.createDefaultModel() ;
    		try {
				model.read(new ByteArrayInputStream(SERVICE.extract(sentence.toString(), "en" , "re").getBytes()),null, "TTL");
			} catch (Exception e) {
				e.printStackTrace();
			}
    		StmtIterator iterator = model.listStatements();
    					    		
    		while(iterator.hasNext())
    		{
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
