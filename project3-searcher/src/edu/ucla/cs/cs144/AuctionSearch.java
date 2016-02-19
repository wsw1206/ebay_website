package edu.ucla.cs.cs144;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.HashSet;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.lucene.document.Document;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import edu.ucla.cs.cs144.DbManager;
import edu.ucla.cs.cs144.SearchRegion;
import edu.ucla.cs.cs144.SearchResult;

public class AuctionSearch implements IAuctionSearch {

	/* 
         * You will probably have to use JDBC to access MySQL data
         * Lucene IndexSearcher class to lookup Lucene index.
         * Read the corresponding tutorial to learn about how to use these.
         *
	 * You may create helper functions or classes to simplify writing these
	 * methods. Make sure that your helper functions are not public,
         * so that they are not exposed to outside of this class.
         *
         * Any new classes that you create should be part of
         * edu.ucla.cs.cs144 package and their source files should be
         * placed at src/edu/ucla/cs/cs144.
         *
         */
	private IndexSearcher searcher = null;
    private QueryParser parser = null;

    public AuctionSearch() throws IOException {
        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File("/var/lib/lucene/index1"))));
        parser = new QueryParser("content", new StandardAnalyzer());
    }


	public SearchResult[] basicSearch(String query, int numResultsToSkip, 
			int numResultsToReturn) {
		// TODO: Your code here!
		try{
		Query queryObj = parser.parse(query);
        TopDocs results = searcher.search(queryObj, numResultsToSkip + numResultsToReturn);

        int totalHits = results.totalHits;
        ScoreDoc[] hits = results.scoreDocs;

        if (totalHits < (numResultsToReturn + numResultsToSkip)) {
        	numResultsToReturn = Math.max(0, totalHits - numResultsToSkip);
        }

        SearchResult[] searchResults = new SearchResult[numResultsToReturn];

        for (int i=0; i<numResultsToReturn; i++) {
        	Document tmp = getDocument(hits[numResultsToSkip + i].doc);
        	searchResults[i] = new SearchResult(tmp.get("ItemID"), tmp.get("Name"));
        }
        System.out.println("Reply: " + query);
		return searchResults;
	}
	catch (ParseException ex) {
			System.out.println(ex);
		} catch (IOException ex) {
			System.out.println(ex);
		}
		return new SearchResult[0];
	}

	public Document getDocument(int docId) throws IOException {
        return searcher.doc(docId);
    }


	public SearchResult[] spatialSearch(String query, SearchRegion region,
			int numResultsToSkip, int numResultsToReturn) {
		// TODO: Your code here!
		SearchResult[] basicSearchResult = basicSearch(query, 0, Integer.MAX_VALUE);

		SearchResult[] spatialSearchResult = new SearchResult[numResultsToReturn];
		SearchResult[] finalSpatialSearchResult = null;

		Connection conn = null;
        // create a connection to the database to retrieve Items from MySQL
		try {
	    	conn = DbManager.getConnection(true);
		} catch (SQLException ex) {
	    	System.out.println(ex);
		}

		String lx = String.valueOf(region.getLx());
		String ly = String.valueOf(region.getLy());
		String rx = String.valueOf(region.getRx());
		String ry = String.valueOf(region.getRy());

		String leftUp = lx + " " + ry;
		String leftLow = lx + " " + ly;
		String rightUp = rx + " " + ry;
		String rightLow = rx + " " + ly;

		try{
			String poly = "Polygon((" + leftUp + "," +leftLow + "," + rightLow + "," + rightUp + "," + leftUp + "))";
			String queryString = "select ItemID from Location where MBRContains(GeomFromText('" + poly + "'), Coordinates)";
			Statement stmt = conn.createStatement();
			ResultSet locationSet = stmt.executeQuery(queryString);

			HashSet<String> locationResultHash = new HashSet<String>();
			while (locationSet.next()) 
				locationResultHash.add(locationSet.getString("ItemID"));

			int total = 0;
			for (int i = 0; i < basicSearchResult.length; i++) {
				String curItemId = basicSearchResult[i].getItemId();
				if (locationResultHash.contains(curItemId)) {
					total++;
					if ((total > numResultsToSkip) && (total <= (numResultsToSkip + numResultsToReturn))) {
						spatialSearchResult[total - numResultsToSkip - 1] = new SearchResult(curItemId, basicSearchResult[i].getName());
					}
					if (total > (numResultsToSkip + numResultsToReturn)) {
						break;
					}
				}
			}
			finalSpatialSearchResult = new SearchResult[total];
			for (int i = 0; i < total; i++)
            {
                finalSpatialSearchResult[i] = spatialSearchResult[i];
            }
			stmt.close();
			conn.close();
		}catch (SQLException sqlException) {
            System.out.println(sqlException);
        }
		return finalSpatialSearchResult;
	}

	public String getXMLDataForItemId(String itemId) {
		// TODO: Your code here!
		String resultXML = "";

		Connection conn = null;
        // create a connection to the database to retrieve Items from MySQL
		try {
	    	conn = DbManager.getConnection(true);
		} catch (SQLException ex) {
	    	System.out.println(ex);
		}

		

		try{
			String queryString = "select * from Item where ItemID = " + itemId + ";";
			Statement stmt = conn.createStatement();
			ResultSet itemSet = stmt.executeQuery(queryString);
			if (!itemSet.next()) {
				return "";
			}

			resultXML += "<Item ItemID=\"" + getParsedString(itemId) + "\">\n";
			resultXML += "\t<Name>" + getParsedString(itemSet.getString("Name")) + "</Name>\n";
			resultXML += getCategory(itemId);
			resultXML += "\t<Currently>$" + getCurrencyString(itemSet.getFloat("Currently")) + "</Currently>\n";
			if (itemSet.getFloat("BuyPrice") != 0) {
				resultXML += "\t<Buy_Price>$" + getCurrencyString(itemSet.getFloat("BuyPrice")) + "</Buy_Price>\n";
			}
			resultXML += "\t<First_Bid>$" + itemSet.getFloat("FirstBid") + "</First_Bid>\n";
			resultXML += "\t<Number_of_Bids>" + itemSet.getInt("NumOfBid") + "</Number_of_Bids>\n";

			resultXML += getBids(itemId);

			Statement stmt2 = conn.createStatement();
			queryString = "select * from User where UserID = \"" + itemSet.getString("UserID") + "\";";
			ResultSet userSet = stmt2.executeQuery(queryString);
			String latitude = "";
			String longitude = "";
			String location = "";
			String country = "";
			String rating = "";
			if (userSet.next()) {
				latitude = userSet.getString("Latitude");
				longitude = userSet.getString("Longitude");
				location = userSet.getString("Location");
				country = userSet.getString("Country");
				rating += userSet.getInt("Rating");
				if ((latitude != null) || (longitude != null)) {
					resultXML += "\t<Location>" + getParsedString(location) + "</Location>\n";
					
				}
				else {
					resultXML += "\t<Location Latitude=\"" + latitude + "\" Longitude=\"" + longitude + "\">" + getParsedString(location) + "</Location>\n";
				}
				resultXML += "\t<Country>" + getParsedString(country) + "</Country>\n";
				resultXML += "\t<Started>" + getTimeString(itemSet.getString("Started")) + "</Started>\n";
				resultXML += "\t<Ends>" + getTimeString(itemSet.getString("Ends")) + "</Ends>\n";
				resultXML += "\t<Seller Rating=\"" + rating + "\" UserID=\"" + getParsedString(itemSet.getString("UserID")) + "\" />\n";
			}

			
			resultXML += "\t<Description>" + getParsedString(itemSet.getString("Description")) + "</Description>\n";
			resultXML += "</Item>\n";
			stmt2.close();
			stmt.close();
			conn.close();
			return resultXML;

		}catch (Exception exception){
            System.out.println(exception);
        }
		return "";
	}
	private String getBids(String itemId) throws SQLException{
		Connection conn = null;
        // create a connection to the database to retrieve Items from MySQL
		try {
	    	conn = DbManager.getConnection(true);
		} catch (SQLException ex) {
	    	System.out.println(ex);
		}

		try{
			Statement stmt = conn.createStatement();
			String queryString = "select * from Bid where ItemID = " + itemId + ";";
			ResultSet bidSet = stmt.executeQuery(queryString);

			if (!bidSet.next()) {
				return "\t</Bids>\n";
			}

			String bidString = "";
			bidString += "\t<Bids>\n";
			while(bidSet.next()) {
				String curBid = "";
				curBid += "\t  <Bid>\n";
				curBid += "\t   <Bidder Rating=\"";

				String userID = "";
				userID = bidSet.getString("UserID");

				Statement stmt2 = conn.createStatement();
				queryString = "select * from User where UserID = \"" + userID + "\";";
				ResultSet bidInfo = stmt2.executeQuery(queryString);
				if (bidInfo.next()) {
					
					curBid += bidInfo.getInt("Rating") + "\" UserID=\"";
					curBid += getParsedString(userID) + "\">\n";
					curBid += "\t    <Location>" + getParsedString(bidInfo.getString("Location")) + "</Location>\n";
					curBid += "\t    <Country>" + getParsedString(bidInfo.getString("Country")) + "</Country>\n";
				}
				curBid += "\t   </Bidder>\n";
				
				curBid += "\t   <Time>" + getTimeString(bidSet.getString("Time")) + "</Time>\n";
				curBid += "\t   <Amount>$" + getCurrencyString(bidSet.getFloat("Amount")) + "</Amount>\n";
				stmt2.close();
				bidString += curBid;
				bidString += "\t  </Bid>\n";
			}
			bidString += "\t</Bids>\n";
			stmt.close();
			conn.close();
			return bidString;
		}catch (Exception exception){
            System.out.println(exception);
        }
        return "";
	}
	private String getTimeString(String timeStamp)
    {
        SimpleDateFormat inputTimeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat outputTimeStamp = new SimpleDateFormat("MMM-dd-yy HH:mm:ss");
        StringBuffer stringBuffer = new StringBuffer();

        try{
            Date parsedDate = inputTimeStamp.parse(timeStamp);
            return "" + outputTimeStamp.format(parsedDate);
        }
        catch (Exception exception){
            System.err.println("Timestamp parse error!");
            return "";
        }
    }
	private String getCurrencyString(float amount)
    {
        return String.format("%.2f", amount);
    }
	private String getCategory(String itemId) throws SQLException{
		Connection conn = null;
        // create a connection to the database to retrieve Items from MySQL
		try {
	    	conn = DbManager.getConnection(true);
		} catch (SQLException ex) {
	    	System.out.println(ex);
		}

		try{
			Statement stmt = conn.createStatement();
			String queryString = "select Category from ItemCategory where ItemID = " + itemId + ";";
			ResultSet categorySet = stmt.executeQuery(queryString);
			String categoryString = "";
			while(categorySet.next()) {
				categoryString += "\t<Category>" + getParsedString(categorySet.getString("Category")) + "</Category>\n";
			}
			stmt.close();
			conn.close();
			return categoryString;
		}
		catch (Exception exception){
            System.out.println(exception);
        }
        return "";
	}

	public String getParsedString(String input) {
		return input.replaceAll("&", "&amp;").replaceAll("\"", "&quot;").replaceAll("'", "&apos;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}
	public String echo(String message) {
		return message;
	}

}
