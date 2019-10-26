package com.stackroute.searchnlpservice.service;

import com.fasterxml.jackson.databind.util.JSONPObject;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.JSONOutputter;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.apache.catalina.Pipeline;
import javax.json.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class QueryNlpService {

    ArrayList<String> conceptList = new ArrayList<>();
    ArrayList<String> diseaseList = new ArrayList<>();
    ArrayList<String> medicationList = new ArrayList<>();
    ArrayList<String> symptomList = new ArrayList<>();
    ArrayList<String> causeList = new ArrayList<>();
    public QueryNlpService(){
        conceptList.add("medication");
        conceptList.add("disease");
        conceptList.add("cause");
        conceptList.add("symptom");
        diseaseList.add("cancer");
        diseaseList.add("fever");
        diseaseList.add("chicken pox");
        medicationList.add("ibuprofen");
        medicationList.add("paracetamol");
        symptomList.add("cold");
        symptomList.add("headache");
        symptomList.add("pain");
        causeList.add("bacteria");
        causeList.add("virus");
    }

    private String userQuery = "What is the medication taken for diseases cancer which has cold symptoms and caused by bacteria";
    private String[] stopWords = new String[]{"what","and","like","taken","also","for","it", "with","who","which","required","used","do","is","?", "by","take","are", "give", "of", "in", "times","me","How","many", "the","if","a","has","getting"};
    private static Properties properties;
    /**
     * propertiesName is setting values for NLP. ssplit is used for splitting the
     * sentence,pos will describe words as parts of speech, sentiment will do
     * sentiment analysis according to Stanford DB
     */
    private static String propertiesName = "tokenize, ssplit, pos, lemma";
    private static StanfordCoreNLP stanfordCoreNLP;

    static {
        properties = new Properties();
        properties.setProperty("annotators", propertiesName);
    }

    public static StanfordCoreNLP getPipeline() {
        if (stanfordCoreNLP == null) {
            stanfordCoreNLP = new StanfordCoreNLP(properties);
        }
        return stanfordCoreNLP;
    }

    //removing Extra Spaces of the userQuery
    public String getTrimmedQuery() {
        String trimmedQuery = this.userQuery.trim();
        trimmedQuery = trimmedQuery.replaceAll("\\s+", " ");
        trimmedQuery = trimmedQuery.replaceAll("\\t", " ");

        return trimmedQuery;
    }

    public ArrayList<String> getListWithoutStopWords() {
        String trimmedQuery = getTrimmedQuery();
        String[] wordsSplitArray = trimmedQuery.split(" ");
        ArrayList<String> wordsSplitList = new ArrayList<String>();
        for (int i = 0; i < wordsSplitArray.length; i++) wordsSplitList.add(wordsSplitArray[i]);

        for (int i = 0; i < stopWords.length; i++) {
            for (int j = 0; j < wordsSplitList.size(); j++) {
                if (wordsSplitList.get(j).equalsIgnoreCase(stopWords[i].trim())) {
                    wordsSplitList.remove(wordsSplitList.get(j));
                }
            }
        }
        return wordsSplitList;
    }

    public List<String> getLemmatizedList() {

        List<String> lemmatizedWordsList = new ArrayList<String>();
        ArrayList<String> listWithoutStopWords = getListWithoutStopWords();
        String stringWithoutStopWords = "";

        for (int i = 0; i < listWithoutStopWords.size(); i++) {
            stringWithoutStopWords = stringWithoutStopWords + listWithoutStopWords.get(i) + " ";
        }
//        Sentence sentence = new Sentence(stringWithoutStopWords);
//        return sentence.lemmas();

        Annotation document = new Annotation(stringWithoutStopWords);
        System.out.println("**document" + document);
        StanfordCoreNLP stanfordCoreNLP = getPipeline();
        // run all Annotators on this text
        stanfordCoreNLP.annotate(document);
        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                // list of lemmas
                lemmatizedWordsList.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }
        return lemmatizedWordsList;

    }

    public void RedisMatcher(String lemmatizedString){
        for(int i=0; i < symptomList.size(); i++ ){
            Pattern pattern = Pattern.compile(symptomList.get(i));
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find()){
                lemmatizedString = lemmatizedString.replaceAll("symptom ", "");
                System.out.println("symptom "+symptomList.get(i));
            }
        }

        for(int i=0; i < diseaseList.size(); i++ ){
            Pattern pattern = Pattern.compile(diseaseList.get(i));
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find()){
                lemmatizedString = lemmatizedString.replaceAll("disease ", "");
                System.out.println("disease "+diseaseList.get(i));
            }
        }

        for(int i=0; i < causeList.size(); i++ ){
            lemmatizedString = lemmatizedString.replaceAll("bacterium", "bacteria");
            Pattern pattern = Pattern.compile(causeList.get(i));
            Matcher matcher = pattern.matcher(lemmatizedString);
            //System.out.println(lemmatizedString);
            if(matcher.find()){
                lemmatizedString = lemmatizedString.replaceAll("cause ", "");
                System.out.println("cause "+causeList.get(i));
            }
        }

        for (int i=0; i<conceptList.size();i++){
               Pattern pattern = Pattern.compile(conceptList.get(i));
               Matcher matcher = pattern.matcher(lemmatizedString);
               if(matcher.find()){
                   System.out.println("concept "+ conceptList.get(i));
               }
           }

    }


}

class Runner {
    public static void main(String[] args) {
        QueryNlpService QS = new QueryNlpService();
        String trimmedQuery = QS.getTrimmedQuery();
       // System.out.println("trimmed Query   " + trimmedQuery);
        ArrayList<String> listWithoutStopWords = QS.getListWithoutStopWords();
        for (int i = 0; i < listWithoutStopWords.size(); i++)
            System.out.println("without StopWords " + listWithoutStopWords.get(i));
        List<String> lemmatizedList = QS.getLemmatizedList();
        String stringWithoutStopWords = "";
        for (int i = 0; i < lemmatizedList.size(); i++) {
                stringWithoutStopWords = stringWithoutStopWords + lemmatizedList.get(i) + " ";

        }

        QS.RedisMatcher(stringWithoutStopWords);
    }
}


