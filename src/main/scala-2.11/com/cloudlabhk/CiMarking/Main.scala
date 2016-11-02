package com.cloudlabhk.CiMarking


import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.amazonaws.services.s3.transfer.TransferManager

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
  val resultBucket = "cimarking-cloudlabhk.com"
  val jdkFileName = "jdk-8u101-linux-x64.tar.gz"

  val projectZipFileName = "MultipleChoicesTestMarking.zip"
  val tmp = "/tmp/"
  val projectFolder = tmp + "project"


  def decodeS3Key(key: String): String = URLDecoder.decode(key.replace("+", " "), "utf-8")

  def processNewCode(event: S3Event): String = {
    val s3Client = new AmazonS3Client
    val zipArchive = new ZipArchive

    val bucket = event.getRecords.asScala.map(record => decodeS3Key(record.getS3.getBucket.getName)).head
    val key = event.getRecords.asScala.map(record => decodeS3Key(record.getS3.getObject.getKey)).head

    val prefix = key.replace("/McMarker.java", "")
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
      zipFile.delete()
      new File(tmp + replaceFileNameForFolder(zipFileName)).renameTo(new File(unZipFolder))
    }

    def cleanOldBuild: Unit = {
      println(s"cp $codeFile $projectFolder/src/main/java/$packageFolder/" !)
      println(s"chmod -R 777 $projectFolder" !)
      println(s"rm -rf $projectFolder/target/" !)
      println(s"rm -rf $projectFolder/build/" !)
    }

    def setupJdk: Unit = {
      val jdkFolder = new File(tmp + "jdk")
      if (!jdkFolder.exists()) {
        //for the case of container reuse
        val s3Object = s3Client.getObject(new GetObjectRequest(sourceBucket, jdkFileName))
        TarUtils.createDirectoryFromTarGz(s3Object.getObjectContent, jdkFolder)
        println(s"du -sh /tmp/jdk/jdk1.8.0_101/" !)
        //println(s"rm -rf $JAVA_HOME/src.zip $JAVA_HOME/javafx-src.zip $JAVA_HOME/man" !)
        println(s"rm -rf /tmp/jdk/jdk1.8.0_101/*src.zip " +
          s"/tmp/jdk/jdk1.8.0_101/lib/missioncontrol " +
          s"/tmp/jdk/jdk1.8.0_101/lib/visualvm " +
          s"/tmp/jdk/jdk1.8.0_101/lib/*javafx* " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/plugin.jar " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/ext/jfxrt.jar " +
          s"/tmp/jdk/jdk1.8.0_101/jre/bin/javaws " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/javaws.jar " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/desktop " +
          s"/tmp/jdk/jdk1.8.0_101/jre/plugin " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/deploy* " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/*javafx* " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/*jfx* " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/amd64/libdecora_sse.so " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/amd64/libprism_*.so " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/amd64/libfxplugins.so " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/amd64/libglass.so " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/amd64/libgstreamer-lite.so " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/amd64/libjavafx*.so " +
          s"/tmp/jdk/jdk1.8.0_101/jre/lib/amd64/libjfx*.so" !)
        println(s"du -sh /tmp/jdk/jdk1.8.0_101/" !)
      }
      println(s"chmod -R 777 $tmp/jdk/jdk1.8.0_101" !)
    }
    def runTest: Unit = {
      println(s"du -sh $tmp" !)
      println(s"env JAVA_HOME=$tmp/jdk/jdk1.8.0_101 env GRADLE_USER_HOME=$tmp $projectFolder/gradlew test -p $projectFolder" !)
      println(s"du -sh $tmp" !)
    }

    def uploadTestResult: Unit = {
      val credentialProviderChain = new DefaultAWSCredentialsProviderChain
      val tx = new TransferManager(credentialProviderChain.getCredentials)
      val markUpload = tx.upload(resultBucket, s"$prefix/mark.txt", new File("/tmp/mark.txt"))
      markUpload.waitForUploadResult

      val testFolder = new File("/tmp/project/build/reports/tests/");
      if (testFolder.exists()) {
        val testUpload = tx.uploadDirectory(resultBucket, s"$prefix/test/", testFolder, true)
        testUpload.waitForCompletion

        val testResultUpload = tx.uploadDirectory(resultBucket, s"$prefix/test-results/", new File("/tmp/project/build/test-results/"), true)
        testResultUpload.waitForCompletion
        tx.shutdownNow
        println("Upload test result completed")
      }
    }

    def calculateMarks = {
      import scala.xml.XML
      val marks = List(("readFolderPath", 5),
        ("printRow", 4),
        ("print2DStringArray", 3),
        ("getWrongAnswers", 13),
        ("getTotalMarks", 7),
        ("printTotalMarkReport", 3),
        ("printFirstRow", 3),
        ("printAnswerReport", 4),
        ("printWrongAnswerReport", 1),
        ("populateStudentAnswers", 3),
        ("getStudentId", 8),
        ("getDigit", 10),
        ("getAllAnswers", 3),
        ("getAnswerForQuestion", 13)
      )
      val testResultXml = "/tmp/project/build/test-results/test/TEST-hk.edu.vtc.it3101.mcmarking.McMarkerTest.xml"
      println("testResultXml " + new File(testResultXml).exists())
      val markMessage =
        (new File(testResultXml).exists()) match {
          case true => {
            val xml = XML.loadFile(testResultXml)
            val passedTest = (xml \\ "testsuite" \\ "testcase").filter(x =>
              (x \\ "failure").size == 0).map(_.attribute("name").get.text).toSet

            val detailMarks = marks.map { case (t, m) => (t, passedTest.contains(t) match {
              case true => m
              case false => 0
            })
            }
            val finalMark = detailMarks.map(_._2).sum
            val details = detailMarks.map { case (t, m) => s"$t : $m" }.mkString("\n")
            s"Total Coding Mark: $finalMark\n$details"
          }
          case false => "You code cannot build!"
        }

      val writer = new PrintWriter(new File("/tmp/mark.txt"))
      writer.write(markMessage)
      writer.close
    }

    def deleteSourceJava: Unit = {
      val s3client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain)
      s3client.deleteObject(new DeleteObjectRequest(bucket, key))
    }

    //Remove the old project code!
    println(s"rm -rf $projectFolder" !)
    downloadAndUnzip(projectZipFileName, projectFolder)

    val testFile = new File(s"$projectFolder/src/test/java/$packageFolder/$codeTestFileName")
    println(testFile)
    if (!testFile.exists()) {
      return s"No test found for $codeFileName"
    }

    cleanOldBuild
    setupJdk
    runTest
    calculateMarks
    uploadTestResult
    deleteSourceJava

    "OK"
  }
}