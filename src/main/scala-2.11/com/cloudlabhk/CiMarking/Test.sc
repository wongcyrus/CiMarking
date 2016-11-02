import scala.xml.XML

val xml = XML.loadFile("E:\\CiMarking\\src\\test\\resources\\partial.xml")

val passedTest = (xml \\ "testsuite" \\ "testcase").filter(x =>
  (x \\ "failure").isEmpty).map(_.attribute("name").get.text).toSet

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


val detailMarks = marks.map { case (t, m) => (t, passedTest.contains(t) match {
  case true => m
  case false => 0
})
}

val finalMark = detailMarks.map(_._2).sum
