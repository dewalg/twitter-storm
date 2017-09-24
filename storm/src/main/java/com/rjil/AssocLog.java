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

public class AssocLog extends BaseRichBolt {
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

        String LOG_TWEET = "UPDATE wordassoc SET count = count + 1 WHERE word = ? and assoc = ? ;";
        log = new BoundStatement(client.getSession().prepare(LOG_TWEET));
    }

    @Override
    public void execute(Tuple tuple) {

        String word = tuple.getStringByField("word");
        String assoc = tuple.getStringByField("assoc");

        try {
            client.getSession().execute(log.bind(
                word, assoc
            ));

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
