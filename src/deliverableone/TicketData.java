package deliverableone;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TicketData {
	private static String readAll(Reader rd) throws IOException {
	      StringBuilder sb = new StringBuilder();
	      int cp;
	      while ((cp = rd.read()) != -1) {
	         sb.append((char) cp);
	      }
	      return sb.toString();
	   }

public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
  InputStream is = new URL(url).openStream();
  try {
     BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
     String jsonText = readAll(rd);
     JSONArray json = new JSONArray(jsonText);
     return json;
   } finally {
     is.close();
   }
}

public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
  InputStream is = new URL(url).openStream();
  try(BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))) {
     String jsonText = readAll(rd);
     JSONObject json = new JSONObject(jsonText);
     return json;
   } finally {
     is.close();
   }
}

public static List<String> retrieveTickID() throws JSONException, IOException {
	   String projName ="MAHOUT";
	   Integer j = 0, i = 0, total = 1;
	   List<String> ids = new ArrayList<>();
    //Get JSON API for closed bugs w/ AV in the project
    do {
       //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
       j = i + 1000;
       String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
              + projName + "%22AND%22issueType%22=%22New%20Feature%22AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created&startAt="
              + i.toString() + "&maxResults=" + j.toString();
       JSONObject json = readJsonFromUrl(url);
       JSONArray issues = json.getJSONArray("issues");
       total = json.getInt("total");
       for (; i < total && i < j; i++) {
          //Iterate through each bug
          String key = issues.getJSONObject(i%1000).get("key").toString();
          ids.add(key);
       } 
    } while (i < total);
    return ids;
}

public static void csvWriter() throws IOException, InterruptedException {
	Integer i = 1,k;
	String msg,dt;
	FileWriter csvWriter = new FileWriter("dataCommit.csv");
	try {
		csvWriter.append("date");
		csvWriter.append(",");
		csvWriter.append("message");
		csvWriter.append("\n");
		for(;;i++) {
			String url = "https://api.github.com/repos/apache/mahout/commits?page="+i.toString()+"&per_page=100";
			Thread.sleep(1000);
	  	JSONArray json = readJsonArrayFromUrl(url);
	  	Integer l = json.length();
	  	if(l == 0) {
	  		System.out.println(json.length());
	  		csvWriter.flush();
	  		csvWriter.close();
	  		return;
	  	}
	  	for( k=0 ; k<l ; k++ ) {
	  		if(l != 0) {
	  			msg = json.getJSONObject(k).getJSONObject("commit").getString("message");
	  			dt = json.getJSONObject(k).getJSONObject("commit").getJSONObject("committer").getString("date");
	  			csvWriter.append(dt);
	  			csvWriter.append(",");
	  			csvWriter.append(msg);
	  			csvWriter.append("\n");
	  		}
	  	}
		}
		
	} catch(Exception e) {
		
	}finally {
		csvWriter.close();
	}
	
}



public static void main(String[] args) throws IOException, JSONException, InterruptedException {
	String row,toFind;
	Integer i,match1,match2,match3;
	List<String> rawData = new ArrayList<>();
	List<String> parsedData1 = new ArrayList<>();
	FileWriter csvWriter = new FileWriter("finalData.csv");

	
	File csvFile = new File("dataCommit.csv");
	if(!csvFile.isFile()) {
		System.out.println("caching file...");
		csvWriter();
	}
	
	List<String> ids = retrieveTickID();
	for(i = 0 ; i < ids.size(); i++) {
		toFind = ids.get(i);
		//System.out.println("ID TO FIND : " + toFind );
		BufferedReader csvReader = new BufferedReader(new FileReader("dataCommit.csv"));
		while ((row = csvReader.readLine()) != null) {
		    String[] dataCommit = row.split(",");
		    if(dataCommit.length >= 2) {
		    	match1 = StringUtils.countMatches(dataCommit[1],"[" + toFind + "]");
		    	match2 = StringUtils.countMatches(dataCommit[1],toFind + ":");
		    	match3 = StringUtils.countMatches(dataCommit[1],toFind + " ");
		    	//System.out.println(dataCommit[0]+","+dataCommit[1]);
		    	if(match1 >= 1 || match2 >= 1 || match3 >=1) {
		    		//System.out.println("Match found!");
		    		String[] date = dataCommit[0].split("T");
		    		String[] y_m = date[0].split("-");
		    		rawData.add(toFind + ":" + y_m[0]+"-"+y_m[1]);
		    	}
		    }
		}
		csvReader.close();
		//System.out.println(ids.get(i));
	}
	
	for(String e : rawData) {
		String[] s = e.split(":");
		parsedData1.add(s[0]);
	}
	
	int count = 0;
	List<Integer> toDelete = new ArrayList<Integer>();
	for(String elem : parsedData1) {
		//System.out.println("cerco elemento con id "+elem);
		for(int k=0;k<rawData.size();k++) {
			String id = rawData.get(k).split(":")[0];
			if(elem.equals(id)) {
				count++;
				if(count > 1) {
					toDelete.add(k);
				}
			}
		}
		//System.out.println("Per "+elem+"ho trovato questi doppioni");
		//System.out.println(toDelete.toString());
		for(int del : toDelete) {
			rawData.remove(del);
		}
		count = 0;
		toDelete.clear();
	}
	
	List<String> date = new ArrayList<>();

	int countCommit;
	for(String s1 : rawData) {
		countCommit = 0;
		String[] info = s1.split(":");
		date.add(info[1]);
	}
	
	Collections.sort(date);
	
	List<Integer> commitCount = new ArrayList<Integer>();
	List<String> dateCount = new ArrayList<>();
	String[] d = date.get(0).split("-");
	Integer yearMin = Integer.parseInt(d[0]);
	Integer monthMin = Integer.parseInt(d[1]);
	d = date.get(date.size()-1).split("-");
	Integer yearMax = Integer.parseInt(d[0]);
	Integer monthMax = Integer.parseInt(d[1]);
	System.out.println("yearMax : "+yearMax);
	for(int year = yearMin;year <= yearMax; year++) {
		System.out.println(year);
		for(int month = 1;month <= 12;month++) {
			System.out.println(month);
			if(month < 10) {
				dateCount.add(year+"-0"+month);
				commitCount.add(Collections.frequency(date, year+"-0"+month));
			}
			else {
				dateCount.add(year+"-"+month);
				commitCount.add(Collections.frequency(date, year+"-"+month));
			}
			
		}
	}

	try {
		csvWriter.append("Date");
		csvWriter.append(";");
		csvWriter.append("Number of commits");
		csvWriter.append("\n");
		for(int j = 0;j<dateCount.size();j++) {
			csvWriter.append(dateCount.get(j).toString());
			csvWriter.append(";");
			csvWriter.append(commitCount.get(j).toString());
			csvWriter.append("\n");
		}
	}catch(Exception e) {
		
	}finally {
		csvWriter.close();
	}

}

}
