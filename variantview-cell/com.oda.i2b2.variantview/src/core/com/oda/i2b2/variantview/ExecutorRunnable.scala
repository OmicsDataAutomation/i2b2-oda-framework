package com.oda.i2b2.variantview

import java.lang.Exception

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import ExecutorRunnable._

import scala.beans.{BeanProperty, BooleanBeanProperty}

//remove if not needed
import scala.collection.JavaConversions._

object ExecutorRunnable {

  private var log: Log = LogFactory.getLog(classOf[ExecutorRunnable])

}

/**
  * Implements thread runnable interface, to do the
  * processing using thread.
  */
class ExecutorRunnable extends Runnable {

  @BeanProperty
  var inputString: String = null

  @BeanProperty
  var outputString: String = null

  private var ex: Exception = null

  @BooleanBeanProperty
  var jobCompleteFlag: Boolean = false

  @BooleanBeanProperty
  var jobErrorFlag: Boolean = false

  def getJobCompleteFlag: Boolean = jobCompleteFlag
  def getJobErrorFlag: Boolean = jobErrorFlag

  def getJobException(): Exception = ex

  def setJobException(ex: Exception): Unit = {
    this.ex = ex
  }

  def run(): Unit = {
    log.debug("Running from thread, before extracting  ...")
    try {
      outputString = PluginHelper.extractVariants(inputString)
      this.jobCompleteFlag = true
      log.debug("Running from thread, completed extracting ...")
    } catch {
      case e: Exception => {
        this.jobErrorFlag = true
        this.ex = e 
      }
    }
  }

}

