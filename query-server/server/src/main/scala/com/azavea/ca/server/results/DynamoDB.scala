package com.azavea.ca.server.results

import com.amazonaws.auth.{DefaultAWSCredentialsProviderChain, AWSCredentials, AWSCredentialsProvider}
import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.dynamodbv2.model._

import java.util.UUID
import scala.collection.JavaConverters._

object DynamoDB {
  val TEST_TABLE_NAME = "gwgm-ca-results"
  val REAL_TABLE_NAME = "gwgm-ca-run-results"

  def credentials = new DefaultAWSCredentialsProviderChain()
  lazy val db = new AmazonDynamoDBClient(credentials)

  private def a(s: String): AttributeValue =
    new AttributeValue(s)

  private def a(n: Long): AttributeValue = {
    val a = new AttributeValue
    a.setN(n.toString)
    a
  }

  private def a(n: Int): AttributeValue = {
    val a = new AttributeValue
    a.setN(n.toString)
    a
  }

  private def a(b: Boolean): AttributeValue = {
    val a = new AttributeValue
    a.setBOOL(b)
    a
  }

  def saveResult(runResult: RunResult): RunResult = {
    if(runResult.isTest) {
      // Put GM results
      runResult.gmResult.foreach { r =>
        val uuid = UUID.randomUUID.toString

        db.putItem(
          TEST_TABLE_NAME,
          Map(
            "uuid" -> a(uuid),
            "queryName" -> a(runResult.testName),
            "system" -> a("GM"),
            "clusterId" -> a(r.clusterId),
            "startTime" -> a(r.startTime),
            "endTime" -> a(r.endTime),
            "duration" -> a(r.duration),
            "timeAtFirstResult" -> a(r.timeAtFirstResult),
            "result" -> a(r.result),
            "test" -> a(runResult.isTest)
          ).asJava
        )
      }

      // Put GW results
      runResult.gwResult.foreach { r =>
        val uuid = UUID.randomUUID.toString

        db.putItem(
          TEST_TABLE_NAME,
          Map(
            "uuid" -> a(uuid),
            "queryName" -> a(runResult.testName),
            "system" -> a("GW"),
            "clusterId" -> a(r.clusterId),
            "startTime" -> a(r.startTime),
            "endTime" -> a(r.endTime),
            "duration" -> a(r.duration),
            "timeAtFirstResult" -> a(r.timeAtFirstResult),
            "result" -> a(r.result),
            "test" -> a(runResult.isTest)
          ).asJava
        )
      }
    } else {
      // Put GM results
      runResult.gmResult.foreach { r =>
        val uuid = UUID.randomUUID.toString

        db.putItem(
          REAL_TABLE_NAME,
          Map(
            "uuid" -> a(uuid),
            "queryName" -> a(runResult.testName),
            "system" -> a("GM"),
            "clusterId" -> a(r.clusterId),
            "startTime" -> a(r.startTime),
            "endTime" -> a(r.endTime),
            "duration" -> a(r.duration),
            "timeAtFirstResult" -> a(r.timeAtFirstResult),
            "result" -> a(r.result)
          ).asJava
        )
      }

      // Put GW results
      runResult.gwResult.foreach { r =>
        val uuid = UUID.randomUUID.toString

        db.putItem(
          REAL_TABLE_NAME,
          Map(
            "uuid" -> a(uuid),
            "queryName" -> a(runResult.testName),
            "system" -> a("GW"),
            "clusterId" -> a(r.clusterId),
            "startTime" -> a(r.startTime),
            "endTime" -> a(r.endTime),
            "duration" -> a(r.duration),
            "timeAtFirstResult" -> a(r.timeAtFirstResult),
            "result" -> a(r.result)
          ).asJava
        )
      }
    }
    runResult
  }
}
