package com.rjil;

import java.util.*;

import storm.kafka.KafkaConfig;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.BrokerHosts;
import storm.kafka.ZkHosts;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.utils.Utils;
import backtype.storm.task.ShellBolt;
import backtype.storm.topology.*;
// import backtype.storm.topology.OutputFieldsDeclarer;


import com.hmsonline.storm.cassandra.StormCassandraConstants;
import com.hmsonline.storm.cassandra.bolt.AckStrategy;
import com.hmsonline.storm.cassandra.bolt.CassandraCounterBatchingBolt;
import com.hmsonline.storm.cassandra.bolt.CassandraBatchingBolt;
import com.hmsonline.storm.cassandra.bolt.mapper.DefaultTupleMapper;

import com.rjil.HTSplitterBolt;
import com.rjil.PrinterBolt;
import com.rjil.ParserBolt;
import com.rjil.CassLog;
import com.rjil.TagMapper;
import com.rjil.Sentiment;
import com.rjil.SpamFilter;
//import com.rjil.ScalaBolt;
import com.rjil.AssocLog;

public class tDashTopology {
	//template for creating a python bolt called "testBolt"
//    public static class testBolt extends ShellBolt implements IRichBolt {
//
//        public testBolt() {
//            super("python", "testBolt.py");
//        }
//
//        @Override
//        public void declareOutputFields(OutputFieldsDeclarer declarer) {
//        }
//
//        @Override
//        public Map<String, Object> getComponentConfiguration() {
//            return null;
//        }
//    }

    public static void main(String[] args) {

        TopologyBuilder builder = new TopologyBuilder();

        int partitions = 1;
        final String offsetPath = "/twitter-topic";
        final String consumerId = "v1";
        final String topic = "twitter-topic";
        BrokerHosts hosts = new ZkHosts("localhost:2181");

        CassandraCounterBatchingBolt logHT = new CassandraCounterBatchingBolt("twitter", "cassandra-config", "hashtags", "rowKeyField", "incrementAmount");
        logHT.setAckStrategy(AckStrategy.ACK_ON_WRITE);

        SpoutConfig spoutConfig = new SpoutConfig(hosts, topic, offsetPath, consumerId);
        spoutConfig.bufferSizeBytes = 1024*1024*4;
        spoutConfig.fetchSizeBytes = 1024*1024*4;
        spoutConfig.forceFromStart = false;
        spoutConfig.startOffsetTime = kafka.api.OffsetRequest.LatestTime();

        KafkaSpout kspout = new KafkaSpout(spoutConfig);

        builder.setSpout("twitter_stream", kspout);
        builder.setBolt("twitter", new SpamFilter())
                .shuffleGrouping("twitter_stream");
        builder.setBolt("parse", new ParserBolt())
                .shuffleGrouping("twitter");

        builder.setBolt("wordassoc", new ScalaBolt())
        		.shuffleGrouping("parse");
        builder.setBolt("assoclog", new AssocLog())
				.shuffleGrouping("wordassoc");
        
        builder.setBolt("sentiment", new Sentiment())
                .shuffleGrouping("parse");
        builder.setBolt("dataPersist", new CassLog())
                .shuffleGrouping("sentiment");

        builder.setBolt("splitter", new HTSplitterBolt())
                .shuffleGrouping("sentiment");
        builder.setBolt("log", logHT)
                .shuffleGrouping("splitter");
        builder.setBolt("tag2tweetMap", new TagMapper())
                .shuffleGrouping("splitter", "2TagMapper");

        Config conf = new Config();

        String configKey = "cassandra-config";
        HashMap<String, Object> clientConfig = new HashMap<String, Object>();
        clientConfig.put(StormCassandraConstants.CASSANDRA_HOST, "localhost:9160");
        clientConfig.put(StormCassandraConstants.CASSANDRA_KEYSPACE, Arrays.asList(new String [] {"twitter"}));
        conf.put(configKey, clientConfig);

        LocalCluster cluster = new LocalCluster();

        cluster.submitTopology("test", conf, builder.createTopology());
        //
        // Utils.sleep(20000);
        // cluster.shutdown();
    }
}
