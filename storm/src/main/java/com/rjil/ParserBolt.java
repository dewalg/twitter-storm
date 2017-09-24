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

import java.nio.charset.Charset;

public class ParserBolt extends BaseRichBolt {
    OutputCollector _collector;

    @Override
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        _collector = collector;
    }

    @Override
    public void execute(Tuple tuple) {

        // byte[] t = (byte[])tuple.getValue(0);
        // String s = new String(t);
        String s = tuple.getStringByField("tweet");

        try {
            JSONObject status = new JSONObject(s);
            String tweet = status.getString("text");
            String id = status.getString("id_str");
            JSONObject user = status.getJSONObject("user");
            int fol_count = user.getInt("followers_count");
            String sn = user.getString("screen_name");
            String created = status.getString("created_at");

            // System.out.println("@ " + sn + "(" + fol_count + "): " + tweet);
            _collector.emit(tuple, new Values(id, tweet, sn, fol_count, created,s));

        } catch (Exception e) {
            System.out.println("Canned a weird tweet with incomplete params" + e);
        }

        _collector.ack(tuple);

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("rowKey", "tweet", "user", "follower_count", "time","jsontweet"));
    }

}
