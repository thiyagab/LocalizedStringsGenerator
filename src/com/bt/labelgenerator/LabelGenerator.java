package com.bt.labelgenerator;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import com.google.gdata.client.spreadsheet.FeedURLFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetQuery;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

/**
 * An application to show the basic operations of the List feed.
 * 
 * 
 */
public class LabelGenerator {

	
	/** Help on setting up this demo. */
	private static final String[] WELCOME_MESSAGE = { "Label Generator tool convert the labels in google spreadsheet to android/j2me/ios,windows and infra compatible string files.\n"
			+ "IMPORTANT: The tool will work only if the sheet has NON-EMPTY rows. The iteration will end, if the tool encounters an empty row\n" };

	

	/** Our view of Google Spreadsheets as an authenticated Google user. */
	private SpreadsheetService service;

	/** The URL of the list feed for the selected spreadsheet. */
	private URL listFeedUrl;

	/** A factory that generates the appropriate feed URLs. */
	private FeedURLFactory factory;

	/** The output stream to use. */
	private PrintStream out;
	
	private static HashMap<String, String> stringsXMLMap = new HashMap<String, String>();
	
	static{
		stringsXMLMap.put("<", "&lt;");
		stringsXMLMap.put(">", "&gt;");
		stringsXMLMap.put("'", "\\'");
		stringsXMLMap.put("&", "&amp;");
	}


	/**
	 * Starts up the demo with the specified service.
	 * 
	 * @param service
	 *            the connection to the Google Spradsheets service.
	 * @param outputStream
	 *            a handle for stdout.
	 */
	public LabelGenerator(SpreadsheetService service, PrintStream outputStream) {
		this.out = outputStream;
		this.service = service;
		this.factory = FeedURLFactory.getDefault();
	}

	/**
	 * Log in to Google, under a Google Spreadsheets account.
	 * 
	 * @param username
	 *            name of user to authenticate (e.g. yourname@gmail.com)
	 * @param password
	 *            password to use for authentication
	 * @throws AuthenticationException
	 *             if the service is unable to validate the username and
	 *             password.
	 */
	public void login(String username, String password)
			throws AuthenticationException {
		// Authenticate
		service.setUserCredentials(username, password);
	}

	

	

	
	static StringBuffer duplicateKeys = new StringBuffer();
	ArrayList<String> keyList = new ArrayList<String>();
	static final String QUOTE = "\"";
	/**
	 * Prints the entire list entry, in a way that mildly resembles what the
	 * actual XML looks like.
	 * 
	 * In addition, all printed entries are cached here. This way, they can be
	 * updated or deleted, without having to retrieve the version identifier
	 * again from the server.
	 * 
	 * @param entry
	 *            the list entry to print
	 */
	static HashMap<String, StringBuffer> androidStringsMap = new HashMap<String, StringBuffer>();
	static HashMap<String, StringBuffer> iOSStringsMap = new HashMap<String, StringBuffer>();
	
	/**
	 * Process each row from the spreadsheet
	 * and populate the androidStringMap and iOSStringsMap
	 * @param entry
	 */

	public void processEachEntry(ListEntry entry) {

		String key = null;
		String value = null;
		HashMap<String, String> valueEntry = new HashMap<String, String>();
		for (String tag : entry.getCustomElements().getTags()) {

			if (tag.equals(KEY_COLUMN_NAME)) {
				key = entry.getCustomElements().getValue(tag);
			}
			if (key != null && tag.startsWith(VALUE_COLUMN_NAME)) {
				value = entry.getCustomElements().getValue(tag);
				String temp[]=tag.split("-");
				String langcode="";
				if(temp.length>1){
					langcode=temp[1];
				}
				valueEntry.put(langcode,value);
				
			}
		}
		if (key != null && key.trim().length() != 0) {
			key = key.trim();
			if(key.contains(" ")){
				System.out.println("The following Key contain white-space "+key+"\n\nPress any key to exit the tool..");
				getUserInput();
				System.exit(1);
			}
			if (keyList.contains(key)) {
				duplicateKeys.append(key).append("\n");
			} else
				keyList.add(key);
			
			if (key.length() != 0 && value != null) {
				value = changeSpecial(value);
				appendEachEntry(key,valueEntry);
			}
		}
	}
	
	/*
	 * Get the string value for every language and fill the hashmap with langcode and
	 * the string value
	 */
	private void appendEachEntry(String key, HashMap<String, String> valueEntry) {
				
		    Set<String> keySet = valueEntry.keySet();
		    for (String langCode : keySet) {
				String value = valueEntry.get(langCode);
				processAndroidStrings(langCode,key,value);
				processiOSStrings(langCode,key,value);
				
			}
	}
	
	
	private void processiOSStrings(String langCode, String key, String value) {
		StringBuffer localizableStrings=iOSStringsMap.get(langCode);
		
		if(localizableStrings==null){
			localizableStrings= new StringBuffer();
		}
		
		localizableStrings.append("\"").append(key)
		.append("\" = ").append("\"").append(value).append("\";\n");
		
		iOSStringsMap.put(langCode, localizableStrings);
	}

	
	
	private void processAndroidStrings(String langCode, String key, String value) {
		StringBuffer stringsXML=androidStringsMap.get(langCode);
		
		if(stringsXML==null){
			stringsXML= new StringBuffer();
			stringsXML.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
					"<resources>\n");
		}
		
		stringsXML.append("<string name=\"").append(key)
		.append("\">").append(value).append("</string>\n");
		
		androidStringsMap.put(langCode, stringsXML);
		
	}

	

	private String changeSpecial(String value) {
		for (String key : stringsXMLMap.keySet()) {
			value=value.replace(key, stringsXMLMap.get(key));
		}
		
		return value.trim();
	}

	public static void getUserInput(){
		 BufferedReader reader = new BufferedReader(
			        new InputStreamReader(System.in));
		 try {
			reader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Uses the user's creadentials to get a list of spreadsheets. Then asks the
	 * user which spreadsheet to load. If the selected spreadsheet has multiple
	 * worksheets then the user will also be prompted to select what sheet to
	 * use.
	 * 
	 * @param reader
	 *            to read input from the keyboard
	 * @throws ServiceException
	 *             when the request causes an error in the Google Spreadsheets
	 *             service.
	 * @throws IOException
	 *             when an error occurs in communication with the Google
	 *             Spreadsheets service.
	 * 
	 */
	public void loadSheet() throws IOException, ServiceException {
		// Get the spreadsheet to load
		SpreadsheetQuery sq = new SpreadsheetQuery(
				factory.getSpreadsheetsFeedUrl());
		sq.setTitleQuery(SPREADSHEET_NAME);
		SpreadsheetFeed feed = service.query(sq, SpreadsheetFeed.class);
		SpreadsheetEntry spreadsheet = feed.getEntries().get(0);
		// Get the worksheet to load
		if (spreadsheet.getWorksheets().size() >= 1) {
			listFeedUrl = spreadsheet.getWorksheets().get(0).getListFeedUrl();
		}
		System.out.println("\nSheet loaded: " + SPREADSHEET_NAME
				+ "  Processing the sheet..");
	}

	/**
	 * Process all rows in the spreadsheet.
	 * 
	 * @throws ServiceException
	 *             when the request causes an error in the Google Spreadsheets
	 *             service.
	 * @throws IOException
	 *             when an error occurs in communication with the Google
	 *             Spreadsheets service.
	 */
	public void processAllEntries() throws IOException, ServiceException {
		ListFeed feed = service.getFeed(listFeedUrl, ListFeed.class);
		
		System.out.println("\nIterating through "+feed.getTotalResults()+" labels...");
		
		for (ListEntry entry : feed.getEntries()) {
			
			processEachEntry(entry);
		}
		
		
		System.out.println("\nProcessed all data...");

	}


	/**
	 * Starts up the demo and prompts for commands.
	 * 
	 * @param username
	 *            name of user to authenticate (e.g. yourname@gmail.com)
	 * @param password
	 *            password to use for authentication
	 * @throws AuthenticationException
	 *             if the service is unable to validate the username and
	 *             password.
	 */
	public void run(String username, String password)
			throws AuthenticationException {
		for (String s : WELCOME_MESSAGE) {
			out.println(s);
		}

		out.println("\nLogin to google docs as " + USERNAME);
		login(username, password);
		try {
			out.println("\nLogin success. Loading spreadsheet "
					+ SPREADSHEET_NAME);
			loadSheet();
			processAllEntries();
		} catch (Exception e) {
			System.out.println("Error occured :"+e.getMessage());
			e.printStackTrace();
			getUserInput();
			System.exit(1);
		
		}

	}

	/**
	 * Runs the demo.
	 * 
	 * @param args
	 *            the command-line arguments
	 * @throws AuthenticationException
	 *             if the service is unable to validate the username and
	 *             password.
	 * @throws IOException
	 */
	public static void main(String[] args) throws AuthenticationException,
			IOException {
		loadConfig();
		LabelGenerator generator = new LabelGenerator(new SpreadsheetService(
				"Label Generator"), System.out);

		generator.run(USERNAME, PASSWORD);
		writeStringsXML();
		writeLocalizedStrings();
		System.out.println("\nLabel Generated successfully. Press any key to close this window");
		getUserInput();
		System.exit(0);
	}

	static final String CONFIG_FILE = "config.properties";
	static String USERNAME = "";
	static String PASSWORD = "";
	static String SPREADSHEET_NAME = "Label Config";
	static String STRINGS_XML_PATH = "strings.xml";
	static String STRINGS_PLIST_PATH = "Localizable.strings";
	static String KEY_COLUMN_NAME = "key";
	static String VALUE_COLUMN_NAME = "values";
	
	
	
	public static void writeStringsXML() throws IOException {
		Set<String> langCodeSet=androidStringsMap.keySet();
		for (String langCode : langCodeSet) {
			StringBuffer stringsXML = androidStringsMap.get(langCode);
			stringsXML.append("\n</resources>");
			String dirName = "values"+(langCode.isEmpty()?"":"-"+langCode);
			writeToFile(dirName+File.separator+STRINGS_XML_PATH, stringsXML.toString());
		}
		
		if (duplicateKeys.length() > 0) {
			System.out.println("\nWARNING  DUPLICATE KEYS FOUND :\n"
					+ duplicateKeys);
		}
	}
	
	public static void writeLocalizedStrings() throws IOException {
		Set<String> langCodeSet=iOSStringsMap.keySet();
		for (String langCode : langCodeSet) {
			StringBuffer localizedStrings = iOSStringsMap.get(langCode);
			if(langCode.isEmpty()) langCode="en";
			String dirName = langCode+".lproj";
			writeToFile(dirName+File.separator+STRINGS_PLIST_PATH, localizedStrings.toString());
		}
		
		if (duplicateKeys.length() > 0) {
			System.out.println("\nWARNING  DUPLICATE KEYS FOUND :\n"
					+ duplicateKeys);
		}
	}

	

	public static void writeToFile(String filePath, String contents)
			throws IOException {
		final File file = new File(filePath);
		final File parent_directory = file.getParentFile();
		 
		if (null != parent_directory)
		{ 
		    parent_directory.mkdirs();
		} 
		FileWriter pw = new FileWriter(file);
		pw.write(contents);
		pw.flush();
		pw.close();
	}

	/**
	 * Loads the config from the properties file.
	 * We prefer this way, since its easier than giving the values in 
	 * command line arguments
	 */
	public static void loadConfig() {
		Properties p = new Properties();
		try {
			p.load(new FileReader(CONFIG_FILE));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error in reading config file "+e.getMessage());
			getUserInput();
			System.exit(1);
		}

		USERNAME = p.getProperty("USERNAME");
		PASSWORD = p.getProperty("PASSWORD");
		SPREADSHEET_NAME = p.getProperty("SPREADSHEET_NAME");
		STRINGS_XML_PATH = p.getProperty("STRINGS_XML_PATH");
		KEY_COLUMN_NAME = p.getProperty("KEY_COLUMN_NAME");
		VALUE_COLUMN_NAME = p.getProperty("VALUE_COLUMN_NAME");
		STRINGS_PLIST_PATH = p.getProperty("STRINGS_PLIST_PATH");
		
		if(USERNAME==null || USERNAME.isEmpty() || PASSWORD==null || PASSWORD.isEmpty()){
			System.out.println("USERNAME or PASSWORD cannot be empty. Check the config.properties file");
			getUserInput();
			System.exit(1);
		}

	}

	
}
