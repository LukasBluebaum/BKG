package extraction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

public class test {
	
	public static void main(String[] args) throws IOException {
		Model model = ModelFactory.createDefaultModel() ;
		File out = new File("model.ttl");	
		model.read(new FileInputStream(out),null, "TTL");
		
		StmtIterator iterator = model.listStatements();
		
		Model model2 = ModelFactory.createDefaultModel() ;

		
		while(iterator.hasNext())
		{
			Statement s = iterator.next();
			if(s.getObject().toString().startsWith("h://")) {
				Resource subject = s.getSubject();
				Property predicate = s.getPredicate();
				RDFNode object = ResourceFactory.createResource(s.getObject().toString().substring(0, 1) + "ttp" + s.getObject().toString().substring(1));
				Statement statement = ResourceFactory.createStatement(subject, predicate, object);
				model2.add(statement);
			} else {
				model2.add(s);
			}			
		}
		FileWriter writer = null;	
		writer = new FileWriter(new File("out.ttl"),false);
		model2.write(writer, "TTL");
		writer.close();
	}
}
