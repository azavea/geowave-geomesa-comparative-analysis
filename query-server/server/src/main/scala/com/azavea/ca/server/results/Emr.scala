package com.azavea.ca.server.results

import com.amazonaws.auth.{DefaultAWSCredentialsProviderChain, AWSCredentials, AWSCredentialsProvider}
import com.amazonaws.services.elasticmapreduce._
import com.amazonaws.services.elasticmapreduce.model._

import scala.collection.JavaConverters._

case class ClusterDefinition(clusterId: String, clusterName: String, instanceType: String, count: Int)

object Emr {
  val credentials = new DefaultAWSCredentialsProviderChain()
  val emr = new AmazonElasticMapReduceClient(credentials)

  private val HISTORY_SEARCH_LIMIT = 50

  private def withRetry[T](f: => T): T =
    try {
      f
    } catch {
      case e: AmazonElasticMapReduceException =>
        Thread.sleep(1000)
          f
    }

  def getAllClusters(): Seq[ClusterDefinition] = {
    var i = 0
    def f(marker: String = ""): Seq[ClusterSummary] = {
      if(i >= 300 / 50) { Seq() }
      else {
        println(s"Fetching $marker")
        i += 1
        try {
          val r = withRetry {
            if(marker.isEmpty)
              emr.listClusters()
            else {
              val lcr = new ListClustersRequest
              lcr.setCreatedAfter(new java.text.SimpleDateFormat("yyyy-MM-dd").parse("2016-09-04"))
              emr.listClusters()
            }
          }
          //r.getClusters ++ (if(r.
          val newMarker = r.getMarker
          println(newMarker)
          r.getClusters.asScala ++ (if(newMarker != null && !newMarker.isEmpty) { f(newMarker) } else { Seq() })
        } catch {
          case e: AmazonElasticMapReduceException => Seq()
        }
      }
    }
    val clusters = f()

    clusters.flatMap { c =>
      val clusterId = c.getId
      val clusterName = c.getName
      println(s"Reading values for $clusterId - $clusterName")

      // val lir = new ListInstancesRequest()
      // lir.setClusterId(c.getId)
      // lir.setInstanceGroupTypes(Seq("MASTER").asJava)
      // val masterInstances = withRetry { emr.listInstances(lir).getInstances.asScala }
      // if(masterInstances.isEmpty) None
      // else {
      //   val master = masterInstances.head.getPublicDnsName()
        // lir.setInstanceGroupTypes(Seq("CORE", "TASK").asJava)
        // val workers = emr.listInstances(lir)
        // println(master)
        // val size = workers.getInstances.size
        // val workerId = workers.getInstances.asScala.head.getEc2InstanceId

      val lig = new ListInstanceGroupsRequest()
      lig.setClusterId(clusterId)
      val groups = withRetry { emr.listInstanceGroups(lig).getInstanceGroups.asScala }

      groups.find(_.getInstanceGroupType() == "CORE") match {
        case Some(ig) =>
          val (instanceType, count) =
            (ig.getInstanceType, ig.getRequestedInstanceCount)
          Some(ClusterDefinition(clusterId, clusterName, instanceType, count))
        case None =>
          None
      }
    }
  }
}
