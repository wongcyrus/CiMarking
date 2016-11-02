ECHO Compile
CALL sbt compile
ECHO Assembly
CALL sbt assembly
ECHO UPLOAD JAR
CALL aws s3 cp target\scala-2.11\CiMarking-assembly-1.0.jar s3://markingaccelerator-cloudlabhk-com
::CALL aws s3 cp E:\Temp\maven.zip s3://markingaccelerator-cloudlabhk-com
::CALL aws s3 cp path.sh s3://markingaccelerator-cloudlabhk-com

ECHO UPDATE LAMBDA
CALL aws lambda update-function-code --function-name CiMarking --s3-bucket markingaccelerator-cloudlabhk-com --s3-key CiMarking-assembly-1.0.jar --region=ap-southeast-1

ECHO Upload Test
::CALL aws s3 cp "E:\CiMarking\src\test\resources\pass\full\McMarker.java" s3://cimarking-cloudlabhk.com --region ap-southeast-1
::CALL aws s3 cp "E:\CiMarking\src\test\resources\failure\McMarker.java" s3://cimarking-cloudlabhk.com/123456789@stu.vtc.edu.hk/McMarker.java --region ap-southeast-1
CALL aws s3 cp "E:\CiMarking\src\test\resources\pass\partial\McMarker.java" s3://cimarking-cloudlabhk.com/123456789@stu.vtc.edu.hk/McMarker.java --region ap-southeast-1

:: aws s3 cp "E:\Working\MultipleChoicesTestMarking.zip" s3://markingaccelerator-cloudlabhk-com/MultipleChoicesTestMarking.zip --region ap-southeast-1