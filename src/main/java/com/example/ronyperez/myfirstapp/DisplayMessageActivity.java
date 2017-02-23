package com.example.ronyperez.myfirstapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Xml;
import android.view.ViewGroup;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class DisplayMessageActivity extends AppCompatActivity {

    private static final String ns = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);
        List<Entry> foundEntries = new ArrayList<Entry>();
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        InputStream in = this.getClass().getClassLoader().getResourceAsStream("/res/xml/jmdict.xml");
        try {
            foundEntries = parse(in, message);
        } catch (IOException ioexeption){
            System.out.println("IO exception: " + ioexeption.getMessage());
            return;
        } catch (XmlPullParserException xmlexeption){
            System.out.println("XML exception: " + xmlexeption.getMessage());
            return;
        }
        TextView textView = new TextView(this);
        textView.setTextSize(40);
        textView.setText(message);
        ViewGroup layout = (ViewGroup) findViewById(R.id.activity_display_message);
        layout.addView(textView);
        if(foundEntries.size() > 0){
            TextView translate = new TextView(this);
            translate.setTextSize(40);

            translate.setText(foundEntries.get(0).trans_en);
            layout.addView(translate);
        }
    }

    public List<Entry> parse(InputStream in, String query) throws XmlPullParserException, IOException {
        try{
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser, query);
        } finally {
            in.close();
        }
    }

    public static class Entry {
        public final String kanji;
        public final String kana;
        public final String trans_en;

        private Entry(String kanji, String kana, String trans_en) {
            this.kanji = kanji;
            this.kana = kana;
            this.trans_en = trans_en;
        }
    }

    private List<Entry> readFeed(XmlPullParser parser, String query) throws XmlPullParserException, IOException {
        List<Entry> entries = new ArrayList<Entry>();

        parser.require(XmlPullParser.START_TAG, ns, "JMdict");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("entry")) {
                Entry dictEntry = checkEntry(parser, query);
                if(dictEntry != null){
                    entries.add(dictEntry);
                }
            } else {
                skip(parser);
            }
        }
        return entries;
    }
    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    private Entry checkEntry(XmlPullParser parser, String query) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "entry");
        List<String> kanji = new ArrayList<String>();
        List<String> kana = new ArrayList<String>();
        List<String> trans_en = new ArrayList<String>();
        String kanjiStr = null;
        String kanaStr = null;
        String translateStr = null;
        Boolean entryFound = false;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "keb":
                    kanji.add(readKanji(parser));
                    break;
                case "reb":
                    kana.add(readKana(parser));
                    break;
                case "gloss":
                    if (parser.getAttributeCount() == 0){
                        trans_en.add(readTranslation(parser));
                    }
                    break;
            }
            for (int i=0;i< kanji.size(); i++){
                if (kanji.get(i).equals(query)){
                    entryFound = true;
                }
            }
            if(!entryFound){
                for (String k : kana){
                    if(k.equals(query)){
                        entryFound = true;
                    }
                }
            }
            if(entryFound){
                kanjiStr = TextUtils.join(", ", kanji);
                kanaStr = TextUtils.join(", ", kana);
                translateStr = TextUtils.join(", ", trans_en);
            }
//            if (name.equals("keb")) {
//                kanji = kanji + readTitle(parser);
//            } else if (name.equals("reb")) {
//                hiragana = hiragana + readSummary(parser);
//            } else if (name.equals("gloss")) {
//                trans_en = trans_en + readLink(parser);
//            } else {
//                skip(parser);
//            }
            if (translateStr == null){
                return null;
            }
//            if (kanjiStr == null && kanaStr != null){
//                kanjiStr = kanaStr;
//            }
        }
        return new Entry(kanjiStr, kanaStr, translateStr);
    }

    // Processes title tags in the feed.
    private String readKanji(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "keb");
        String kanji = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "keb");
        return kanji;
    }

    // Processes link tags in the feed.
    private String readTranslation(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "gloss");
        String translation = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "gloss");
        return translation;
    }

    // Processes summary tags in the feed.
    private String readKana(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "reb");
        String kana = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "reb");
        return kana;
    }

    // For the tags title and summary, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
