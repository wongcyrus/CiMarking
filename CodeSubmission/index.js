// dependencies
var async = require('async');
var AWS = require('aws-sdk');
var util = require('util');
// get reference to S3 client
var s3 = new AWS.S3();
AWS.config.region = 'ap-southeast-1';

exports.handler = function (event, context, callback) {
    // Read options from the event.
    console.log("Reading options from event:\n", util.inspect(event, {depth: 5}));
    var srcBucket = event.Records[0].s3.bucket.name;
    // Object key may have spaces or unicode non-ASCII characters.
    var srcKey =
        decodeURIComponent(event.Records[0].s3.object.key.replace(/\+/g, " "));
    var dstBucket = "cimarking-cloudlabhk.com";
    var dstKey = "index.html";

    // Download the image from S3, transform, and upload to a different S3 bucket.
    async.waterfall([
            function download(next) {
                var params = {
                    Bucket: srcBucket,
                    Delimiter: '/'
                }
                s3.listObjects(params, function (err, data) {
                    if (err)throw err;
                    console.log(data);
                    next(null, data.CommonPrefixes);
                });
            },
            function transform(data, next) {
                var s = "";

                function logArrayElements(element, index, array) {
                    s += element.Prefix + "\n";
                    console.log(element.Prefix);
                }

                data.forEach(logArrayElements);
                next(null, s);
            },
            function upload(data, next) {
                // Stream the transformed image to a different S3 bucket.
                s3.putObject({
                        Bucket: dstBucket,
                        Key: dstKey,
                        Body: data,
                        ContentType: "text/plain"
                    },
                    next);
            }
        ], function (err) {
            if (err) {
                console.error(
                    'Unable to List ' + srcBucket + '/' + srcKey +
                    ' and upload to ' + dstBucket + '/' + dstKey +
                    ' due to an error: ' + err
                );
            } else {
                console.log(
                    'Successfully List ' + srcBucket + '/' + srcKey +
                    ' and uploaded to ' + dstBucket + '/' + dstKey
                );
            }

            callback(null, "message");
        }
    );
};