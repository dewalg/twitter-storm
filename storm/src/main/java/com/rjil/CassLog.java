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

public class CassLog extends BaseRichBolt {
    OutputCollector _collector;
    int j =0;
    BoundStatement log;
    BoundStatement log2;
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

        String LOG_TWEET = "INSERT INTO twitter.tweets (id, text, user, follower_count, time, bucket, sentiment) VALUES (?, ?, ?, ?, ?, ?, ?);";
        String LOG_TWEET_BY_USER = "INSERT INTO twitter.tweets_by_user (id, text, user, time, sentiment) VALUES (?, ?, ?, ?, ?);";
        log = new BoundStatement(client.getSession().prepare(LOG_TWEET));
        log2 = new BoundStatement(client.getSession().prepare(LOG_TWEET_BY_USER));
    }

    @Override
    public void execute(Tuple tuple) {

        String id = tuple.getStringByField("rowKey");
        String text = tuple.getStringByField("tweet");
        String user = tuple.getStringByField("user");
        int fol_count = tuple.getIntegerByField("follower_count");
        String time = tuple.getStringByField("time");
        String sentiment = tuple.getStringByField("sentiment");

        final String TWITTER="EEE MMM dd HH:mm:ss ZZZZZ yyyy";
        SimpleDateFormat sf = new SimpleDateFormat(TWITTER, Locale.ENGLISH);
        sf.setLenient(true);
        Date t = new Date(); //get current date+time
        SimpleDateFormat getBucket = new SimpleDateFormat("MMdd");
        String bucket = getBucket.format(t); //initialize minute to something
        try {
            t = sf.parse(time);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // time = dateFormat.format(t);
            bucket = getBucket.format(t);
        } catch (ParseException e) {
            System.out.println("Unparseable using " + sf);
        }

        try {
            client.getSession().execute(log.bind(
                id, text, user, fol_count, t, bucket, sentiment
            ));

            client.getSession().execute(log2.bind(
                id, text, user, t, sentiment
            ));
            System.out.println(Integer.toString(j) + '\t' + id);
//            System.out.println(j);
            j++;

        } catch (Exception e) {
            System.out.println("FAILED TO WRITE TO CASSANDRA");
            _collector.fail(tuple);
        }
        // _collector.emit(tuple, new Values(id, minute, text));
        _collector.ack(tuple);

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        // declarer.declare(new Fields("id", "bucket", "text"));
    }

}
