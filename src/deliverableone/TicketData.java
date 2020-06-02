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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class TicketData {

	private static final String DATA_COMMIT = "dataCommit.csv";
	private static final String FINAL_DATA = "finalData.csv";
	
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
  try(BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));) {
     String jsonText = readAll(rd);
     return new JSONArray(jsonText);
   } finally {
     is.close();
   }
}

public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
  InputStream is = new URL(url).openStream();
  try(BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
     String jsonText = readAll(rd);
     return new JSONObject(jsonText);
   } finally {
     is.close();
   }
}

public static List<String> retrieveTickID() throws JSONException, IOException {
	   String projName ="MAHOUT";
	   Integer j = 0; 
	   Integer i = 0;
	   Integer total = 1;
	   List<String> ids = new ArrayList<>();
    do {
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
	Integer i = 1;
	Integer k;
	String msg;
	String dt;
	try(FileWriter csvWriter = new FileWriter(DATA_COMMIT);) {
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
	  		return;
	  	}
	  	for( k=0 ; k<l ; k++ ) {
	  		msg = json.getJSONObject(k).getJSONObject("commit").getString("message");
	  		dt = json.getJSONObject(k).getJSONObject("commit").getJSONObject("committer").getString("date");
	  		csvWriter.append(dt);
	  		csvWriter.append(",");
	  		csvWriter.append(msg);
	  		csvWriter.append("\n");
	  		}
		}
		
	}
	
}

public static List<String> rawDataParser() throws IOException {
	String row;
	String toFind;
	Integer i;
	Integer match1;
	Integer match2;
	Integer match3;
	Double numberFound = 0.0;
	Boolean found = false;
	List<String> rawData = new ArrayList<>();
	Logger l = Logger.getLogger(TicketData.class.getName());
	
	
	List<String> ids = retrieveTickID();
	for(i = 0 ; i < ids.size(); i++) {
		toFind = ids.get(i);
		try(BufferedReader csvReader = new BufferedReader(new FileReader(DATA_COMMIT))){
			while ((row = csvReader.readLine()) != null) {
			    String[] dataCommit = row.split(",");
			    if(dataCommit.length >= 2) {
			    	match1 = StringUtils.countMatches(dataCommit[1],"[" + toFind + "]");
			    	match2 = StringUtils.countMatches(dataCommit[1],toFind + ":");
			    	match3 = StringUtils.countMatches(dataCommit[1],toFind + " ");
			    	if(match1 >= 1 || match2 >= 1 || match3 >=1) {
			    		String[] date = dataCommit[0].split("T");
			    		String[] ym = date[0].split("-");
			    		rawData.add(toFind + ":" + ym[0]+"-"+ym[1]);
			    		found = true;
			    	}
			    }
			}
		}
		if(Boolean.TRUE.equals(found)) {
			numberFound++;
			found = false;
		}
	}
	Double notFoundPerc = 100 - (numberFound/ids.size())*100;
	l.log(Level.INFO,"Data not found : {0} %",notFoundPerc);
	return rawData;
}

public static List<String> deleteDouble(List<String> parsedData1,List<String> rawData){
	int count = 0;
	List<Integer> toDelete = new ArrayList<>();
	
	
	for(String elem : parsedData1) {
		for(int k=0;k<rawData.size();k++) {
			String id = rawData.get(k).split(":")[0];
			if(elem.equals(id)) {
				count++;
				if(count > 1) {
					toDelete.add(k);
				}
			}
		}
		for(int del : toDelete) {
			rawData.remove(del);
		}
		count = 0;
		toDelete.clear();
	}
	return rawData;
}

public static List<Double> calculateStatistics(List<Integer> list){
    double sum = 0.0;
    double mean;
    double num = 0.0;
    double numi;
    double std;
    double upperLimit;
    double lowerLimit;
    List<Double> statistics = new ArrayList<>();

    for (Integer i : list) {
        sum+=i;
    }
    mean = sum/list.size();

    for (Integer i : list) {
        numi = Math.pow((double) i - mean, 2);
        num+=numi;
    }
    std = Math.sqrt(num/list.size());
    upperLimit = mean + 3*std;
    lowerLimit = mean - 3*std;
    statistics.add(mean);
    statistics.add(std);
    statistics.add(upperLimit);
    statistics.add(lowerLimit);
    return statistics;
}



public static void main(String[] args) throws IOException, JSONException, InterruptedException {

	List<String> rawData = new ArrayList<>();
	List<String> parsedData1 = new ArrayList<>();
	Logger l = Logger.getLogger(TicketData.class.getName());

	
	//Retrieve TickID and message from git commits
	File csvFile = new File(DATA_COMMIT);
	if(!csvFile.isFile()) {
		l.log(Level.INFO, "Caching file ...");
		csvWriter();
	}
	l.log(Level.INFO, "Starting processing data");
	
	//Searching only New Features commits
	rawData = rawDataParser();
	
	for(String e : rawData) {
		String[] s = e.split(":");
		parsedData1.add(s[0]);
	}
	
	//Delete eventually doubled tickets
	rawData = deleteDouble(parsedData1,rawData);
	
	List<String> date = new ArrayList<>();

	//Create a list of date of commits and then sort it
	for(String s1 : rawData) {
		String[] info = s1.split(":");
		date.add(info[1]);
	}
	Collections.sort(date);
	
	
	List<Integer> commitCount = new ArrayList<>();
	List<String> dateCount = new ArrayList<>();
	
	//Get starting date of the commits
	String[] d = date.get(0).split("-");
	Integer yearMin = Integer.parseInt(d[0]);
	//Getting ending date of the commits
	d = date.get(date.size()-1).split("-");
	Integer yearMax = Integer.parseInt(d[0]);
	
	//Count number of commits for every month
	for(int year = yearMin;year <= yearMax; year++) {
		for(int month = 1;month <= 12;month++) {
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
	
	//Reverse cycle to delete overflow
	while(commitCount.get(commitCount.size()-1) == 0){
		commitCount.remove(commitCount.size()-1);
		dateCount.remove(dateCount.size()-1);		
	}
	
	//Calculate statistics
	List<Double> stat = calculateStatistics(commitCount);
	
	try(FileWriter csvWriter = new FileWriter(FINAL_DATA);) {
		csvWriter.append("Date");
		csvWriter.append(",");
		csvWriter.append("Number of commits,");
		csvWriter.append("Mean,StdDev,Upper limit,Lower limit");
		csvWriter.append("\n");
		for(int j = 0;j<dateCount.size();j++) {
			csvWriter.append(dateCount.get(j));
			csvWriter.append(",");
			csvWriter.append(commitCount.get(j).toString()+",");
			csvWriter.append(stat.get(0).toString() + "," + stat.get(1).toString()+","+stat.get(2).toString() + "," + stat.get(3).toString() );
			csvWriter.append("\n");
		}
	}
	l.log(Level.INFO, "Done.See the results in " + FINAL_DATA + "file");

}

}
