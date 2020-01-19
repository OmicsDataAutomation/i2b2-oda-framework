package com.oda.i2b2.variantview

import edu.harvard.i2b2.common.exception.I2B2Exception

import com.oda.i2b2.variantview.datavo.i2b2message.MessageHeaderType
import com.oda.i2b2.variantview.datavo.i2b2message.ResponseMessageType
import com.oda.i2b2.variantview.datavo.pdo.PatientSet
import com.oda.i2b2.variantview.datavo.pdo.PatientType
import com.oda.i2b2.variantview.datavo.pdo.ObservationSet
import com.oda.i2b2.variantview.datavo.pdo.ObservationType

import com.oda.i2b2.variantview.datavo.varmessages.PhenotypeGenotypeAssocSet
import com.oda.i2b2.variantview.datavo.varmessages.PhenotypeGenotypeAssoc
import com.oda.i2b2.variantview.datavo.varmessages.MongoObject

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.List

import java.lang.Exception
import com.mongodb.MongoSocketReadException

//remove if not needed
import scala.collection.JavaConversions._

/**
  *   
  */
object PluginHelper {

  private var log: Log = LogFactory.getLog(PluginHelper.getClass)

  /**
    * Helper function reads the request message and creates PatientDataMessage
    *
    * After requested analysis is done, and results stored in MongoDB, we
    * return a message with sequence of variant objects and a mongodb object
    * @param requestPdo
    * @return String i2b2 message which contains variants and mongodb object
    *
    * @throws I2B2Exception
    */
  def extractVariants(requestPdo: String): String = {
    var responsePdo: String = null
    // check the incoming request
    if (requestPdo == null) {
      val errMsg: String = "NULL request PDO was received"
      log.error(errMsg)
      val responseMessageType: ResponseMessageType =
        MessageFactory.doBuildErrorResponse(null, errMsg)
      responsePdo = MessageFactory.convertToXMLString(responseMessageType)
    } else {
      // the bulk of the processing happens here
      // when we create new PatientDataMessage, we talk to CRC
      // using getPdoFromInputList, then spawn off a spark job
      // on the remote EMR cluster. The remote cluster writes
      // results to MongoDB
      var msg: PatientDataMessage = null
      try msg = new PatientDataMessage(requestPdo)
      catch {
        case e: Exception => {
          e.printStackTrace
          val errMsg: String = "Error creating PDM Object "+e.getMessage
          log.error(errMsg)
          val responseMessageType: ResponseMessageType =
            MessageFactory.doBuildErrorResponse(null, errMsg)
          responsePdo = MessageFactory.convertToXMLString(responseMessageType)
          throw new I2B2Exception(errMsg)
        }

      }

      var mo: MongoObject = null
      // read results from mongoDB
      try mo = msg.getMongoObject
      catch {
        case e: Exception => {
          val errMsg: String = "Error returning results form MongoDB "+e.getMessage
          log.error(errMsg)
          val responseMessageType: ResponseMessageType =
            MessageFactory.doBuildErrorResponse(null, errMsg)
          responsePdo = MessageFactory.convertToXMLString(responseMessageType)
          throw new I2B2Exception(errMsg)
        }

      }
      try {
        // encapsulate mongo objects into response to webclient
        val messageHeader: MessageHeaderType =
          msg.getRequestMessageType.getMessageHeader
        val responseMessageType: ResponseMessageType =
          MessageFactory.createBuildResponse(messageHeader, mo)
        responsePdo = MessageFactory.convertToXMLString(responseMessageType)
      } catch {
        case e: Exception => {
          val errMsg: String = "Error responding to request "+e.getMessage
          log.error(errMsg)
          val responseMessageType: ResponseMessageType =
            MessageFactory.doBuildErrorResponse(null, errMsg)
          responsePdo = MessageFactory.convertToXMLString(responseMessageType)
          throw new I2B2Exception(errMsg)
        }

      }
    }
    log.debug("ResponsePdo = " + responsePdo)
    responsePdo
  }

}

