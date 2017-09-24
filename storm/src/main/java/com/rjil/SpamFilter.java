package com.rjil;

import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

import java.util.*;
import java.text.*;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

import java.nio.charset.Charset;

public class SpamFilter extends BaseRichBolt {
    OutputCollector _collector;

    @Override
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        _collector = collector;
    }

    @Override
    public void execute(Tuple tuple) {

        byte[] byteTuple = (byte[])tuple.getValue(0);
        String s = new String(byteTuple);
        Boolean tosend = true;

        try {
            JSONObject status = new JSONObject(s);
            String tweet = status.getString("text");
            String id = status.getString("id_str");

            JSONObject user = status.getJSONObject("user");
            //number of users this tweeter is following:
            int friends_count = user.getInt("friends_count");
            //number of users he/she is followed by:
            int fol_count = user.getInt("followers_count");
            // tweets favorited in lifetime:
            int fav_count = user.getInt("favourites_count");
            int posts_count = user.getInt("statuses_count");
            int htcount = tweet.length() - tweet.replace("#", "").length();

            String tweet_created = status.getString("created_at");
            String user_created = user.getString("created_at");

            String userDescription = "";
            try {
            	userDescription = user.getString("description");
            } catch (Exception e) {
            	//doesn't matter
            }

            Boolean verified = user.getBoolean("verified");

            final String TWITTER="EEE MMM dd HH:mm:ss ZZZZZ yyyy";
            SimpleDateFormat sf = new SimpleDateFormat(TWITTER, Locale.ENGLISH);
            sf.setLenient(true);
            Date t1 = new Date();
            Date t2 = new Date();
            try {
                t1 = sf.parse(tweet_created);
                t2 = sf.parse(user_created);
            } catch (ParseException e) {
                System.out.println("Unparseable using " + sf);
                tosend = false;
            }

            long difference = t1.getTime() - t2.getTime();
            //account was created within a day
            if (difference < 86400000l) {
            	tosend = false;
            }
            if ((double)friends_count/fol_count < 0.01)
            	tosend = false;

            if (posts_count < 10)
            	tosend = false;

            if (userDescription.length() < 30)
            	tosend = false;

            if (htcount > 4)
            	tosend = false;

            if (tweet.length() < 20)
            	tosend = false;

            //make sure this is last!
            if (verified)
            	tosend=true;

//            if (!tosend)
//                System.out.println("SPAM: " + tweet);

        } catch (Exception e) {
            System.out.println("Canned a weird tweet with incomplete params");
            tosend = false;
        }

        if (tosend) {
        	_collector.emit(tuple, new Values(s));
//        	System.out.println("SENT: " + tweet);
        }

        _collector.ack(tuple);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("tweet"));
    }

}
