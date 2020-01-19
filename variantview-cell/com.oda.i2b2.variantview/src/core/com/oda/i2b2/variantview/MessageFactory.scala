package com.oda.i2b2.variantview

import edu.harvard.i2b2.common.exception.I2B2Exception
import edu.harvard.i2b2.common.util.jaxb.DTOFactory
import edu.harvard.i2b2.common.util.jaxb.JAXBUtil
import edu.harvard.i2b2.common.util.jaxb.JAXBUtilException

import com.oda.i2b2.variantview.datavo.i2b2message.ApplicationType
import com.oda.i2b2.variantview.datavo.i2b2message.BodyType
import com.oda.i2b2.variantview.datavo.i2b2message.FacilityType
import com.oda.i2b2.variantview.datavo.i2b2message.MessageControlIdType
import com.oda.i2b2.variantview.datavo.i2b2message.MessageHeaderType
import com.oda.i2b2.variantview.datavo.i2b2message.PasswordType
import com.oda.i2b2.variantview.datavo.i2b2message.ProcessingIdType
import com.oda.i2b2.variantview.datavo.i2b2message.RequestHeaderType
import com.oda.i2b2.variantview.datavo.i2b2message.RequestMessageType
import com.oda.i2b2.variantview.datavo.i2b2message.ResponseHeaderType
import com.oda.i2b2.variantview.datavo.i2b2message.ResponseMessageType
import com.oda.i2b2.variantview.datavo.i2b2message.ResultStatusType
import com.oda.i2b2.variantview.datavo.i2b2message.SecurityType
import com.oda.i2b2.variantview.datavo.i2b2message.StatusType
import com.oda.i2b2.variantview.datavo.pdo.query.FactOutputOptionType
import com.oda.i2b2.variantview.datavo.pdo.query.FilterListType
import com.oda.i2b2.variantview.datavo.pdo.query.GetPDOFromInputListRequestType
import com.oda.i2b2.variantview.datavo.pdo.query.InputOptionListType
import com.oda.i2b2.variantview.datavo.pdo.query.ItemType
import com.oda.i2b2.variantview.datavo.pdo.query.OutputOptionListType
import com.oda.i2b2.variantview.datavo.pdo.query.OutputOptionNameType
import com.oda.i2b2.variantview.datavo.pdo.query.OutputOptionSelectType
import com.oda.i2b2.variantview.datavo.pdo.query.OutputOptionType
import com.oda.i2b2.variantview.datavo.pdo.query.PanelType
import com.oda.i2b2.variantview.datavo.pdo.query.PatientListType
import com.oda.i2b2.variantview.datavo.pdo.query.PdoQryHeaderType
import com.oda.i2b2.variantview.datavo.pdo.query.PdoRequestTypeType
import com.oda.i2b2.variantview.datavo.pdo.query.RequestType
import com.oda.i2b2.variantview.datavo.pdo.ObservationSet
import com.oda.i2b2.variantview.datavo.pdo.PatientDataType
import com.oda.i2b2.variantview.datavo.pdo.PatientIdType
import com.oda.i2b2.variantview.datavo.pdo.PatientSet
import com.oda.i2b2.variantview.datavo.pdo.ObservationType
import com.oda.i2b2.variantview.datavo.varmessages.PhenotypeGenotypeAssocSet
import com.oda.i2b2.variantview.datavo.varmessages.PhenotypeGenotypeAssoc
import com.oda.i2b2.variantview.datavo.varmessages.MongoObject

import org.apache.axiom.om.OMElement
import org.apache.axiom.om.impl.builder.StAXOMBuilder

import java.io.StringReader
import java.io.StringWriter
import java.io.InputStream
import java.math.BigDecimal
import java.util.Date
import java.util.List

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader

//remove if not needed
import scala.collection.JavaConversions._

object MessageFactory {

  private var log: Log = LogFactory.getLog(MessageFactory.getClass)

  private val _PatientId_QNAME: QName =
    new QName("http://www.i2b2.org/xsd/hive/pdo/1.1/", "patient_id")

  private val _PatientSet_QNAME: QName =
    new QName("http://www.i2b2.org/xsd/hive/pdo/1.1/", "patient_set")

  private val _Variants_QNAME: QName = 
    new QName("http://www.i2b2.org/xsd/cell/varview/1.0/", "variants")

  private val _PhenotypeGenotypeAssocSet_QNAME: QName = 
    new QName("http://www.i2b2.org/xsd/cell/varview/1.0/", "phenotype_genotype_assoc_set")

  private val _MongoObject_QNAME: QName = 
    new QName("http://www.i2b2.org/xsd/cell/varview/1.0/", "mongo_object")

  private var jaxbUtil: JAXBUtil = new JAXBUtil(
    JAXBConstant.DEFAULT_PACKAGE_NAME)

  /**
    * Function creates response OMElement from xml string
    * @param xmlString
    * @return OMElement
    * @throws XMLStreamException
    */
  def createResponseOMElementFromString(xmlString: String): OMElement = {
    var returnElement: OMElement = null
    try {
      val strReader: StringReader = new StringReader(xmlString)
      val xif: XMLInputFactory = XMLInputFactory.newInstance()
      val reader: XMLStreamReader = xif.createXMLStreamReader(strReader)
      val builder: StAXOMBuilder = new StAXOMBuilder(reader)
      returnElement = builder.getDocumentElement
    } catch {
      case xmlStreamEx: XMLStreamException => {
        log.error("Error while converting response PDO to OMElement")
        throw xmlStreamEx
      }

    }
    returnElement
  }

  /**
    * Function to build patientData body type
    *
    * @param mo
    *           mongo object to be returned
    * @return BodyType object
    */
  def createBodyType(mo: MongoObject): BodyType = {
    val bodyType: BodyType = new BodyType()
    bodyType.getAny.add(
       new JAXBElement[MongoObject](_MongoObject_QNAME,
                                  classOf[MongoObject],
                                  null,
                                  mo))
   bodyType
  }

  /**
    * Function to create response  message header based
    * on request message header
    *
    * @return MessageHeader object
    */
  def createResponseMessageHeader(
      messageHeaderType: MessageHeaderType): MessageHeaderType = {
    val messageHeader: MessageHeaderType = new MessageHeaderType()
    messageHeader.setI2B2VersionCompatible(new BigDecimal("1.1"))
    messageHeader.setHl7VersionCompatible(new BigDecimal("2.4"))
    val appType: ApplicationType = new ApplicationType()
    appType.setApplicationName("Variantview Cell")
    appType.setApplicationVersion("1.0")
    messageHeader.setSendingApplication(appType)
    val facility: FacilityType = new FacilityType()
    facility.setFacilityName("i2b2 Hive")
    messageHeader.setSendingFacility(facility)
    if (messageHeaderType != null) {
      if (messageHeaderType.getSendingApplication != null) {
        messageHeader.setReceivingApplication(
          messageHeaderType.getSendingApplication)
      }
      messageHeader.setReceivingFacility(messageHeaderType.getSendingFacility)
    }
    val currentDate: Date = new Date()
    val factory: DTOFactory = new DTOFactory()
    messageHeader.setDatetimeOfMessage(
      factory.getXMLGregorianCalendar(currentDate.getTime))
    val mcIdType: MessageControlIdType = new MessageControlIdType()
    mcIdType.setInstanceNum(1)
    if (messageHeaderType != null) {
      if (messageHeaderType.getMessageControlId != null) {
        mcIdType.setMessageNum(
          messageHeaderType.getMessageControlId.getMessageNum)
        mcIdType.setSessionId(
          messageHeaderType.getMessageControlId.getSessionId)
      }
    }
    messageHeader.setMessageControlId(mcIdType)
    val proc: ProcessingIdType = new ProcessingIdType()
    proc.setProcessingId("P")
    proc.setProcessingMode("I")
    messageHeader.setProcessingId(proc)
    messageHeader.setAcceptAcknowledgementType("AL")
    messageHeader.setApplicationAcknowledgementType("AL")
    messageHeader.setCountryCode("US")
    if (messageHeaderType != null) {
      if (messageHeaderType.getProjectId != null) {
        messageHeader.setProjectId(messageHeaderType.getProjectId)
      }
    }
    messageHeader
  }

  /**
    * Function to create response message type
    * @param messageHeader
    * @param respHeader
    * @param bodyType
    * @return ResponseMessageType
    */
  def createResponseMessageType(messageHeader: MessageHeaderType,
                                respHeader: ResponseHeaderType,
                                bodyType: BodyType): ResponseMessageType = {
    val respMsgType: ResponseMessageType = new ResponseMessageType()
    respMsgType.setMessageHeader(messageHeader)
    respMsgType.setMessageBody(bodyType)
    respMsgType.setResponseHeader(respHeader)
    respMsgType
  }

  /**
    * Function to convert ResponseMessageType to string
    * @param respMessageType
    * @return String
    * @throws Exception
    */
  def convertToXMLString(respMessageType: ResponseMessageType): String = {
    var strWriter: StringWriter = null
    try {
      val jaxbUtil: JAXBUtil = new JAXBUtil(JAXBConstant.DEFAULT_PACKAGE_NAME)
      strWriter = new StringWriter()
      val objectFactory: com.oda.i2b2.variantview.datavo.i2b2message.ObjectFactory =
        new com.oda.i2b2.variantview.datavo.i2b2message.ObjectFactory()
      jaxbUtil.marshaller(objectFactory.createResponse(respMessageType),
                          strWriter)
    } catch {
      case e: JAXBUtilException => {
        e.printStackTrace()
        throw new I2B2Exception(
          "Error converting response message type to string " +
            e.getMessage,
          e)
      }

    }
    strWriter.toString
  }

  /**
    * Function to build Response message type and return it as an XML string
    *
    * @param mo
    *           mongo object to be included in response
    *
    * @return A String data type containing the ResponseMessage in XML format
    * @throws Exception
    */
  def createBuildResponse(messageHeaderType: MessageHeaderType,
                           mo: MongoObject): ResponseMessageType = {
    var respMessageType: ResponseMessageType = null
    val messageHeader: MessageHeaderType = createResponseMessageHeader(
      messageHeaderType)
    val respHeader: ResponseHeaderType =
      createResponseHeader("DONE", "processing completed")
    val bodyType: BodyType = createBodyType(mo)
    respMessageType =
      createResponseMessageType(messageHeader, respHeader, bodyType)
    respMessageType
  }

  /**
    * Function to get i2b2 Request message header
    *
    * @return RequestHeader object
    */
  def getRequestHeader(): RequestHeaderType = {
    val reqHeader: RequestHeaderType = new RequestHeaderType()
    reqHeader.setResultWaittimeMs(120000)
    reqHeader
  }

  /**
    * Function to create Response with given error message
    * @param messageHeaderType
    * @param errorMessage
    * @return
    * @throws Exception
    */
  def doBuildErrorResponse(messageHeaderType: MessageHeaderType,
                           errorMessage: String): ResponseMessageType = {
    var respMessageType: ResponseMessageType = null
    val messageHeader: MessageHeaderType = createResponseMessageHeader(
      messageHeaderType)
    val respHeader: ResponseHeaderType =
      createResponseHeader("ERROR", errorMessage)
    respMessageType =
      createResponseMessageType(messageHeader, respHeader, null)
    respMessageType
  }

  /**
    * Creates ResponseHeader for the given type and value
    * @param type
    * @param value
    * @return
    */
  private def createResponseHeader(`type`: String,
                                   value: String): ResponseHeaderType = {
    val respHeader: ResponseHeaderType = new ResponseHeaderType()
    val status: StatusType = new StatusType()
    status.setType(`type`)
    status.setValue(value)
    val resStat: ResultStatusType = new ResultStatusType()
    resStat.setStatus(status)
    respHeader.setResultStatus(resStat)
    respHeader
  }

// Create request message type out of the other message parts
  def createRequestMessageType(messageHeader: MessageHeaderType,
                               reqHeader: RequestHeaderType,
                               bodyType: BodyType): RequestMessageType = {
    val reqMsgType: RequestMessageType = new RequestMessageType()
    reqMsgType.setMessageHeader(messageHeader)
    reqMsgType.setMessageBody(bodyType)
    reqMsgType.setRequestHeader(reqHeader)
    reqMsgType
  }

// Create request message header
  def createRequestMessageHeaderType(domain: String,
                                     username: String,
                                     password: String,
                                     projectid: String): MessageHeaderType = {
    val messageHeader: MessageHeaderType = new MessageHeaderType()
    messageHeader.setI2B2VersionCompatible(new BigDecimal("1.1"))
    messageHeader.setHl7VersionCompatible(new BigDecimal("2.4"))
    val sendAppType: ApplicationType = new ApplicationType()
    sendAppType.setApplicationName("Variantview Cell")
    sendAppType.setApplicationVersion("1.0")
    messageHeader.setSendingApplication(sendAppType)
    val sendFacility: FacilityType = new FacilityType()
    sendFacility.setFacilityName("i2b2 Hive")
    messageHeader.setSendingFacility(sendFacility)
    val recvAppType: ApplicationType = new ApplicationType()
    recvAppType.setApplicationName("i2b2_DataRepositoryCell")
    recvAppType.setApplicationVersion("1.7")
    messageHeader.setReceivingApplication(recvAppType)
    val recvFacility: FacilityType = new FacilityType()
    recvFacility.setFacilityName("PHS")
    messageHeader.setReceivingFacility(recvFacility)
    val currentDate: Date = new Date()
    val factory: DTOFactory = new DTOFactory()
    messageHeader.setDatetimeOfMessage(
      factory.getXMLGregorianCalendar(currentDate.getTime))
    val secType: SecurityType = new SecurityType()
    secType.setDomain(domain)
    secType.setUsername(username)
    val pwt: PasswordType = new PasswordType()
    pwt.setValue(password)
    secType.setPassword(pwt)
    messageHeader.setSecurity(secType)
    val mcIdType: MessageControlIdType = new MessageControlIdType()
    mcIdType.setInstanceNum(0)
    mcIdType.setMessageNum(generateMessageId())
    mcIdType.setSessionId("1")
    messageHeader.setMessageControlId(mcIdType)
    val proc: ProcessingIdType = new ProcessingIdType()
    proc.setProcessingId("P")
    proc.setProcessingMode("I")
    messageHeader.setProcessingId(proc)
    messageHeader.setAcceptAcknowledgementType("AL")
    messageHeader.setApplicationAcknowledgementType("AL")
    messageHeader.setCountryCode("DE")
    messageHeader.setProjectId(projectid)
    messageHeader
  }

// Creates RequestHeader
  def createRequestHeaderType(): RequestHeaderType = {
    val reqHeader: RequestHeaderType = new RequestHeaderType()
    reqHeader.setResultWaittimeMs(25000)
    reqHeader
  }

// Create PDOHeader
  def createPDOHeader(): PdoQryHeaderType = {
    val prop = PropertyFactory()
    val pqht: PdoQryHeaderType = new PdoQryHeaderType()
    pqht.setPatientSetLimit(0)
    pqht.setEstimatedTime(prop.timeout)
    pqht.setRequestType(PdoRequestTypeType.GET_PDO_FROM_INPUT_LIST)
    pqht
  }

// Create PDORequest
  def createPDORequest(pdoCollID: Int,
                       conceptsList: FilterListType): RequestType = {
    val pdoReqType: GetPDOFromInputListRequestType =
      new GetPDOFromInputListRequestType()
// Specify patient list by collection id
    val iolt: InputOptionListType = new InputOptionListType()
    val plt: PatientListType = new PatientListType()
    plt.setPatientSetCollId(String.valueOf(pdoCollID))
    plt.setMin(0)
    plt.setMax(0)
    iolt.setPatientList(plt)
    pdoReqType.setInputList(iolt)
    var conceptAvailable: Boolean = true
    if (conceptsList.getPanel.size == 0) conceptAvailable = false
// Specify filter list if a concept was specified
    if (conceptAvailable) {
      pdoReqType.setFilterList(conceptsList)
    }
// Specify output options
    val oolt: OutputOptionListType = new OutputOptionListType()
    oolt.setNames(OutputOptionNameType.ASATTRIBUTES)
// Patient set
    val ootForPatientSet: OutputOptionType = new OutputOptionType()
    ootForPatientSet.setSelect(OutputOptionSelectType.USING_INPUT_LIST)
    ootForPatientSet.setOnlykeys(false)
    ootForPatientSet.setTechdata(true)
    oolt.setPatientSet(ootForPatientSet)
// Observation set
    if (conceptAvailable) {
      val ootForObsSet: FactOutputOptionType = new FactOutputOptionType()
      ootForObsSet.setBlob(false)
      ootForObsSet.setOnlykeys(false)
      ootForObsSet.setTechdata(true)
      oolt.setObservationSet(ootForObsSet)
// Concept informations (we need the concept_path)
      val ootForConceptSet: OutputOptionType = new OutputOptionType()
      ootForConceptSet.setSelect(OutputOptionSelectType.USING_FILTER_LIST)
      ootForConceptSet.setOnlykeys(false)
      ootForConceptSet.setTechdata(true)
      ootForConceptSet.setBlob(true)
      oolt.setConceptSetUsingFilterList(ootForConceptSet)
// Create a general OutputOptionType
      val oot: OutputOptionType = new OutputOptionType()
      oot.setOnlykeys(false)
      oot.setSelect(OutputOptionSelectType.USING_FILTER_LIST)
      oot.setTechdata(true)
// Remaining sets
      oolt.setModifierSetUsingFilterList(oot)
      //oolt.setEventSet(oot)
      oolt.setObserverSetUsingFilterList(oot)
    }
    pdoReqType.setOutputOption(oolt)
    pdoReqType
  }

// Convert XML string to RequestMessageType (JAXB) = unmarshalling the request (from client)
  def convertXMLTORequestMessageType(xmlString: String): RequestMessageType = {
    val rmt: RequestMessageType = jaxbUtil
      .unMashallFromString(xmlString)
      .getValue
      .asInstanceOf[RequestMessageType]
    if (rmt == null) {
      throw new I2B2Exception(
        "Null value from unmarshall for VDO xml : " + xmlString)
    }
    rmt
  }

// Convert XML string to ResponseMessageType (JAXB) = unmarshalling the response (from CRC)
  def convertXMLTOResponseMessageType(xmlString: String): ResponseMessageType = {
    val rmt: ResponseMessageType = jaxbUtil
      .unMashallFromString(xmlString)
      .getValue
      .asInstanceOf[ResponseMessageType]
    if (rmt == null) {
      throw new I2B2Exception(
        "Null value from unmarshall for VDO xml : " + xmlString)
    }
    rmt
  }

  def convertXMLTOResponseMessageType(xmlIn: InputStream): ResponseMessageType = {
    val rmt: ResponseMessageType = jaxbUtil
      .unMarshalFromInputStream(xmlIn)
      .getValue
      .asInstanceOf[ResponseMessageType]
    if (rmt == null) {
      throw new I2B2Exception(
        "Null value from unmarshall for VDO xml file ")
    }
    rmt
  }
// Convert ResponseMessageType (JAXB) to XML string = marshalling the response (to client)
  def convertResponseMessageTypeToXML(
      respMessageType: ResponseMessageType): String = {
    var strWriter: StringWriter = null
    strWriter = new StringWriter()
    val objectFactory: com.oda.i2b2.variantview.datavo.i2b2message.ObjectFactory =
      new com.oda.i2b2.variantview.datavo.i2b2message.ObjectFactory()
    jaxbUtil.marshaller(objectFactory.createResponse(respMessageType),
                        strWriter)
    strWriter.toString
  }

// Convert RequestMessageType (JAXB) to XML string = marshalling the request (to CRC)
  def convertRequestMessageTypeToXML(
      reqMessageType: RequestMessageType): String = {
    var strWriter: StringWriter = null
    strWriter = new StringWriter()
    val objectFactory: com.oda.i2b2.variantview.datavo.i2b2message.ObjectFactory =
      new com.oda.i2b2.variantview.datavo.i2b2message.ObjectFactory()
    jaxbUtil.marshaller(objectFactory.createRequest(reqMessageType), strWriter)
    strWriter.toString
  }

// Convert XML string to OMElement (for responses after marshalling)
  def convertXMLToOMElement(xmlString: String): OMElement = {
    var returnElement: OMElement = null
    val strReader: StringReader = new StringReader(xmlString)
    val xif: XMLInputFactory = XMLInputFactory.newInstance()
    val reader: XMLStreamReader = xif.createXMLStreamReader(strReader)
    val builder: StAXOMBuilder = new StAXOMBuilder(reader)
    returnElement = builder.getDocumentElement
    returnElement
  }

  /**
    * Function to generate i2b2 message header message number
    *
    * @return String
    */
  private def generateMessageId(): String = {
    val strWriter: StringWriter = new StringWriter()
    for (i <- 0.until(20)) {
      val num: Int = getValidAcsiiValue
      strWriter.append(num.toChar)
    }
    strWriter.toString
  }

  /**
    * Function to generate random number used in message number
    *
    * @return int
    */
  private def getValidAcsiiValue(): Int = {
    val rand: Int = Math.round(Math.random() * 62).toInt
    val number: Int = 
        if (rand < 10) { 48 + rand }
        else {
            if (rand > 9 && rand < 37) { 65 + (rand - 9) }
            else { 97 + (rand - 36) }
        }
    number
  }

}

