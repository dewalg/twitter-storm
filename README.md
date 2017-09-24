# Twitter Dashboarding Project

## Apache Kafka

Tweets from twitter are ingested using the Twitter Streaming API into a Kafka Queue. These tweets are then polled from the Queue using Apache Storm, and with the help of Apache Zookeeper.

## Apache Storm

The tweets go through a pre-defined topology for Apache Storm (located in the storm directory). This topology extracts useful information such as hashtag mentions, important keywords, sentiment analysis, and geo-location data. This data is all extracted and put into a Cassadra keyspace for later consumption. The details of building the Cassandra Schema are provided in ```schema.txt```. 

## Front-end Dashboard

The front end dashboard is built using Node.js and a variety of modules such as D3.js for charting and data visualization. Node.js also allows us to continuously stream from the Cassandra Keystore to provide live data for the dashboard. Some of the visualizations include a chart for the volume of tweets with certain hashtags and/or keywords, a sentiment analysis for incoming tweets (positive or negative), and a hashtag word bubble chart to visualize the frequencies of hashtags used in the tweets.

Some screenshots for a better idea of these visualizations:
![Dashboard View 1](https://raw.githubusercontent.com/dewalg/twitter-storm/master/screen_shots/dashboard.png "Dashboard")
![Dashboard View 2](https://raw.githubusercontent.com/dewalg/twitter-storm/master/screen_shots/dashboard2.png "Dashboard 2")
![Hashtag Bubble Histogram](https://raw.githubusercontent.com/dewalg/twitter-storm/master/screen_shots/hashtags.png "Hashtag Bubble Chart")
![Geo-Location Visualization](https://raw.githubusercontent.com/dewalg/twitter-storm/master/screen_shots/map.png "map")

