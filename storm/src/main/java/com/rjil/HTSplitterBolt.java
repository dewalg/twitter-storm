package com.rjil;

import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import java.util.Map;

import java.util.ArrayList;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

public class HTSplitterBolt extends BaseRichBolt {
    OutputCollector _collector;

    @Override
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        _collector = collector;
    }

    @Override
    public void execute(Tuple tuple) {

            String s = tuple.getStringByField("jsontweet");

            String text = tuple.getStringByField("tweet");
            String user = tuple.getStringByField("user");
            String time = tuple.getStringByField("time");
            String sentiment = tuple.getStringByField("sentiment");
            int fol_count = tuple.getIntegerByField("follower_count");

        try {

            JSONObject status = new JSONObject(s);
            JSONObject entities = status.getJSONObject("entities");
            JSONArray htArray = entities.getJSONArray("hashtags");
            String t_id = status.getString("id_str");
            // ArrayList<String> hashtags = new ArrayList<String>();

            for (int i=0; i<htArray.length(); i++) {
                JSONObject htText = htArray.getJSONObject(i);
                String ht = htText.getString("text").toLowerCase();

                //convert hashtag to lower case?
                // hashtags.add( ht.toLowerCase() );
                _collector.emit(tuple, new Values(ht, 1L, "count"));
                _collector.emit("2TagMapper", tuple, new Values(text, user, fol_count, time, sentiment, ht, t_id));
            }

        } catch (Exception e) {
            System.out.println("Canned a weird tweet with incomplete params");
        }

        _collector.ack(tuple);

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("rowKeyField", "incrementAmount", "incrementColumn"));
        declarer.declareStream("2TagMapper", new Fields("tweet", "user", "follower_count", "time", "sentiment", "tag", "id"));
    }

}
