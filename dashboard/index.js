//http://www.gianlucaguarini.com/blog/push-notification-server-streaming-on-a-mysql-database/
var express             = require('express');
var app                 = express();
var server              = require('http').createServer(app);
var io                  = require('socket.io')(server),
    fs                  = require('fs'),
    connectionsArray    = [],
    cassandra           = require('cassandra-driver'),
    connection          = new cassandra.Client({contactPoints: ['localhost']}),
    http                = require('http'),
    POLLING_INTERVAL = 1000,
    pollingTimer,
    pollingTimer2,
    pollingTimer3;

var watchlist = [];
var ids_sent = [];
var m = 0;
var neg=0,pos=0;
var bucket = 0;
var BigNumber = require('bignumber.js');
var id = new BigNumber(0);

var main = io.of('/main');
var alltweets = io.of('/alltweets');
var map = io.of('/map');
var cloud = io.of('/cloud');
var ht = io.of('/tag')
var alltweet_connections = [];
// If there is an error connecting to the database
connection.connect(function(err) {
    if (err) {
        console.log('[ERROR] db error: '+ err);
    }
    console.log( "[DB CONN] connected to db" );
});

// create a new nodejs server ( localhost:8080 )
server.listen(8080);
app.use(express.static( __dirname + '/public'));
app.use('/bower_components',  express.static(__dirname + '/bower_components'));
// on server ready we can load our client.html page
app.get('/', function (req, res) {
  res.sendFile(__dirname + '/index.html');
});

app.get('/map', function (req, res) {
  res.sendFile(__dirname + '/map.html');
});

app.get('/client', function (req, res) {
  res.sendFile(__dirname + '/client.html');
});

app.get('/cloud', function (req, res) {
  res.sendFile(__dirname + '/wordcloud.html');
});

app.get('/alltweets', function (req, res) {
  res.sendFile(__dirname + '/alltweets.html');
});

app.get('/tag', function (req, res) {
  res.sendFile(__dirname + '/experimental.html');
});
//
// app.get('/test2', function (req, res) {
//   res.sendFile(__dirname + '/test.html');
// });

app.disable('etag');

var pollingLoop = function () {
    // Make the database query
    var hashtags = [];
    connection.stream('SELECT tag,count FROM twitter.hashtags')
        .on('readable', function(){
            var row;
            while (row = this.read()) {
                hashtags.push({"tag":row.tag, "count":row.count});
            }
        })
        .on('end', function(){
            if (connectionsArray.length) {
                pollingTimer = setTimeout( pollingLoop, POLLING_INTERVAL );
                hashtags.sort(function(a, b){return b['count']-a['count']});
                updateSockets(hashtags.slice(0,100), 'hashtag', main);
            }
        })
        .on('error', function(err) {
            updateSockets(err, 'hashtag', main);
        });
};

var get_all_tweets = function(){

    var query = 'select * from twitter.tweets where bucket=?';
    var tosend = [];

    var current_date = new Date();

    var day = current_date.getDate(); //from 1-31
    var month = current_date.getMonth() + 1; //from 1-12
    var bucket_string = ('0'+month.toString()).slice(-2) + ('0'+day.toString()).slice(-2);

    var x = connection.stream(query, [bucket_string], {prepared:true, fetchSize : 25000 });
        x.on('readable', function(){
            while(row = this.read()) {
                tosend.push({'user':row.user, 'text':row.text, 'sentiment':row.sentiment, 'followers':row.follower_count, 'time':row.time.toString()});
            }
        })
        .on('end', function(){
            updateSockets(tosend, 'alltweets', alltweets);
        })
        .on('error', function(err) {
            console.log("[ERROR] " + err);
            updateSockets(err, 'alltweets', alltweets);
        });
};

var senti = function () {
    var query = 'select * from twitter.tweets where bucket=? and id>?';
    var current_date = new Date();
    var text = [];

    var day = current_date.getDate(); //from 1-31
    var month = current_date.getMonth() + 1; //from 1-12
    var bucket_string = ('0'+month.toString()).slice(-2) + ('0'+day.toString()).slice(-2);

    var id_string = id.toString();

    connection.stream(query, [bucket_string, id_string], {prepared:true})
        .on('readable', function(){
            while(row = this.read()) {
                id = BigNumber.max(id, new BigNumber(row.id));

                if (row.sentiment == "0") {
                    neg++;
                } else if (row.sentiment == "1") {
                    pos++;
                }

                ///////////////////////
                //SEE IF ANY NEW TWEET CONTAINS A MARKED HASHTAG

                for (var i=0; i<watchlist.length; i++) {

                    if (row.text.toLowerCase().search("#" + watchlist[i]) != -1) {
                        console.log(watchlist);
                        text.push({"text":row.text, "user":row.user, "sentiment":row.sentiment});
                        break;
                    }

                }

            }
        })
        .on('end', function(){
            if (connectionsArray.length) {
                pollingTimer3 = setTimeout( senti, POLLING_INTERVAL );
                var counts = [{'positive':pos, 'negative':neg}];
                updateSockets(counts, 'sentiment', main);
                updateSockets(text, 'tweets', main);
            }
            pos = 0;
            neg = 0;


        })
        .on('error', function(err) {
            console.log("[ERROR] " + err);
            updateSockets(err, 'tweets', main);
        });
};

alltweets.on('connection', function(socket) {
    alltweet_connections.push( socket );
    console.log('[INFO] Total number of connections: ' + (connectionsArray.length+alltweet_connections.length));
    // start the polling loop only if at least there is one user connected
    if (alltweet_connections.length == 1) {
        get_all_tweets();
        // pollingLoop2();
    }

    socket.on('disconnect', function () {

        console.log('[DISCONN] alltweets socket disconnected');
        var socketIndex = alltweet_connections.indexOf( socket );
        if (socketIndex >= 0) {
            alltweet_connections.splice( socketIndex, 1 );
        } else {
            console.log("uh oh, something went wrong!");
        }
    });


    socket.on('update_required', function() {
        get_all_tweets();
    });

    socket.on('tweets_by_user', function(user) {
        var user_tweets = "";
        query = "SELECT * FROM twitter.tweets_by_user WHERE user = ?";
        connection.stream(query, [user], {prepared:true})
            .on('readable', function(){
                var row;
                while (row = this.read()) {
                    var color = "red";
                    if (row.sentiment == 1)
                        color = "green";

                    user_tweets += "<li style=\"border-left: 3px solid " + color + "; \">" + row.text + "</li><br>";
                }
            })
            .on('end', function(){
                socket.emit('tweets_by_user', user_tweets);
                user_tweets = "";
            })
            .on('error', function(err) {
                console.log('could not run query');
            });
    });

    console.log( '[CONN] A new socket is connected to alltweets!' );
});

map.on('connection', function(socket) {
    console.log('logged on to map');

    function sendTestData() {
        var noise = Math.random()*2;
        var noisey = Math.random()*2;
        var testset = [{'lat':28.6139+noise+noisey, 'long':77.2090+noisey}, {'lat':18.9750+noise, 'long':72.8258+noisey}, {'lat':28.4700+noisey, 'long':77.0300+noise}, {'lat':16.5083+noise+noisey, 'long':80.6417+noisey}];
        map.emit('map', testset);
        t = setTimeout(function() { sendTestData(); }, 500);
    }
    socket.on('disconnect', function() {
        console.log('left map');
    });

    sendTestData();
});

cloud.on('connection', function(socket) {

    socket.on('disconnect', function () {
        console.log("someone went off of wordassoc");
    });
    socket.on('update_required', function(word) {

        var query = 'select * from twitter.wordassoc where word=?';
        var tosend = [];

        var x = connection.stream(query, [word], {prepared:true});
            x.on('readable', function(){
                while(row = this.read()) {
                    //don't consume more than 10,000 data elements!
                    if (tosend.length > 50000) {
                        x.read();
                        continue;
                    }
                    tosend.push({'text':row.assoc, 'size':row.count});
                }
            })
            .on('end', function(){
                mx = 0;
                for(var i=0; i<tosend.length; i++) {
                    mx = Math.max(mx, tosend[i].size);
                }

                for(var i=0; i<tosend.length; i++) {
                    var x = (tosend[i].size)/mx * 100;
                    if (x < 0.1) {
                        tosend.splice(i, 1);
                        i--;
                    }
                }

                socket.emit('wordmap', tosend);
            })
            .on('error', function(err) {
                console.log("[ERROR] " + err);
                socket.emit('error', "error");
            });

    });

    console.log( '[CONN] A new socket is connected to word assoc!' );
});

ht.on('connection', function(socket) {
    //we only care about starting the function get_ht and stopping it
    //we will start it if there's only 1 connection, and stop it
    //if there's 0 left.

    socket.on('disconnect', function () {
        console.log("[DISCONN] someone went off of ht analysis page");
    });

    socket.on('update', function() {
        var query = 'select * from twitter.tweets_by_hashtag';
        var tosend = [];

        var x = connection.stream(query);
            x.on('readable', function(){
                while(row = this.read()) {
                    //don't consume more than 10,000 data elements!
                    if (tosend.length > 50000) {
                        x.read();
                        continue;
                    }
                    tosend.push({'text':row.text, 'fol_count':row.fol_count, 'date':row.time, 'user':row.user, 'sentiment':row.sentiment, 'hashtag':row.hashtag});
                }
            })
            .on('end', function(){
                socket.emit('tags', tosend);
            })
            .on('error', function(err) {
                console.log("[ERROR] " + err);
                socket.emit('error', "error");
            });
    });

    console.log( '[CONN] A new socket is connected to the ht analysis page!' );
});

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%//

main.on( 'connection', function ( socket ) {

    console.log('[INFO] Total number of connections: ' + (connectionsArray.length+1+alltweet_connections));
    // start the polling loop only if at least there is one user connected
    if (!connectionsArray.length) {
        senti();
        pollingLoop();
        // pollingLoop2();
    }

    socket.on("watchlist", function(data) {
        watchlist = data;
    });

    socket.on('disconnect', function () {
        var socketIndex = connectionsArray.indexOf( socket );
        console.log('[DISCONN]socket = ' + socketIndex + ' disconnected');
        if (socketIndex >= 0) {
            connectionsArray.splice( socketIndex, 1 );
        }
    });

    console.log( '[CONN] A new socket is connected to main!' );
    connectionsArray.push( socket );

});

var updateSockets = function ( data, id, namespace ) {

    // console.log("[TRANSMITTING] " + data.length + " object under id: " + id);
    //
    // for (i = 0; i < connectionsArray.length; ++i) {
    //     tmpSocket = connectionsArray[i];
    //     tmpSocket.emit( id , data );
    // }
    namespace.emit(id, data);
};
