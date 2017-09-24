package com.rjil;

import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

// import java.util.ArrayList;
import java.text.*;
import java.util.*;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;

import com.rjil.cassClient;

public class TagMapper extends BaseRichBolt {
    OutputCollector _collector;
    int j =0;
    BoundStatement log;
    cassClient client;

    @Override
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        _collector = collector;
        client = new cassClient();
        try {
            client.connect("twitter");
        } catch (Exception e) {
            System.out.println("FAILED TO CONNECT TO DB");
        }

        // String LOG_TWEET = "UPDATE twitter.tag2tweet SET tweet_ids = tweet_ids + ? WHERE tag = ?;";
        String LOG_TWEET = "INSERT INTO twitter.tweets_by_hashtag (id, text, fol_count, user, time, sentiment, hashtag) VALUES (?, ?, ?, ?, ?, ?, ?);";
        log = new BoundStatement(client.getSession().prepare(LOG_TWEET));

    }

    @Override
    public void execute(Tuple tuple) {
        //("tweet", "user", "time", "sentiment", "tag", "id")
        String text = tuple.getStringByField("tweet");
        String user = tuple.getStringByField("user");
        String time = tuple.getStringByField("time");
        String sentiment = tuple.getStringByField("sentiment");
        int fol_count = tuple.getIntegerByField("follower_count");

        final String TWITTER="EEE MMM dd HH:mm:ss ZZZZZ yyyy";
        SimpleDateFormat sf = new SimpleDateFormat(TWITTER, Locale.ENGLISH);
        sf.setLenient(true);
        Date t = new Date(); //get current date+time
        try {
            t = sf.parse(time);
        } catch (ParseException e) {
            System.out.println("Unparseable using " + sf);
            //if fails - rather put in current day vs a corrupted one
        }

        String tweet_id = tuple.getStringByField("id");
        String tag = tuple.getStringByField("tag");

        client.getSession().execute(log.bind(
            tweet_id, text, fol_count, user, t, sentiment, tag
        ));

        _collector.ack(tuple);

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }

}
