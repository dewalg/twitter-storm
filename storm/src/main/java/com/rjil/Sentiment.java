package com.rjil;

import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

import com.aliasi.classify.LMClassifier;
import com.aliasi.classify.Classification;


import java.util.*;
import java.io.*;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;

import com.rjil.cassClient;

public class Sentiment extends BaseRichBolt {
    OutputCollector _collector;
    int j =0;
    BoundStatement log;
    cassClient client;
    LMClassifier loadedModelClassifier;

    @Override
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        _collector = collector;

        //INITIALIZE CASSANDRA CONNECTION
        // client = new cassClient();
        // try {
        //     client.connect("twitter");
        // } catch (Exception e) {
        //     System.out.println("FAILED TO CONNECT TO DB");
        // }

        //INITIALIZE LINGPIPE SENTIMENT ANALYZER
        try {
            FileInputStream fileIn = new FileInputStream("resources/polarity2.model");
            ObjectInputStream objIn = new ObjectInputStream(fileIn);
            loadedModelClassifier = (LMClassifier)objIn.readObject();
            objIn.close();

        } catch (Throwable t) {
            System.out.println("Thrown: " + t);
            t.printStackTrace(System.out);
        }

        // String LOG_TWEET = "UPDATE twitter.tweets SET sentiment=? where bucket = ? and id = ?;";
        // log = new BoundStatement(client.getSession().prepare(LOG_TWEET));

    }

    @Override
    public void execute(Tuple tuple) {
        String id = tuple.getStringByField("rowKey");
        String text = tuple.getStringByField("tweet");
        String user = tuple.getStringByField("user");
        int fol_count = tuple.getIntegerByField("follower_count");
        String time = tuple.getStringByField("time");
        String s = tuple.getStringByField("jsontweet");

        text = text.replaceAll("^a-zA-Z@:/]", " ").toLowerCase();
        //
        // try {
            Classification classification = loadedModelClassifier.classify( text );
            String sentiment = classification.bestCategory();
        // } catch (Exception e) {
        //     System.out.println("Could not run sentiment model.");
        //     _collector.fail(tuple);
        // }

        _collector.emit(tuple, new Values(id, text, user, fol_count, time, sentiment, s));
        _collector.ack(tuple);        // try {
        //     client.getSession().execute(log.bind(
        //         sentiment, bucket, id
        //     ));
        //
        //     System.out.println(id + ": " + sentiment);
        //     _collector.ack(tuple);
        //
        // } catch (Exception e) {
        //     System.out.println("Failed to update sentiment for tweet!");
        //     _collector.fail(tuple);
        // }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("rowKey", "tweet", "user", "follower_count", "time", "sentiment", "jsontweet"));
    }

}
