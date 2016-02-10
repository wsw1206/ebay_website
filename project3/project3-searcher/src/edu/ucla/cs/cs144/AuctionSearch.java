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
		return "";
	}
	
	public String echo(String message) {
		return message;
	}

}
