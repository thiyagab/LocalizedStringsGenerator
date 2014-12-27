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

	

	

	static StringBuffer stringsXML = new StringBuffer();
	static StringBuffer configjson = new StringBuffer();
	static StringBuffer stringsplist = new StringBuffer();
	static StringBuffer constants = new StringBuffer();
	static StringBuffer labelconfig = new StringBuffer();
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
	boolean addedNewLine = false;
	static HashMap<String, StringBuffer> valuesMap = new HashMap<String, StringBuffer>();

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
//				appendEachEntry(key,value);
				appendEachEntry(key,valueEntry);
				addedNewLine = false;
			}
		} else if (!addedNewLine) {
			addedNewLine = true;
//			stringsXML.append("\n");
//			configjson.append("\n");
//			stringsplist.append("\n");
//			constants.append("\n");
//			labelconfig.append("\n");
		}
	}
	
	
	private void appendEachEntry(String key, HashMap<String, String> valueEntry) {
				
		    Set<String> keySet = valueEntry.keySet();
		    for (String langCode : keySet) {
				String value = valueEntry.get(langCode);
				StringBuffer stringsXML=valuesMap.get(langCode);
				
				if(stringsXML==null){
					stringsXML= new StringBuffer();
					stringsXML.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
							"<resources>\n");
				}
				
				stringsXML.append("<string name=\"").append(key)
				.append("\">").append(value).append("</string>\n");
				
				valuesMap.put(langCode, stringsXML);
			}
	}

	private void appendEachEntry(String key, String value) {
		labelconfig.append(key).append(" = ").append(value).append("\n");
		
		stringsXML.append("<string name=\"").append(key)
		.append("\">").append(value).append("</string>\n");
		
		stringsplist.append(QUOTE).append(key).append(QUOTE)
		.append("=").append(QUOTE).append(value)
		.append(QUOTE).append(";\n");
		
		configjson.append("{\"key\":\"").append(key)
		.append("\",\"value\":\"").append(value)
		.append("\"},\n");

		constants.append("String ").append(key).append("=")
		.append(QUOTE).append(key).append(QUOTE).append(";\n");		
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
		prefixFiles();
		
		System.out.println("\nIterating through "+feed.getTotalResults()+" labels...");
		
		for (ListEntry entry : feed.getEntries()) {
			
			processEachEntry(entry);
		}
		suffixFiles();
		
		
		System.out.println("\nProcessed all data...");

	}

	

	private void suffixFiles() {
		constants.append("\n\n}");
		configjson.insert(0, "[\n").deleteCharAt(configjson.length()-1).append("\n]");
	}

	private void prefixFiles() {
		constants
		.append("package ").append(CONSTANTS_PACKAGE).append(";\n\npublic interface LabelConfig {\n\n");
stringsXML.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
"<resources>\n");
		
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

		// while (executeCommand(reader)) {
		// }
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
		LabelGenerator demo = new LabelGenerator(new SpreadsheetService(
				"Label Generator"), System.out);

		demo.run(USERNAME, PASSWORD);
//		writeToFile();
		writeStringsXML();
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
	static String CONSTANTS_PATH = "LabelConfig.java";
	static String CONFIG_JSON_PATH = "config.json";
	static String LABEL_PROPS_PATH = "labelconfig.properties";
	static String KEY_COLUMN_NAME = "key";
	static String VALUE_COLUMN_NAME = "values";
	static String DYNAMIC_COLUMN_NAME = "isdynamic";
	static String CONSTANTS_PACKAGE="com.bt.labels";
	
	
	public static void writeStringsXML() throws IOException {
		Set<String> langCodeSet=valuesMap.keySet();
		for (String langCode : langCodeSet) {
			StringBuffer stringsXML = valuesMap.get(langCode);
			stringsXML.append("\n</resources>");
			String dirName = "values"+(langCode.isEmpty()?"":"-"+langCode);
			writeToFile(dirName+File.separator+STRINGS_XML_PATH, stringsXML.toString());
		}
	}

	public static void writeToFile() throws IOException {
		stringsXML.append("\n\n</resources>");
		writeToFile(STRINGS_XML_PATH, stringsXML.toString());
		System.out.println("\nProcessed file " + STRINGS_XML_PATH);
		writeToFile(CONFIG_JSON_PATH, configjson.toString());
		System.out.println("\nProcessed file " + CONFIG_JSON_PATH);
		writeToFile(STRINGS_PLIST_PATH, stringsplist.toString());
		System.out.println("\nProcessed file " + STRINGS_PLIST_PATH);
		writeToFile(CONSTANTS_PATH, constants.toString());
		System.out.println("\nProcessed file " + CONSTANTS_PATH);
		writeToFile(LABEL_PROPS_PATH, labelconfig.toString());
		System.out.println("\nProcessed file " + LABEL_PROPS_PATH);

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
		STRINGS_PLIST_PATH = p.getProperty("STRINGS_PLIST_PATH");
		CONSTANTS_PATH = p.getProperty("CONSTANTS_PATH");
		CONFIG_JSON_PATH = p.getProperty("CONFIG_JSON_PATH");
		LABEL_PROPS_PATH = p.getProperty("LABEL_PROPS_PATH");
		KEY_COLUMN_NAME = p.getProperty("KEY_COLUMN_NAME");
		VALUE_COLUMN_NAME = p.getProperty("VALUE_COLUMN_NAME");
		DYNAMIC_COLUMN_NAME = p.getProperty("DYNAMIC_COLUMN_NAME");
		CONSTANTS_PACKAGE= p.getProperty("CONSTANTS_PACKAGE");
		if(USERNAME==null || USERNAME.isEmpty() || PASSWORD==null || PASSWORD.isEmpty()){
			System.out.println("USERNAME or PASSWORD cannot be empty. Check the config.properties file");
			getUserInput();
			System.exit(1);
		}

	}

	
}
