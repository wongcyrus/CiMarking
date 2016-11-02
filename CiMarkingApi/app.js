'use strict';
const express = require('express');
const bodyParser = require("body-parser");
const app = express();
const S3Manager = require('./S3Manager');
const config = require('./configure');
const configure = config.configure;


app.use(bodyParser.urlencoded({extended: false}));
app.use(bodyParser.json());

app.get('/', (req, res) => {
    console.log(req.query.email);
    res.sendFile(`${__dirname}/index.html`);
});

let saveToS3 = function (email, filePathName, code, metadata, res, req) {
    console.log(email);
    let s3Manager = new S3Manager(configure.region, configure.sourceBucket);
    s3Manager.uploadString(`${email}/${filePathName}`, code, metadata).then(
        ()=>res.send(req.query.email),
        err => console.error(err)
    );
};
app.get('/code', (req, res) => {
    console.log(req.query);
    let metadata = JSON.parse(req.query.metadata);
    let email = metadata.email;
    let filePathName = metadata.filePathName;

    saveToS3(email, filePathName, req.query.code, metadata, res, req);
});

app.post('/code', (req, res) => {
    console.log(req.body);
    let metadata = JSON.parse(req.body.metadata);
    let email = metadata.email;
    let filePathName = metadata.filePathName;

    saveToS3(email, filePathName, req.body.code, metadata, res, req);
});

// app.listen(3000) // <-- comment this line out from your app

module.exports = app; // export your app so aws-serverless-express can use it