//package extraction;
//
//import java.io.IOException;
//import java.util.List;
//
//import org.json.simple.parser.ParseException;
//
//import utils.Entity;
//
//
//
//public class RelationThread implements Runnable {
//
//	private NLPParser parser;
//	
//	private String question;
//	
//
//	
//	public RelationThread(NLPParser parser, String question) {
//		this.parser = parser;
//		this.question = question;
//	}
//	
//	@Override
//	public void run() {
//	
//		SpotlightWebservice service = new SpotlightWebservice();
//		List<Entity> entities = null;
//		try {
//			entities = service.getEntitiesProcessed(question);
//		} catch (IOException | ParseException e) {
//			e.printStackTrace();
//		}
//		if(entities == null || entities.size() == 0) return;
//		
//		
//		
//	}
//	
//}