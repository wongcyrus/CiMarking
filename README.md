#CI Marking
This is project that makes of AWS server-less technologies to build, test and mark student assignment.
It consists of 3 main modules:
1. CiMakringApi is claudia express web apps, which saves students code from a code monitoring tools.
2. CodeSubmission is node applications and lambda function that manages students submision status and trigger CI process.
3. CiMarking will triggered by S3 put event, and run gradle test with grading. [For the details](https://www.linkedin.com/pulse/run-gradle-within-aws-lambda-wong-chun-yin-cyrus-%E9%BB%83%E4%BF%8A%E5%BD%A5-?trk=pulse_spock-articles)