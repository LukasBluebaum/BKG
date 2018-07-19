package extraction;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import utils.Entity;

/**
 * Requests DBpedia spotlight and processes the output.
 * @author Lukas Blübaum
 * @author Nick Düsterhus
 * @author Monika Werner
 *
 */
public class SpotlightWebservice {
	
    private final String REQUESTURL = "http://model.dbpedia-spotlight.org/en/annotate";
    
	private final String CONFIDENCE = "0.45"; 
	
	private final String SUPPORT = "20";	
	
	private final int TIMEOUT = 10000;
	
	/**
	 * Sets the parameters for the next Spotlight request and calls {@link #requestPOST(String, String)}.
	 * @param inputText Current sentences.
	 * @return Output from DBpedia Spotlight.
	 * @throws IOException
	 */
	public String getEntities(final String inputText) throws IOException {

		String urlParameters = "text=" + URLEncoder.encode(inputText, "UTF-8");
		urlParameters += "&confidence=" + CONFIDENCE;
		urlParameters += "&support=" + SUPPORT;

		return requestPOST(urlParameters, REQUESTURL);
	}
		
	/**
	 * Sends a POST request to the Spotlight demo.
	 * @param urlParameters Parameters for the request.
	 * @param requestURL Spotlight demo url.
	 * @return Response from Spotlight.
	 */
	private String requestPOST(final String urlParameters, final String requestURL){
		try {	
			URL url = new URL(requestURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			connection.setRequestProperty("Content-Length", String.valueOf(urlParameters.length()));
			connection.setConnectTimeout(TIMEOUT);
	
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
	
			InputStream inputStream = connection.getInputStream();
			InputStreamReader in = new InputStreamReader(inputStream);
			BufferedReader reader = new BufferedReader(in);
	
			StringBuilder sb = new StringBuilder();
			while (reader.ready()) {
				sb.append(reader.readLine());
			}
	
			wr.close();
			reader.close();
			connection.disconnect();
	
			return sb.toString();
		} catch(IOException e) {
			return null;
		} 
	}
	
	/**
	 * Processes the response from Spotlight and extracts the entities with their uris, types
	 * surface forms and offsets.
	 * @param response A response from the spotlight online demo.
	 * @return List of processed entities.
	 * @throws ParseException
	 */
	public List<Entity> postProcessing(final String response) throws ParseException {
		
		List<Entity> namedEntities = new ArrayList<>();
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = (JSONObject) parser.parse(response);

		JSONArray resources = (JSONArray) jsonObject.get("Resources");
		if (resources != null) {
			for (Object res : resources.toArray()) {
				JSONObject next = (JSONObject) res;
				String uri = ((String) next.get("@URI"));
				String type = ((String) next.get("@types"));
				String surface = ((String) next.get("@surfaceForm"));
				int offset = Integer.parseInt((String) next.get("@offset"));
				Entity entity = new Entity(type,uri,surface, offset);
				namedEntities.add(entity);
			}
		}
		return namedEntities;
	}
	
}
