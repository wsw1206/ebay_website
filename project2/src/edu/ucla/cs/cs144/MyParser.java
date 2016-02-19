/* CS144
 *
 * Parser skeleton for processing item-???.xml files. Must be compiled in
 * JDK 1.5 or above.
 *
 * Instructions:
 *
 * This program processes all files passed on the command line (to parse
 * an entire diectory, type "java MyParser myFiles/*.xml" at the shell).
 *
 * At the point noted below, an individual XML file has been parsed into a
 * DOM Document node. You should fill in code to process the node. Java's
 * interface for the Document Object Model (DOM) is in package
 * org.w3c.dom. The documentation is available online at
 *
 * http://java.sun.com/j2se/1.5.0/docs/api/index.html
 *
 * A tutorial of Java's XML Parsing can be found at:
 *
 * http://java.sun.com/webservices/jaxp/
 *
 * Some auxiliary methods have been written for you. You may find them
 * useful.
 */

package edu.ucla.cs.cs144;

import java.io.*;
import java.text.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;


class MyParser {
    
    static final String columnSeparator = "|*|";
    static DocumentBuilder builder;
    
    private static BufferedWriter itemFileWriter;
    private static BufferedWriter userFileWriter;
    private static BufferedWriter categoryFileWriter;
    private static BufferedWriter bidFileWriter;
    
    private static int bidID = 0;
    
    static final String[] typeName = {
	"none",
	"Element",
	"Attr",
	"Text",
	"CDATA",
	"EntityRef",
	"Entity",
	"ProcInstr",
	"Comment",
	"Document",
	"DocType",
	"DocFragment",
	"Notation",
    };
    
    static class MyErrorHandler implements ErrorHandler {
        
        public void warning(SAXParseException exception)
        throws SAXException {
            fatalError(exception);
        }
        
        public void error(SAXParseException exception)
        throws SAXException {
            fatalError(exception);
        }
        
        public void fatalError(SAXParseException exception)
        throws SAXException {
            exception.printStackTrace();
            System.out.println("There should be no errors " +
                               "in the supplied XML files.");
            System.exit(3);
        }
        
    }
    
    /* Non-recursive (NR) version of Node.getElementsByTagName(...)
     */
    static Element[] getElementsByTagNameNR(Element e, String tagName) {
        Vector< Element > elements = new Vector< Element >();
        Node child = e.getFirstChild();
        while (child != null) {
            if (child instanceof Element && child.getNodeName().equals(tagName))
            {
                elements.add( (Element)child );
            }
            child = child.getNextSibling();
        }
        Element[] result = new Element[elements.size()];
        elements.copyInto(result);
        return result;
    }
    
    /* Returns the first subelement of e matching the given tagName, or
     * null if one does not exist. NR means Non-Recursive.
     */
    static Element getElementByTagNameNR(Element e, String tagName) {
        Node child = e.getFirstChild();
        while (child != null) {
            if (child instanceof Element && child.getNodeName().equals(tagName))
                return (Element) child;
            child = child.getNextSibling();
        }
        return null;
    }
    
    /* Returns the text associated with the given element (which must have
     * type #PCDATA) as child, or "" if it contains no text.
     */
    static String getElementText(Element e) {
        if (e.getChildNodes().getLength() == 1) {
            Text elementText = (Text) e.getFirstChild();
            return elementText.getNodeValue();
        }
        else
            return "";
    }
    
    /* Returns the text (#PCDATA) associated with the first subelement X
     * of e with the given tagName. If no such X exists or X contains no
     * text, "" is returned. NR means Non-Recursive.
     */
    static String getElementTextByTagNameNR(Element e, String tagName) {
        Element elem = getElementByTagNameNR(e, tagName);
        if (elem != null)
            return getElementText(elem);
        else
            return "";
    }
    
    /* Returns the amount (in XXXXX.xx format) denoted by a money-string
     * like $3,453.23. Returns the input if the input is an empty string.
     */
    static String strip(String money) {
        if (money.equals(""))
            return money;
        else {
            double am = 0.0;
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
            try { am = nf.parse(money).doubleValue(); }
            catch (ParseException e) {
                System.out.println("This method should work for all " +
                                   "money values you find in our data.");
                System.exit(20);
            }
            nf.setGroupingUsed(false);
            return nf.format(am).substring(1);
        }
    }
    
    /* Process one items-???.xml file.
     */
    static void processFile(File xmlFile) {
        Document doc = null;
        try {
            doc = builder.parse(xmlFile);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(3);
        }
        catch (SAXException e) {
            System.out.println("Parsing error on file " + xmlFile);
            System.out.println("  (not supposed to happen with supplied XML files)");
            e.printStackTrace();
            System.exit(3);
        }
        
        /* At this point 'doc' contains a DOM representation of an 'Items' XML
         * file. Use doc.getDocumentElement() to get the root Element. */
        System.out.println("Successfully parsed - " + xmlFile);
        
        /* Fill in code here (you will probably need to write auxiliary
            methods). */
        
        Element[] items = getElementsByTagNameNR(doc.getDocumentElement(), "Item");
        
        try {
            for (int i = 0; i < items.length; i++) {
                parseItem(items[i]);
                parseUser(items[i]);
                parseCategories(items[i]);
                parseBid(items[i]);
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        
        
        /**************************************************************/
        
    }
    public static void parseItem(Element item) throws IOException {
        String itemID = item.getAttribute("ItemID");
        
        String name = getElementTextByTagNameNR(item, "Name");
        String buyPrice = strip(getElementTextByTagNameNR(item, "Buy_Price"));
        String curr = strip(getElementTextByTagNameNR(item, "Currently"));
        String firstBid = strip(getElementTextByTagNameNR(item, "First_Bid"));
        String numOfBid = getElementTextByTagNameNR(item, "Number_of_Bids");
        
        String itemStarted = getElementTextByTagNameNR(item, "Started");
        String itemEnds = getElementTextByTagNameNR(item, "Ends");
        String started = "" + timestamp(itemStarted);
        String ends = "" + timestamp(itemEnds);
        
        Element seller = getElementByTagNameNR(item, "Seller");
        String sellerID = seller.getAttribute("UserID");
        
        String desc = getElementTextByTagNameNR(item, "Description");
        if(desc.length() > 4000)
            desc = desc.substring(0, 4000);
        
        load(itemFileWriter, itemID, sellerID, name, buyPrice, firstBid, curr, started, ends, desc, numOfBid);
        
    }
    
    public static void parseUser(Element item) throws IOException {
        Element seller = getElementByTagNameNR(item, "Seller");
        String userID = seller.getAttribute("UserID");
        String rating = seller.getAttribute("Rating");
        
        Element latlong = getElementByTagNameNR(item, "Location");
        String latitude = latlong.getAttribute("Latitude");
        String longitude = latlong.getAttribute("Longitude");
        
        //try different
        String location = "" + getElementText(getElementByTagNameNR(item, "Location"));
        String country = "" + getElementText(getElementByTagNameNR(item, "Country"));
        
        load(userFileWriter, userID, rating, location, country, latitude, longitude);
        
        Element[] bids = getElementsByTagNameNR(getElementByTagNameNR(item, "Bids"), "Bid");
        
        for (int i=0; i < bids.length; i++) {
            Element bidder = getElementByTagNameNR(bids[i], "Bidder");
            String bidderID = bidder.getAttribute("UserID");
            String bidderRating = bidder.getAttribute("Rating");
            String 		bidderLocation = "" + getElementTextByTagNameNR(bidder, "Location");
            String 		bidderCountry = "" + getElementTextByTagNameNR(bidder, "Country");
            
            
            String b_latitude = "";
            String b_longitude = "";
            
            
            load(userFileWriter, bidderID, bidderRating, bidderLocation, bidderCountry, b_latitude, b_longitude);
        }
    }
    
    public static void parseBid(Element item) throws IOException {
        String itemID = item.getAttribute("ItemID");
        Element[] bids = getElementsByTagNameNR(getElementByTagNameNR(item, "Bids"), "Bid");
        
        for (int i=0; i < bids.length; i++) {
            Element bidder = getElementByTagNameNR(bids[i], "Bidder");
            String userID = bidder.getAttribute("UserID");
            String bid_time = getElementTextByTagNameNR(bids[i], "Time");
            String time = "" + timestamp(bid_time);
            
            String amount = strip(getElementTextByTagNameNR(bids[i], "Amount"));
            
            load(bidFileWriter, "" + bidID++, userID, itemID, time, amount);
        }
    }
    
    public static void parseCategories(Element item) throws IOException {
        String itemID = item.getAttribute("ItemID");
        
        Element[] categories = getElementsByTagNameNR(item, "Category");
        
        for (int i = 0; i < categories.length; i++) {
            String category = getElementText(categories[i]);
            
            load(categoryFileWriter, itemID, category);
        }
    }
    
    private static String timestamp(String date)
    {
        SimpleDateFormat format_in = new SimpleDateFormat("MMM-dd-yy HH:mm:ss");
        
        SimpleDateFormat format_out = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        StringBuffer buffer = new StringBuffer();
        
        try
        {
            Date parsedDate = format_in.parse(date);
            
            return "" + format_out.format(parsedDate);
        }
        catch(ParseException pe) {
            System.err.println("Parse error");
            return "Parse error";
        }
    }
    
    private static String formatRow(String[] input) {
        String res = "";
        int i;
        for(i = 0; i < input.length - 1 ; i++) {
            res += input[i] + columnSeparator;
        }
        res += input[i];
        return res;
    }
    
    private static void load(BufferedWriter output, String... args) throws IOException {
        output.write(formatRow(args));
        output.newLine();
    }
    
    public static void main (String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java MyParser [file] [file] ...");
            System.exit(1);
        }
        
        /* Initialize parser. */
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringElementContentWhitespace(true);      
            builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new MyErrorHandler());
        }
        catch (FactoryConfigurationError e) {
            System.out.println("unable to get a document builder factory");
            System.exit(2);
        } 
        catch (ParserConfigurationException e) {
            System.out.println("parser was unable to be configured");
            System.exit(2);
        }
        
        /* Process all files listed on command line. */
        try
        {
            itemFileWriter = new BufferedWriter(new FileWriter("item.dat",true));
            userFileWriter = new BufferedWriter(new FileWriter("user.dat",true));
            categoryFileWriter = new BufferedWriter(new FileWriter("category.dat",true));
            bidFileWriter = new BufferedWriter(new FileWriter("bid.dat",true));
            
            /* Process all files listed on command line. */
            for (int i = 0; i < args.length; i++) {
                File currentFile = new File(args[i]);
                processFile(currentFile);
            }
            
            itemFileWriter.close();
            userFileWriter.close();
            categoryFileWriter.close();
            bidFileWriter.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
