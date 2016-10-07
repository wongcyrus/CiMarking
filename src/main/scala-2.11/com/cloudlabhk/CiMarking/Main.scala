package com.cloudlabhk.CiMarking


import scala.io.Source
import scala.sys.process._

class Main {

  import java.io._
  import java.net.URLDecoder

  import com.amazonaws.services.lambda.runtime.events.S3Event
  import com.amazonaws.services.s3.AmazonS3Client
  import com.amazonaws.services.s3.model.GetObjectRequest

  import scala.collection.JavaConverters._

  val sourceBucket = "markingaccelerator-cloudlabhk-com"
  val mavenZipFileName = "apache-maven-3.3.9-bin.zip"

  val jdkFileName = "jdk-8u101-linux-x64.tar.gz"

  val projectZipFileName = "ite3101.zip"
  val pathScript = "path.sh"
  val tmp = "/tmp/"
  val mavenFolder = tmp + "maven"
  val projectFolder = tmp + "project"

  def decodeS3Key(key: String): String = URLDecoder.decode(key.replace("+", " "), "utf-8")

  def processNewCode(event: S3Event): String = {
    val s3Client = new AmazonS3Client
    val zipArchive = new ZipArchive

    val bucket = event.getRecords.asScala.map(record => decodeS3Key(record.getS3.getBucket.getName)).head
    val key = event.getRecords.asScala.map(record => decodeS3Key(record.getS3.getObject.getKey)).head
    val codeFileName = key.split("/").last
    val codeTestFileName = codeFileName.replace(".java", "Test.java")


    val codeFile = new File(tmp + codeFileName)
    s3Client.getObject(new GetObjectRequest(bucket, key), codeFile)
    val packageLine = Source.fromFile(tmp + codeFileName).getLines().find(s => s.contains("package")).getOrElse("")

    if (packageLine.isEmpty) {
      return "Invalid code without package declaration!"
    }
    val packageName = packageLine.replace("package", "").replaceAll(";", "").trim
    val packageFolder = packageName.replaceAll("\\.", "/")

    def replaceFileNameForFolder(s: String) = s.replaceAll(".zip", "")
      .replaceAll("-bin", "") //maven folder

    def downloadAndUnzip(zipFileName: String, unZipFolder: String): Unit = {
      val zipFile = new File(tmp + zipFileName)
      s3Client.getObject(new GetObjectRequest(sourceBucket, zipFileName), zipFile)
      zipArchive.unZip(tmp + zipFileName, tmp)
      new File(tmp + replaceFileNameForFolder(zipFileName)).renameTo(new File(unZipFolder))
    }

    //Remove the old project code!
    println(s"rm -rf $projectFolder" !)
    downloadAndUnzip(projectZipFileName, projectFolder)

    val testFile = new File(s"$projectFolder/src/test/java/$packageFolder/$codeTestFileName")
    println(testFile)
    if (!testFile.exists()) {
      return s"No test found for $codeFileName"
    }

    println(s"cp $codeFile $projectFolder/src/main/java/$packageFolder/" !)

    val jdkFolder = new File(tmp + "jdk")
    if (!jdkFolder.exists()) {
      //for the case of container reuse
      val s3Object = s3Client.getObject(new GetObjectRequest(sourceBucket, jdkFileName))
      TarUtils.createDirectoryFromTarGz(s3Object.getObjectContent, jdkFolder)
      downloadAndUnzip(mavenZipFileName, mavenFolder)
    }

    println(s"chmod -R 777 $tmp/jdk/jdk1.8.0_101" !)
    println(s"chmod -R 777 $mavenFolder/bin/" !)

    println(s"rm -rf $projectFolder/target/" !)

    val testClassName = codeFileName.replace(".java", "Test")
    val testClass = s"$packageName.$testClassName"
    println(s"env JAVA_HOME=/tmp/jdk/jdk1.8.0_101 $mavenFolder/bin/mvn -Dmaven.repo.local=/tmp/repo -q -f $projectFolder -Dtest=$testClass test surefire-report:report" !)
    println(s"ls -al $projectFolder/target/surefire-reports/" !)

    Source.fromFile(s"$projectFolder/target/surefire-reports/$testClass.txt").getLines.mkString
  }
}