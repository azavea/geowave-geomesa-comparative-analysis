package com.azavea.ca.server.results

case class RunResult(
  testName: String,
  gmResult: Option[TestResult],
  gwResult: Option[TestResult],
  isTest: Boolean
)

object RunResult {
  def apply(testName: String, gmResult: TestResult, gwResult: TestResult): RunResult =
    apply(testName, Some(gmResult), Some(gwResult), true)

  def apply(testName: String, gmResult: TestResult, gwResult: TestResult, isTest: Boolean): RunResult =
    apply(testName, Some(gmResult), Some(gwResult), isTest)

  def geomesaOnly(testName: String, gmResult: TestResult): RunResult =
    apply(testName, Some(gmResult), None, true)

  def geomesaOnly(testName: String, gmResult: TestResult, isTest: Boolean): RunResult =
    apply(testName, Some(gmResult), None, isTest)

  def geowaveOnly(testName: String, gwResult: TestResult): RunResult =
    apply(testName, None, Some(gwResult), true)

  def geowaveOnly(testName: String, gwResult: TestResult, isTest: Boolean): RunResult =
    apply(testName, None, Some(gwResult), isTest)
}
