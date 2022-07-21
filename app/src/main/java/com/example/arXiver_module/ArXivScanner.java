package com.example.arXiver_module;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.example.arXiver_module.items.DateItem;
import com.example.arXiver_module.items.GeneralItem;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
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
    static final String BASE_URL_RSS = "http://export.arxiv.org/rss/";
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
                    ArXivPaper paper = new ArXivPaper(title, id.replace("/","-"), authors, categories, pdfString, publishedDate, updatedDate, abs, false);
                    result.add(paper);
                }
            }
        }catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return result;

    }

    public static ArrayList<ArXivPaper> extractPapersRSS(String category) {
        ArrayList<ArXivPaper> result = new ArrayList<>();

        try {
            URL url = new URL(BASE_URL_RSS + category);


            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(url.openStream());
            document.getDocumentElement().normalize();

            NodeList itemList = document.getElementsByTagName("item");

            for (int i = 0; i < itemList.getLength(); i++) {
                Node itemNode = itemList.item(i);
                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element item = (Element) itemNode;

                    // Title, id, and categories
                    String title = "";
                    String id = "";
                    String[] categories = null;
                    boolean isNew = true;

                    if (item.getElementsByTagName("title").getLength() > 0) {
                        Pattern pattern = Pattern.compile(ARXIV_RSS_TITLE_PATTERN);
                        Matcher matcher = pattern.matcher(
                                item.getElementsByTagName("title").item(0).getTextContent()
                        );
                        while (matcher.find()) {
                            title = StringEscapeUtils.unescapeXml(matcher.group(1));
                            id = matcher.group(2);
                            categories = parseCategories(matcher, category);

                            String groupTag = Objects.requireNonNull(matcher.group(4));
                            isNew = !groupTag.equals(" UPDATED");
                        }
                    }

                    // URL
                    String pdfURL = "";
                    if (item.getElementsByTagName("link").getLength() > 0) {
                        pdfURL = item.getElementsByTagName("link").item(0)
                                .getTextContent().replace("/abs/", "/pdf/") + ".pdf";
                    }

                    // Abstract
                    String abs = "";
                    if (item.getElementsByTagName("description").getLength() > 0) {
                        String unescapedAbs = StringEscapeUtils.unescapeXml(
                                item.getElementsByTagName("description").item(0).getTextContent()
                        );
                        abs = Jsoup.parse(unescapedAbs).text();
                    }

                    // Date
                    // 2021-08-03T17:41:33Z
                    Date dateObj = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
                    String todayDate = simpleDateFormat.format(dateObj);

                    // Authors
                    String rawAuthors = item.getElementsByTagName("dc:creator").item(0).getTextContent();
                    String[] rawAuthorsArray = rawAuthors.split(", ");
                    //String[] authors = new String[rawAuthorsArray.length];
                    ArrayList<String> authorsList = new ArrayList<>();
                    for (int j = 0; j < rawAuthorsArray.length; j++) {
                        Pattern patternAuthors = Pattern.compile("(.*?)<a href=\"(.*?)\">(.*?)</a>(.*?)");
                        Matcher matcherAuthors = patternAuthors.matcher(rawAuthorsArray[j]);
                        while (matcherAuthors.find()) {
                            authorsList.add(
                                    StringEscapeUtils.unescapeXml(matcherAuthors.group(3))
                            );
                        }
                    }
                    String[] authors = new String[authorsList.size()];
                    for (int j = 0; j < authorsList.size(); j++) {
                        authors[j] = authorsList.get(j);
                    }

                    // paper
                    if((!id.isEmpty()) && (!title.isEmpty()) && (categories != null) && (authors.length>0) && (!pdfURL.isEmpty())) {
                        ArXivPaper paper = new ArXivPaper(title, id.replace("/", "-"), authors, categories, pdfURL, todayDate, todayDate, abs, isNew);
                        result.add(paper);
                    }
                }
            }
        }catch (IOException e) {// No internet connection
            return null;
        }catch( ParserConfigurationException | SAXException e) {// Parser problem
            return null;
        }
        return result;
    }

    public static String[] parseCategories(Matcher matcher, String category){
        String raw = Objects.requireNonNull(matcher.group(3));
        String[] categoriesRaw = raw.split(", ");

        ArrayList<String> categoriesList = new ArrayList<>();
        for(String potentialCategory:categoriesRaw){
            categoriesList.add(potentialCategory);
        }
        // Determining if it is cross-list. Adding main category if it is
        if (!category.equals(categoriesList.get(0))) {
            categoriesList.add(category);
        }
        // From list to array
        String[] categories = new String[categoriesList.size()];
        for (int j=0; j< categoriesList.size();j++){
            categories[j] = categoriesList.get(j);
        }
        return categories;
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

        ArXivPaper paper  = new ArXivPaper(title, id, authors, categories, pdfURL, publishedDate, updatedDate, abs, true);
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
