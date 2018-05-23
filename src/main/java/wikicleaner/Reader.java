package wikicleaner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class Reader implements Runnable{

	private BlockingQueue<String> readQueue;
  
	private File wikidump;
	
	public Reader(BlockingQueue<String> readQueue, File wikidump){
		this.readQueue = readQueue;     
		this.wikidump = wikidump;
	}

	@Override
	public void run() {
		BufferedReader reader = null;
		try {
    	 		reader = new BufferedReader(new FileReader(wikidump));
            
    	 		String article =null;
    	 		while((article=reader.readLine())!=null) { 
    	 			// only consider lines with valid articles
    	 			if(article.length() > 1 && article.charAt(1) == 'h') {         	
    	 				readQueue.put(article);
    	 			}
    	 		}
    	 		readQueue.put(WikiCleaner.END);  
            
    	 		System.out.println("Reader done.");
		} catch ( IOException | InterruptedException  e) {
			e.printStackTrace();
		} finally{
			try {
				readQueue.put(WikiCleaner.END); 
				reader.close();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}