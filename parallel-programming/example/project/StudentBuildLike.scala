import sbt._
import Keys._
import Settings._

import java.io.{File, IOException, FileInputStream}
import org.apache.commons.codec.binary.Base64

import scala.util.parsing.json.JSON
import scalaj.http._

import scala.util.{Try, Success, Failure}

case class MapMapString (val map: Map[String, Map[String, String]])
/**
  * Note: keep this class concrete (i.e., do not convert it to abstract class or trait).
  */
class StudentBuildLike protected() extends CommonBuild {

  lazy val root = project.in(file(".")).settings(
    course := "",
    assignment := "",
    submitSetting,
    commonSourcePackages := Seq(), // see build.sbt
    courseId := "",
    styleCheckSetting,
    libraryDependencies += "ch.epfl.lamp" % "scala-grading-runtime_2.11" % "0.3"
  ).settings(packageSubmissionFiles: _*)


  //TODOs
  //- handle 400s coursera error codes

  /** **********************************************************
    * SUBMITTING A SOLUTION TO COURSERA
    */

  val packageSubmission = TaskKey[File]("packageSubmission")

  val packageSubmissionFiles = {
    // the packageSrc task uses Defaults.packageSrcMappings, which is defined as concatMappings(resourceMappings, sourceMappings)
    // in the packageSubmission task we only use the sources, not the resources.
    inConfig(Compile)(Defaults.packageTaskSettings(packageSubmission, Defaults.sourceMappings))
  }

  /** Task to submit a solution to coursera */
  val submit = inputKey[Unit]("submit")

  lazy val submitSetting = submit := {
    val args: Seq[String] = Def.spaceDelimited("<arg>").parsed
    val (_, _) = (clean.value, (compile in Compile).value) // depends on clean & compile task
    val s: TaskStreams = streams.value // for logging
    val jar = (packageSubmission in Compile).value

    val assignmentName = assignment.value
    val assignmentDetails = assignmentsMap.value(assignmentName)
    val assignmentKey = assignmentDetails.key
    val courseName = course.value
    val partId = assignmentDetails.partId
    val itemId = assignmentDetails.itemId

    val (email, secret) = args match {
      case email :: secret :: Nil =>
        (email, secret)
      case _ =>
        val inputErr =
          s"""|Invalid input to `submit`. The required syntax for `submit` is:
              |submit <email-address> <submit-token>
              |
              |The submit token is NOT YOUR LOGIN PASSWORD.
              |It can be obtained from the assignment page:
              |https://www.coursera.org/learn/$courseName/programming/$itemId
          """.stripMargin
        s.log.error(inputErr)
        failSubmit()
    }

    /** Check that the jar exists, isn't empty, isn't crazy big, and can be read
      * If so, encode jar as base64 so we can send it to Coursera
      */
    def prepareJar(jar: File): String = {
      val errPrefix = "Error submitting assignment jar: "
      val fileLength = jar.length()
      if (!jar.exists()) {
        s.log.error(errPrefix + "jar archive does not exist\n" + jar.getAbsolutePath)
        failSubmit()
      } else if (fileLength == 0L) {
        s.log.error(errPrefix + "jar archive is empty\n" + jar.getAbsolutePath)
        failSubmit()
      } else if (fileLength > maxSubmitFileSize) {
        s.log.error(errPrefix + "jar archive is too big. Allowed size: " +
          maxSubmitFileSize + " bytes, found " + fileLength + " bytes.\n" +
          jar.getAbsolutePath)
        failSubmit()
      } else {
        val bytes = new Array[Byte](fileLength.toInt)
        val sizeRead = try {
          val is = new FileInputStream(jar)
          val read = is.read(bytes)
          is.close()
          read
        } catch {
          case ex: IOException =>
            s.log.error(errPrefix + "failed to read sources jar archive\n" + ex.toString)
            failSubmit()
        }
        if (sizeRead != bytes.length) {
          s.log.error(errPrefix + "failed to read the sources jar archive, size read: " + sizeRead)
          failSubmit()
        } else encodeBase64(bytes)
      }
    }

    val base64Jar = prepareJar(jar)
    val json =
      s"""|{
          |   "assignmentKey":"$assignmentKey",
          |   "submitterEmail":"$email",
          |   "secret":"$secret",
          |   "parts":{
          |      "$partId":{
          |         "output":"$base64Jar"
          |      }
          |   }
          |}""".stripMargin

    def postSubmission[T](data: String): Try[HttpResponse[String]] = {
      val http = Http("https://www.coursera.org/api/onDemandProgrammingScriptSubmissions.v1")
      val hs = List(
        ("Cache-Control", "no-cache"),
        ("Content-Type", "application/json")
      )
      s.log.info("Connecting to Coursera...")
      val response = Try(http.postData(data)
                         .headers(hs)
                         .option(HttpOptions.connTimeout(10000)) // scalaj default timeout is only 100ms, changing that to 10s
                         .asString) // kick off HTTP POST
      response
    }

    val connectMsg =
      s"""|Attempting to submit "$assignmentName" assignment in "$courseName" course
          |Using:
          |- email: $email
          |- submit token: $secret""".stripMargin
    s.log.info(connectMsg)

    def reportCourseraResponse(response: HttpResponse[String]): Unit = {
      val code = response.code
      val respBody = response.body

      /* Sample JSON response from Coursera
      {
        "message": "Invalid email or token.",
        "details": {
          "learnerMessage": "Invalid email or token."
        }
      }
      */

      code match {
        // case Success, Coursera responds with 2xx HTTP status code
        case cde if cde >= 200 && cde < 300 =>
          val successfulSubmitMsg =
            s"""|Successfully connected to Coursera. (Status $code)
                |
                |Assignment submitted successfully!
                |
                |You can see how you scored by going to:
                |https://www.coursera.org/learn/$courseName/programming/$itemId/
                |and clicking on "My Submission".""".stripMargin
          s.log.info(successfulSubmitMsg)

        // case Failure, Coursera responds with 4xx HTTP status code (client-side failure)
        case cde if cde >= 400 && cde < 500 =>
          val result = JSON.parseFull(respBody)
          val learnerMsg = result match {
            case Some(resp: MapMapString) => // MapMapString to get around erasure
              resp.map("details")("learnerMessage")
            case Some(x) => // shouldn't happen
              "Could not parse Coursera's response:\n" + x
            case None =>
              "Could not parse Coursera's response:\n" + respBody
          }
          val failedSubmitMsg =
            s"""|Submission failed.
                |There was something wrong while attempting to submit.
                |Coursera says:
                |$learnerMsg (Status $code)""".stripMargin
          s.log.error(failedSubmitMsg)
      }
    }

    // kick it all off, actually make request
    postSubmission(json) match {
      case Success(resp) => reportCourseraResponse(resp)
      case Failure(e) =>
        val failedConnectMsg =
          s"""|Connection to Coursera failed.
              |There was something wrong while attempting to connect to Coursera.
              |Check your internet connection.
              |${e.toString}""".stripMargin
        s.log.error(failedConnectMsg)
    }

  }

  def failSubmit(): Nothing = {
    sys.error("Submission failed")
  }

  /**
    * *****************
    * DEALING WITH JARS
    */
  def encodeBase64(bytes: Array[Byte]): String =
    new String(Base64.encodeBase64(bytes))


  /** *****************************************************************
    * RUNNING WEIGHTED SCALATEST & STYLE CHECKER ON DEVELOPMENT SOURCES
    */

  val styleCheck = TaskKey[Unit]("styleCheck")
  val styleCheckSetting = styleCheck := {
    val (_, sourceFiles, assignments, assignmentName) = ((compile in Compile).value, (sources in Compile).value, assignmentsMap.value, assignment.value)
    val styleSheet = assignments(assignmentName).styleSheet
    val logger = streams.value.log
    if (styleSheet != "") {
      val (feedback, score) = StyleChecker.assess(sourceFiles, styleSheet)
      logger.info(
        s"""|$feedback
            |Style Score: $score out of ${StyleChecker.maxResult}""".stripMargin)
    } else logger.warn("Can't check style: there is no style sheet provided.")
  }

}
