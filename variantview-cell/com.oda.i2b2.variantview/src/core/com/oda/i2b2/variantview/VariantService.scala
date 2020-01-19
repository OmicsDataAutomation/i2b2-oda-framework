package com.oda.i2b2.variantview

import edu.harvard.i2b2.common.exception.I2B2Exception

import com.oda.i2b2.variantview.datavo.i2b2message.ResponseMessageType

import org.apache.axiom.om.OMElement

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import javax.xml.stream.XMLStreamException

import java.lang.Exception

import VariantService._

//remove if not needed
import scala.collection.JavaConversions._

object VariantService {

  private var log: Log = LogFactory.getLog(classOf[VariantService])

}

/**
  * This is webservice skeleton class. It passes incoming request 
  * from Variantview FE and passes it on to GenomicsDB core. 
  * Results are then written to MongoDB
  * where the webservice client retrieves them.
  *
  */
class VariantService {

  /**
    * This function is main webservice interface to get requests from
    * Variantview FE
    * It uses AXIOM elements(OMElement) to conveniently parse
    * xml messages.
    *
    * Requests may contain patientset, qturl, genomic & clinical concepts,
    * genotype filter and analysis type inside patientdata object. 
    * The response will be in i2b2 message, which will wrap variant 
    * and mongodb objects
    *
    * @param getVariantDataElement
    * @return OMElement in i2b2message format
    * @throws Exception
    */
  def getVariantData(getVariantDataElement: OMElement): OMElement = {
    log.debug("Received Request PDO Element " + getVariantDataElement)
    var returnElement: OMElement = null
    if (getVariantDataElement == null) {
      log.error("Incoming request is null")
      throw new I2B2Exception("Incoming request is null")
    }
    val requestElementString: String = getVariantDataElement.toString
    val prop = PropertyFactory()
    val waitTime: Long = prop.timeout
    // service could send back message with timeout error.
    var er: ExecutorRunnable = new ExecutorRunnable()
    er.setInputString(requestElementString)
    var t: Thread = new Thread(er)
    var pulmonaryDataResponse: String = null
    var pulmonaryErrorResponse: Exception = null
    var pulmonaryJobComplete: Boolean = er.getJobErrorFlag
    t.synchronized {
      t.start()
      try {
        if (waitTime > 0) {
          t.wait(waitTime)
        } else {
          t.wait()
        }
        pulmonaryJobComplete = er.getJobErrorFlag
        pulmonaryDataResponse = er.getOutputString
        pulmonaryErrorResponse = er.getJobException
        if (pulmonaryJobComplete || pulmonaryDataResponse == null) {
          if (er.getJobException != null) {
            //throw er.getJobException
            val responseMsgType: ResponseMessageType =
              MessageFactory.doBuildErrorResponse(null, er.getJobException.getMessage)
            pulmonaryDataResponse =
              MessageFactory.convertToXMLString(responseMsgType)
         } else if (er.isJobCompleteFlag == false) {
            val timeOuterror: String = "Result waittime millisecond <result_waittime_ms> :" +
                waitTime +
                " elapsed, try again with increased value"
            log.info(timeOuterror)
            val responseMsgType: ResponseMessageType =
              MessageFactory.doBuildErrorResponse(null, timeOuterror)
            pulmonaryDataResponse =
              MessageFactory.convertToXMLString(responseMsgType)
          } else {
            log.error("Error response is null")
            val responseMsgType: ResponseMessageType =
              MessageFactory.doBuildErrorResponse(null, "Error response is null")
            pulmonaryDataResponse =
              MessageFactory.convertToXMLString(responseMsgType)
          }
        }
      } catch {
        case e: InterruptedException => {
          e.printStackTrace()
          throw new I2B2Exception(
            "Thread error while running job " + requestElementString,
            e)
        }

      } finally {
        t.interrupt()
        er = null
        t = null
      }
    }
    try returnElement =
      MessageFactory.createResponseOMElementFromString(pulmonaryDataResponse)
    catch {
      case e: XMLStreamException => {
        e.printStackTrace()
        log.error(
          "Error creating OMElement from response string " + pulmonaryDataResponse,
          e)
      }

    }
    returnElement
  }

}


