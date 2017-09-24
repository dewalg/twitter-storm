var current_date = new Date();
var day = current_date.getDate(); //from 1-31
var month = current_date.getMonth() + 1; //from 1-12

var bucket_string = ('0'+month.toString()).slice(-2) + ('0'+day.toString()).slice(-2);
bucket = 0
if (bucket != bucket_string) {
    bucket = bucket_string;
}

    var d = new Date();
    var time = d.getHours() + '-' + d.getMinutes() + '-' + d.getSeconds();
console.log(time)
