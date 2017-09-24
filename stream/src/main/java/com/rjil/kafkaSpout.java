package com.rjil;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class kafkaSpout {

    private static final String topic = "twitter-topic";

    public static void run() throws InterruptedException {
        String consumerKey = "592VMTqY2Is15Jh8XOUTHlGyk";
        String consumerSecret = "EVN89RY7kkmz4TWUDSrGMXhMX1nILOf5FHj7GMU0E8Oy5hGby8";
        String token = "44311944-d9grWpzhVuUSBoGkF2MPKwGDe5MqkTast9vxKNQ3n";
        String secret = "HvmgHCMKBOsaLwujzY4jF4XGtTaHhGLS4rRfqqNtkeJ3O";

        Properties properties = new Properties();
        properties.put("metadata.broker.list", "localhost:9092");
        properties.put("serializer.class", "kafka.serializer.StringEncoder");
        properties.put("client.id","camus");
        ProducerConfig producerConfig = new ProducerConfig(properties);
        kafka.javaapi.producer.Producer<String, String> producer = new kafka.javaapi.producer.Producer<String, String>(
                producerConfig);

        BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);
        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
        // add some track terms
        endpoint.trackTerms(Lists.newArrayList("apple"));
        endpoint.languages(Lists.newArrayList("en"));

        Authentication auth = new OAuth1(consumerKey, consumerSecret, token,
                secret);
        // Authentication auth = new BasicAuth(username, password);

        // Create a new BasicClient. By default gzip is enabled.
        Client client = new ClientBuilder()
                .hosts(Constants.STREAM_HOST)
                .endpoint(endpoint).authentication(auth)
                .processor(new StringDelimitedProcessor(queue))
                .build();

        // Establish a connection
        client.connect();

        // Do whatever needs to be done with messages
        for (int msgRead = 0; msgRead < 10000; msgRead++) {
            KeyedMessage<String, String> message = null;
            try {
                message = new KeyedMessage<String, String>(topic, queue.take());
                System.out.println(msgRead);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            producer.send(message);
        }
        producer.close();
        client.stop();

    }

    public static void main(String[] args) {
        try {
            kafkaSpout.run();
        } catch (InterruptedException e) {
            System.out.println(e);
        }
    }
}
