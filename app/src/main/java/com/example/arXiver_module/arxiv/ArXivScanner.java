package com.example.arXiver_module.arxiv;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.example.arXiver_module.items.DateItem;
import com.example.arXiver_module.items.GeneralItem;

import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@SuppressWarnings("ALL")
public class ArXivScanner {

    static final String ARXIV_RSS_TITLE_PATTERN = "(.*?) \\(arXiv:(.*?) \\[(.*?)](.*?)\\)";
    static final String BASE_URL_QUERY = "http://export.arxiv.org/api/query?search_query=";
    static final String BASE_URL_RSS = "https://rss.arxiv.org/rss/";//"http://export.arxiv.org/rss/";
    static final String BASE_GITHUB_FETCH_RSS = "https://raw.githubusercontent.com/ehijano/rss_fetch/master/rss_data/";
    static final int MAX_GITHUB_FETCH_ATTEMPTS = 5;
    static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    boolean knowsCategories = false;
    String[] allCategories;

    public ArrayList<ArXivPaper> getSavedPapers(Context context, String PREFS){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();
        ArrayList<ArXivPaper> result = new ArrayList<>();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            ArXivPaper paper = deCompress(entry.getValue().toString());
            result.add(paper);
        }
        return result;
    }

    public ArrayList<GeneralItem> getDateConsolidatedPapers(Context context, String PREFS, String DOWNLOAD_PREFS){
        SharedPreferences downloadPreferences = context.getSharedPreferences(DOWNLOAD_PREFS, Context.MODE_PRIVATE);
        HashMap<String, List<ArXivPaper>> groupedHashMap = groupDataIntoDates(context,PREFS);

        // ordered list of dates
        ArrayList<String> orderedDates = new ArrayList<>();
        for (String date : groupedHashMap.keySet()){
            orderedDates.add(date);
        }
        Comparator<String> dateComparator = new Comparator<String>() {
            DateFormat f = new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());
            @Override
            public int compare(String d1, String d2) {
                try {
                    return f.parse(d2).compareTo(f.parse(d1));
                } catch (ParseException e) {
                    throw new IllegalArgumentException(e);
                }
            }};
        Collections.sort(orderedDates,dateComparator);

        ArrayList<GeneralItem> consolidatedList = new ArrayList<>();
        for(String date:orderedDates){
            DateItem dateItem = new DateItem(date);
            consolidatedList.add(dateItem);

            for(ArXivPaper paper: groupedHashMap.get(date)){
                if(downloadPreferences.contains(paper.id)){// paper should be stored
                    long downloadID = downloadPreferences.getLong(paper.id, -1);
                    if (downloadID == -1){
                        paper.setDownloadID(null);
                    }else {// downloadPreferences thinks the paper is downloaded, but the user could have deleted it manually!
                        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        Uri pdfUri = downloadManager.getUriForDownloadedFile(downloadID);
                        if (pdfUri != null) {
                            paper.setDownloadID(new Long(downloadID));
                        }else{
                            paper.setDownloadID(null);
                        }
                    }
                }
                consolidatedList.add(paper);
            }
        }
        return consolidatedList;
    }

    private HashMap<String, List<ArXivPaper>> groupDataIntoDates(Context context, String PREFS) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();

        HashMap<String, List<ArXivPaper>> groupedHashMap = new HashMap<>();

        for (String key : allEntries.keySet()) {
            ArXivPaper paper = deCompress(allEntries.get(key).toString());

            String hashMapKey = paper.dateSaved;

            if (groupedHashMap.containsKey(hashMapKey)) {
                groupedHashMap.get(hashMapKey).add(paper);
            } else {
                List<ArXivPaper> list = new ArrayList<>();
                list.add(paper);
                groupedHashMap.put(hashMapKey, list);
            }
        }

        return groupedHashMap;
    }

    public boolean isPaperSaved(Context context, String PREFS, String id){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        return !sharedPreferences.contains(id);
    }

    public static ArrayList<ArXivPaper> extractPapersSearch(String query) {
        ArrayList<ArXivPaper> result = new ArrayList<>();
        try {

            URL url = new URL(BASE_URL_QUERY + query);

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(url.openStream());
            //document.getDocumentElement().normalize();

            NodeList itemList = document.getElementsByTagName("entry");

            for (int i = 0; i < itemList.getLength(); i++) {
                Node itemNode = itemList.item(i);
                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element item = (Element) itemNode;

                    String title = "";
                    if(item.getElementsByTagName("title").getLength()>0) {
                        title = StringEscapeUtils.unescapeXml(
                                item.getElementsByTagName("title").item(0).getTextContent()
                                        .replace("\n", "")
                        );
                    }

                    String id="";
                    String pdfString = "";
                    if (item.getElementsByTagName("id").getLength()>0) {
                        id = item.getElementsByTagName("id").item(0).getTextContent().split("http://arxiv.org/abs/")[1];
                        pdfString = item.getElementsByTagName("id").item(0).getTextContent().replace("/abs/", "/pdf/") + ".pdf";
                    }


                    String abs = "";
                    if (item.getElementsByTagName("summary").getLength()>0) {
                        abs = StringEscapeUtils.unescapeXml(
                                item.getElementsByTagName("summary").item(0).getTextContent()
                                        .replace("\n", " ")
                        );
                    }

                    String updatedDate = "";
                    if(item.getElementsByTagName("updated").getLength()>0) {
                        updatedDate = item.getElementsByTagName("updated").item(0).getTextContent();
                    }

                    String publishedDate = "";
                    if(item.getElementsByTagName("published").getLength()>0) {
                        publishedDate = item.getElementsByTagName("published").item(0).getTextContent();
                    }

                    NodeList categoryNodeList = item.getElementsByTagName("category");
                    String[] categories = new String[categoryNodeList.getLength()];
                    for (int j = 0; j < categoryNodeList.getLength(); j++) {
                        String potentialCategory = ((Element) categoryNodeList.item(j)).getAttribute("term");
                        categories[j] = potentialCategory;
                    }

                    NodeList authorsList = item.getElementsByTagName("author");
                    String[] authors = new String[authorsList.getLength()];
                    for (int j = 0; j < authorsList.getLength(); j++) {
                        Element author = (Element) authorsList.item(j);
                        authors[j] = "";
                        if(author.getElementsByTagName("name").getLength()>0) {
                            authors[j] = StringEscapeUtils.unescapeXml(
                                    author.getElementsByTagName("name").item(0).getTextContent()
                            );
                        }
                    }

                    // paper
                    ArXivPaper paper = new ArXivPaper(title, id.replace("/","-"), authors, categories, pdfString, publishedDate, updatedDate, abs, "new");
                    result.add(paper);
                }
            }
        }catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return result;

    }

    public static String removeFirstLine(String text) {
        String[] lines = text.split("\n", 2); // Split the text into two parts, the first line and the rest
        return (lines.length > 1) ? lines[1] : ""; // Return the rest, if it exists, otherwise return an empty string
    }

    public static String extractArxivId(String url) {
        // Regular expression to match the arXiv ID in various formats of the URL
        // This regex covers http, https, with or without www, and different arXiv base URLs
        String regex = "(?i)https?://(?:www\\.)?arxiv\\.org/(?:abs|pdf)/([\\w.]+)/?(?:\\.pdf)?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

    private static ArrayList<ArXivPaper> parseFeed(Document document) {

        ArrayList<ArXivPaper> result = new ArrayList<>();

        NodeList itemList = document.getElementsByTagName("item");

        for (int i = 0; i < itemList.getLength(); i++) {
            Node itemNode = itemList.item(i);


            if (itemNode.getNodeType() == Node.ELEMENT_NODE) {

                Element item = (Element) itemNode;

                // Title, id, and categories
                String title = "";
                String id = "";
                String[] categories = null;
                String announceType = "";

                if (item.getElementsByTagName("title").getLength() > 0) {
                    title = item.getElementsByTagName("title").item(0)
                            .getTextContent();
                }

                // URL
                String pdfURL = "";
                if (item.getElementsByTagName("link").getLength() > 0) {
                    String link = item.getElementsByTagName("link").item(0)
                            .getTextContent();
                    pdfURL = link.toString().replace("/abs/", "/pdf/") + ".pdf";
                    pdfURL = pdfURL.replace("https://", "http://");
                    id = ArXivScanner.extractArxivId(link);
                }

                // Abstract
                String abs = "";
                if (item.getElementsByTagName("description").getLength() > 0) {
                    String rawAbs = item.getElementsByTagName("description").item(0).getTextContent();
                    abs = ArXivScanner.removeFirstLine(rawAbs);
                }

                // Date
                // 2021-08-03T17:41:33Z
                Date dateObj = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
                String todayDate = simpleDateFormat.format(dateObj);

                // Authors
                String rawAuthors = item.getElementsByTagName("dc:creator").item(0).getTextContent();
                String[] authors = rawAuthors.split(", ");

                // Categories
                int category_count = item.getElementsByTagName("category").getLength();
                if (category_count > 0) {
                    categories = new String[category_count];
                    for (int j = 0; j < category_count; j++) {
                        Element categoryElement = (Element) item.getElementsByTagName("category").item(j);
                        categories[j] = categoryElement.getTextContent().toString();
                    }
                }

                // Announce type
                if (item.getElementsByTagName("arxiv:announce_type").getLength() > 0) {
                    announceType = item.getElementsByTagName("arxiv:announce_type").item(0).getTextContent().toString();
                }

                if (false) { // Why dont you log correctly? Why are you so lazy?
                    System.out.println("SUMMARY: ");
                    System.out.println(title);
                    System.out.println(id);
                    System.out.println(java.util.Arrays.toString(authors));
                    System.out.println(java.util.Arrays.toString(categories));
                    System.out.println(pdfURL);
                    System.out.println(todayDate);
                    System.out.println(abs);
                    System.out.println(announceType);
                }

                // paper
                if ((!id.isEmpty()) && (!title.isEmpty()) && (categories != null) && (authors.length > 0) && (!pdfURL.isEmpty())) {
                    ArXivPaper paper = new ArXivPaper(title, id.replace("/", "-"), authors, categories, pdfURL, todayDate, todayDate, abs, announceType);
                    result.add(paper);
                }
            }
        }

        return result;
    }

    private static  ArrayList<ArXivPaper> extractPapersFromURLString(String url_string){
        ArrayList<ArXivPaper> result = new ArrayList<>();

        try {
            URL url = new URL(url_string);

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(url.openStream());
            document.getDocumentElement().normalize();

            result = parseFeed(document);

        }catch (IOException e) {// No internet connection
            return null;
        }catch( ParserConfigurationException | SAXException e) {// Parser problem
            return null;
        }
        return result;

    }

    public static ArrayList<ArXivPaper> extractPapersRSS(String category) {
        ArrayList<ArXivPaper> results = new ArrayList<>();

        // Attempt arxiv feed
        results = extractPapersFromURLString(BASE_URL_RSS + category);

        // If today is empty, check the github fetch
        int attempts = 1;
        TimeZone estTimeZone = TimeZone.getTimeZone("America/New_York");
        Calendar calendar = Calendar.getInstance(estTimeZone);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        while (((results == null) || (results.isEmpty())) && (attempts < MAX_GITHUB_FETCH_ATTEMPTS) ){


            String dateString = dateFormat.format(calendar.getTime());
            System.out.println("FETHING GITHUB DATE:");
            System.out.println(dateString);

            String urlStr = BASE_GITHUB_FETCH_RSS + category + "/" + dateString + "_" + category + ".xml";

            results = extractPapersFromURLString(urlStr);

            // Decrement the date by one day and try again
            calendar.add(Calendar.DATE, -1); // Subtract one day
            attempts += 1;
        }

        return results;
    }

    public String simpleDate(String s){
        String result = "";
        if (!s.isEmpty()){
            SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
            format.setTimeZone(TimeZone.getTimeZone("UTC"));

            try {
                Date date = format.parse(s);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                result = simpleDateFormat.format(
                        Objects.requireNonNull(date)
                );

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static ArXivPaper deCompress(String compressedPaper){
        String abs = "";
        String title = "";
        String id = "";
        String[] categories;
        String pdfURL = "";
        String updatedDate = "";
        String publishedDate = "";
        String dateSaved = "";
        String[] authors;

        String[] rawPaper;

        rawPaper = compressedPaper.split("_Abstract:_ ");
        if(rawPaper.length>1) {
            abs = rawPaper[1].replace("\n", " ");
        }

        rawPaper = rawPaper[0].split("_Title:_ ");
        if(rawPaper.length>1) {
            title = rawPaper[1].replace("\n","");
        }

        rawPaper = rawPaper[0].split("_All Categories:_ ");
        if(rawPaper.length>1) {
            categories = rawPaper[1].split(", ");
        }else{
            categories = new String[0];
        }

        rawPaper = rawPaper[0].split("_Link:_ ");
        if(rawPaper.length>1) {
            pdfURL = rawPaper[1];
        }


        rawPaper = rawPaper[0].split("_Authors:_ ");
        if(rawPaper.length>1) {
            authors = rawPaper[1].split(", ");
        }else{
            authors = new String[0];
        }

        rawPaper = rawPaper[0].split("_Updated:_ ");
        if(rawPaper.length>1) {
            updatedDate = rawPaper[1];
        }

        rawPaper = rawPaper[0].split("_Published:_ ");
        if(rawPaper.length>1) {
            publishedDate = rawPaper[1];
        }

        rawPaper = rawPaper[0].split("_arxiv-id:_ ");
        if(rawPaper.length>1) {
            id = rawPaper[1];
        }

        rawPaper = rawPaper[0].split("_dateSaved:_ ");
        if(rawPaper.length>1) {
            dateSaved = rawPaper[1];
        }

        ArXivPaper paper  = new ArXivPaper(title, id, authors, categories, pdfURL, publishedDate, updatedDate, abs, "new");
        paper.setDateSaved(dateSaved);

        return paper;
    }

    public String compress(ArXivPaper paper){

        String result = "";

        result = result + "_dateSaved:_ " + paper.dateSaved;

        result = result + "_arxiv-id:_ " + paper.id;

        result = result + "_Published:_ " + paper.publishedDate;

        result = result + "_Updated:_ " + paper.updatedDate;

        result = result + "_Authors:_ " + join(", ",paper.authors);

        result = result + "_Link:_ " + paper.pdfURL.toString();

        result = result + "_All Categories:_ " + join(", ",paper.categories);

        result = result + "_Title:_ " + paper.title;

        result = result + "_Abstract:_ " + paper.abs;

        return result;
    }

    public String paperMessage(ArXivPaper paper){

        String result = "";

        result = result + "arxiv-id: " + paper.id + "\n";

        result = result + "Title: " + paper.title + "\n";

        result = result + "Authors: " + join(", ",paper.authors) + "\n";

        result = result + "Published: " + paper.publishedDate + "\n";

        result = result + "Updated: " + paper.updatedDate + "\n";

        result = result + "Link: " + paper.pdfURL.toString().replace(".pdf","").replace("/pdf/","/abs/") + "\n";

        result = result + "All Categories: " + join(", ",paper.categories) + "\n";

        result = result + "Abstract: " + paper.abs;

        return result;
    }

    public static String join(String delimiter, String[] arr){
        StringBuilder result = new StringBuilder();
        for(int i = 0; i < arr.length; i++){
            result.append(arr[i]);
            if (i < arr.length - 1){
                result.append(delimiter);
            }
        }
        return result.toString();
    }
}
