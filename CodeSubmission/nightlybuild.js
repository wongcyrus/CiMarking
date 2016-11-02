"use strict"
let AWS = require('aws-sdk');
let util = require('util');

// get reference to S3 client
let s3 = new AWS.S3();
AWS.config.region = 'ap-southeast-1';

exports.handler = (event, context, callback) => {
    console.log("Reading options from event:\n", util.inspect(event, {depth: 5}));
    const sourceBucket = "ite3101assignment";
    const dstBucket = "cimarking-cloudlabhk.com";

    let getWeeklyCode = ()=> new Promise((resolve, reject)=> {
        let allKeys = [];
        let listAllKeys = (marker, cb)=> {
            s3.listObjects({Bucket: sourceBucket, Marker: marker}, function (err, data) {
                data.Contents.forEach(c=>allKeys.push(c));
                if (data.IsTruncated)
                    listAllKeys(data.NextMarker, cb);
                else
                    cb();
            });
        }
        listAllKeys(null, ()=> {
            console.log(allKeys.length);
            let today = new Date();
            let weeklyCodes = allKeys.filter(v=> {
                    return parseInt((today - v.LastModified) / (24 * 3600 * 1000)) < 7
                }
            )
            console.log("Weekly updated students :" + weeklyCodes.length);
            if (weeklyCodes.length > 0)
                resolve(weeklyCodes.map(d=>
                    d.Key
                ));
            else
                reject("No Code updated");

        })
    });
    let copySource = key => new Promise((resolve, reject)=> {
        let params = {
            Bucket: dstBucket,
            Key: key,
            CopySource: sourceBucket + '/' + key
        };
        s3.copyObject(params, (copyErr, copyData)=> {
            if (copyErr) {
                reject(copyErr);
            }
            else {
                resolve(copyData);
            }
        });
    });

    let copyAllSources = sources => new Promise((resolve, reject)=> {
        Promise.all(sources.map(s=>copySource(s))).then(values => {
            resolve(values);
        }).catch(reason => {
            reject(reason)
        });
    });

    getWeeklyCode()
        .then(copyAllSources)
        .then(console.log)
        .catch(err=>console.error(err));
}


exports.handler();