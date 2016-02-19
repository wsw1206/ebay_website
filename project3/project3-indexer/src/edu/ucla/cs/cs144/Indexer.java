package edu.ucla.cs.cs144;

import java.io.IOException;
import java.io.StringReader;
import java.io.File;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Indexer {
    
    /** Creates a new instance of Indexer */
    public Indexer() {
    }

    private IndexWriter indexWriter = null;

    public IndexWriter getIndexWriter(boolean create) throws IOException {
        if (indexWriter == null) {
            Directory indexDir = FSDirectory.open(new File("/var/lib/lucene/index1"));
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_2, new StandardAnalyzer());

            // if boolean "create" is true, set OpenMode to CREATE (this overwrites previous index with a new one)
            // otherwise, set OpenMode to APPEND (this simply adds to the previous index)
            config.setOpenMode(create ? IndexWriterConfig.OpenMode.CREATE : IndexWriterConfig.OpenMode.APPEND);

            indexWriter = new IndexWriter(indexDir, config);
        }
        return indexWriter;
   }

   public void closeIndexWriter() throws IOException {
        if (indexWriter != null) {
            indexWriter.close();
        }
   }

   public void indexItem(int itemId, String name, String description, String categories) throws IOException {

        
        IndexWriter writer = getIndexWriter(false);
        Document doc = new Document();
        doc.add(new IntField("ItemID", itemId, Field.Store.YES));
        doc.add(new StringField("Name", name, Field.Store.YES));
        //doc.add(new StringField("city", hotel.getCity(), Field.Store.YES));
        String fullSearchableText = name + " " + description + " " + categories;
        doc.add(new TextField("content", fullSearchableText, Field.Store.NO));
        writer.addDocument(doc);
    }

    public void rebuildIndexes() {

        Connection conn = null;

        // create a connection to the database to retrieve Items from MySQL
	try {
	    conn = DbManager.getConnection(true);
	} catch (SQLException ex) {
	    System.out.println(ex);
	}


	/*
	 * Add your code here to retrieve Items using the connection
	 * and add corresponding entries to your Lucene inverted indexes.
         *
         * You will have to use JDBC API to retrieve MySQL data from Java.
         * Read our tutorial on JDBC if you do not know how to use JDBC.
         *
         * You will also have to use Lucene IndexWriter and Document
         * classes to create an index and populate it with Items data.
         * Read our tutorial on Lucene as well if you don't know how.
         *
         * As part of this development, you may want to add 
         * new methods and create additional Java classes. 
         * If you create new classes, make sure that
         * the classes become part of "edu.ucla.cs.cs144" package
         * and place your class source files at src/edu/ucla/cs/cs144/.
	 * 
	 */try{

          getIndexWriter(true);

          Statement stmt = conn.createStatement();
            ResultSet itemrs = stmt.executeQuery("SELECT ItemID, Name, Description FROM Item");
            PreparedStatement getItemCategories = conn.prepareStatement("SELECT Category FROM ItemCategory WHERE ItemID = ?");

            int itemId;
            String name, description, categories;

          while (itemrs.next()) {
            itemId = itemrs.getInt("ItemID");
            name = itemrs.getString("Name");
            description = itemrs.getString("Description");

            categories = "";
            getItemCategories.setInt(1, itemId);
            ResultSet categoryRS = getItemCategories.executeQuery();
            while (categoryRS.next()) {
                categories += categoryRS.getString("Category") + " ";
            }

            indexItem(itemId, name, description, categories);

            categoryRS.close();
          }
          itemrs.close();
          closeIndexWriter();
          stmt.close();
          conn.close();
      }
        // close the database connection
	 catch (SQLException ex) {
	    System.out.println(ex);
	}
    catch (IOException ex) {
            System.out.println(ex);
        }
    }

    public static void main(String args[]) {
        Indexer idx = new Indexer();
        idx.rebuildIndexes();
    }   
}
