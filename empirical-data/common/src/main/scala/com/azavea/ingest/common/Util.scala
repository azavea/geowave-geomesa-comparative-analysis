package com.azavea.ingest.common

import java.io._
import java.util._
import java.net._
import org.apache.commons.io.IOUtils
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.{AmazonS3Client, AmazonS3URI}

object Util {
  def readFile(fileUri: String): String = {
    val uri = new URI(fileUri)

    val is = uri.getScheme match {
      case "file" =>
        new FileInputStream(new File(uri))
      case "http" =>
        uri.toURL.openStream
      case "s3" =>
        val client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain)
        val s3uri = new AmazonS3URI(uri)
        client.getObject(s3uri.getBucket, s3uri.getKey).getObjectContent()
      case _ =>
          throw new IllegalArgumentException(s"Resource at $uri is not valid")
    }

    try {
      IOUtils.toString(is)
    } finally {
      is.close()
    }
  }
}
