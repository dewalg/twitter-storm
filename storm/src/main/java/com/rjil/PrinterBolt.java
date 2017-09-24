package com.rjil;

import com.datastax.driver.core.Session;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import java.util.ArrayList;
import backtype.storm.tuple.Tuple;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

public class PrinterBolt extends BaseBasicBolt {


    @Override
    public void execute(Tuple tuple, BasicOutputCollector collector) {

        byte[] t = (byte[])tuple.getValue(0);
        String s = new String(t);

        JSONObject status = new JSONObject(s);
        String tweet = status.getString("text");
        System.out.println(tweet);

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        // declarer.declare(new Fields("hashtags"));
    }

}
