package wikicleaner;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class WikiCleaner {
	
	protected final static int WORKERAMOUNT = 4;
	
	protected final static String END = "END";
	
	private final static int QUEUESIZE = 10000;
	
	public void cleanWikiDump(File input, File output) {
		BlockingQueue<String> readQueue = new ArrayBlockingQueue<String>(QUEUESIZE);	    
	    BlockingQueue<String> writeQueue = new ArrayBlockingQueue<String>(QUEUESIZE);
	    
	    Reader reader = new Reader(readQueue, input);
	    Writer writer = new Writer(writeQueue,output);
	    
	    Worker[] workers = new Worker[WORKERAMOUNT];
	    for(int i = 0; i<workers.length; i++) {
	    	workers[i] = new Worker(readQueue,writeQueue);
	    }
	    
	    new Thread(reader).start();
	    for(int i = 0; i<workers.length; i++) {
	    	 new Thread(workers[i]).start();
	    }
	    new Thread(writer).start();
	}
	
	public static void main(String[] args) {
		File input = new File("resources/enwiki-20171103-pages.tsv");
		File output = new File("resources/out.txt");
		
		WikiCleaner cleaner = new WikiCleaner();
		cleaner.cleanWikiDump(input, output);
	}
	
	
}
