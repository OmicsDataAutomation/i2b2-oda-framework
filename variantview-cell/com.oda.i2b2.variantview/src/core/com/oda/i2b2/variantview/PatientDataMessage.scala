package com.oda.i2b2.variantview

import scala.util.Random
import java.io.File
import java.util.HashMap
import java.util.LinkedList
import java.util.List
import java.util.ArrayList
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.io.File
import java.io.FileInputStream

import org.apache.axiom.om.OMElement
import org.apache.axis2.AxisFault
import org.apache.axis2.Constants
import org.apache.axis2.addressing.EndpointReference
import org.apache.axis2.client.Options
import org.apache.axis2.client.ServiceClient
import org.apache.axis2.transport.http.HTTPConstants

import org.xml.sax.SAXException

import edu.harvard.i2b2.common.exception.I2B2Exception
import edu.harvard.i2b2.common.util.jaxb.JAXBUnWrapHelper
import edu.harvard.i2b2.common.util.jaxb.JAXBUtil
import edu.harvard.i2b2.common.util.jaxb.JAXBUtilException

import com.oda.i2b2.variantview.datavo.i2b2message.BodyType
import com.oda.i2b2.variantview.datavo.i2b2message.MessageHeaderType
import com.oda.i2b2.variantview.datavo.i2b2message.RequestHeaderType
import com.oda.i2b2.variantview.datavo.i2b2message.RequestMessageType
import com.oda.i2b2.variantview.datavo.i2b2message.ResponseHeaderType
import com.oda.i2b2.variantview.datavo.i2b2message.ResponseMessageType
import com.oda.i2b2.variantview.datavo.pdo.ConceptSet
import com.oda.i2b2.variantview.datavo.pdo.EventSet
import com.oda.i2b2.variantview.datavo.pdo.ModifierSet
import com.oda.i2b2.variantview.datavo.pdo.ObservationSet
import com.oda.i2b2.variantview.datavo.pdo.PatientDataType
import com.oda.i2b2.variantview.datavo.pdo.ObservationType
import com.oda.i2b2.variantview.datavo.pdo.ObserverSet
import com.oda.i2b2.variantview.datavo.pdo.PatientSet
import com.oda.i2b2.variantview.datavo.pdo.PatientType
import com.oda.i2b2.variantview.datavo.varmessages.PatientSetsType
import com.oda.i2b2.variantview.datavo.varmessages.VARRequest
import com.oda.i2b2.variantview.datavo.varmessages.Concepts
import com.oda.i2b2.variantview.datavo.varmessages.Concept
import com.oda.i2b2.variantview.datavo.varmessages.ConceptValue
import com.oda.i2b2.variantview.datavo.varmessages.PhenotypeGenotypeAssocSet
import com.oda.i2b2.variantview.datavo.varmessages.PhenotypeGenotypeAssoc
import com.oda.i2b2.variantview.datavo.varmessages.MongoObject

import com.oda.i2b2.variantview.datavo.pdo.query.PatientListType
import com.oda.i2b2.variantview.datavo.pdo.query.ItemType
import com.oda.i2b2.variantview.datavo.pdo.query.PatientDataResponseType
import com.oda.i2b2.variantview.datavo.pdo.query.PdoQryHeaderType
import com.oda.i2b2.variantview.datavo.pdo.query.RequestType

import org.apache.spark.launcher.SparkLauncher
import org.apache.spark.launcher.SparkAppHandle
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import java.util.concurrent.CountDownLatch


import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import javax.xml.bind.JAXBElement

import PatientDataMessage._
import org.bson.Document

//remove if not needed
import scala.collection.JavaConversions._

object PatientDataMessage {

  private var log: Log = LogFactory.getLog(classOf[PatientDataMessage])

}

/**
  * The PatientDataMessage class is a helper class
  * Here we parse the incoming request message for arguments
  * from the webclient
  * Then we query the CRC cell (if needed) to get patient id
  * information for the (optional) input patient set
  * Lastly, we kick off a spark job on the remote EMR cluster
  * to do the requested analysis
  */
class PatientDataMessage /**
  * The constructor
  */
(requestPdo: String) {

  private var jaxbUtil: JAXBUtil = new JAXBUtil(
    JAXBConstant.DEFAULT_PACKAGE_NAME)

  var reqMessageType: RequestMessageType = null

  var crcCS: ConceptSet = null
  var crcPS: PatientSet = null
  var crcMS: ModifierSet = null
  var crcES: EventSet = null
  var crcObS: ObserverSet = null
  val prop = PropertyFactory()

  log.info("Step 1")

  // unmarshall the incoming request
  try {
    val jaxbElement: JAXBElement[_] = jaxbUtil.unMashallFromString(requestPdo)
    if (jaxbElement == null) {
      throw new I2B2Exception(
        "Null value from unmarshall for PDO xml : " + requestPdo)
    }
    this.reqMessageType = jaxbElement.getValue.asInstanceOf[RequestMessageType]
  } catch {
    case e: JAXBUtilException => {
      e.printStackTrace()
      log.error(e.getMessage, e)
      throw new I2B2Exception("Umashaller error: " + e.getMessage + requestPdo,
                              e)
    }

  }

  log.info("Step 2")

  val unwrapHelper: JAXBUnWrapHelper = new JAXBUnWrapHelper()

// Read out some important informations from message header
  var domain: String = null
  var username: String = null
  var password: String = null
  var project: String = null
  var messagenum: String = null

  try {
    domain = this.reqMessageType.getMessageHeader.getSecurity.getDomain
    username = this.reqMessageType.getMessageHeader.getSecurity.getUsername
    password =
      this.reqMessageType.getMessageHeader.getSecurity.getPassword.getValue
    project = this.reqMessageType.getMessageHeader.getProjectId
    // going to use this (seemingly unique?) message num value to write to mongo collection
    // and ensure we deal with multi-user environment correctly
    messagenum = this.reqMessageType.getMessageHeader.getMessageControlId.getMessageNum
    log.info("domain:" + domain + " username:" + username)
  } catch {
    case e: Exception => {
      log.error(
        "Incoming request XML is not valid: " + e.getMessage +
          e)
      throw new I2B2Exception(
        "Error message delivered from server: Incomplete or invalid XML request header")
    }

  }

  log.info("Step 3")

// get coll id from input request
  var inputPList: VARRequest = null

  try inputPList = unwrapHelper
    .getObjectByClass(
      this.reqMessageType.getMessageBody.getAny.asInstanceOf[List[Object]],
      classOf[VARRequest])
    .asInstanceOf[VARRequest]
  catch {
    case e: JAXBUtilException => {
      log.error("Error while getting patient coll id from request")
      throw new I2B2Exception(
        "Error delivered from server: Getting patient_coll_id from request")
    }

  }
  
  log.info("Step 3.1")

  if (inputPList == null) log.info("inputPList is null")

  // /source/variantview-cell/com.oda.i2b2.variantview/gensrc/com/oda/i2b2/variantview/datavo/varmessages
  // import above objects to work with concepts like patient list below
  println(inputPList.getPatientSets)
  // collIDs is what we're going to use to query CRC cell
  val collIDs: java.util.List[Integer] = inputPList.getPatientSets.getPatientSetCollId

  var conceptAvailable: Boolean = true

  val conceptParentValue = inputPList.getConcepts.getConcept.map(_.getParentVariantName)
  // do we need the below check or should we just let the results be
  // completely filtered if user asks for other types of variants?
  if (!(conceptParentValue.contains("SNV/SNP") ||
        conceptParentValue.contains("indel") ||
        conceptParentValue.contains("deletion") ||
        conceptParentValue.contains("insertion"))) {
      log.error("Genomics store only supports variants that are SNV/SNP, indel, insertion or deletion")
      throw new I2B2Exception(
        "Unsupported variant type in query")
  }
  val conceptValue = inputPList.getConcepts.getConcept.map(_.getConceptValue.getValue)

  val conceptsList = inputPList.getFilterList();
  var analysisType = inputPList.getAnalyses.getAnalysis match {
    case y if y.contains("allele_counts") => "allele_counts"
    case _ => throw new I2B2Exception("Only total allele counts is supported in demo.")
  }
  val genotypeFilter = inputPList.getGenotypeFilter()
  log.info("This is genotype filter")
  log.info(genotypeFilter.isEmpty) 
  val fallback = scala.collection.immutable.List(("a", "b", Kind.toKind("homref"), 1))

  // parse the genotype filter into a list of tuples that the spark
  // backend will use while filtering
  val genotypeExpr = try ConditionParser.parse(genotypeFilter)
  catch{
    case e: Exception => {
      log.error("Error parsing genotype filter:"+e.getMessage)
      throw new I2B2Exception(
        "Error parsing genotype filter:"+e.getMessage)
    }
    fallback
  } 
  val snpFilter = genotypeExpr.map{case (a, b, c, d) => a+","+b+","+c.id+","+d.toString}

  var crcResponseFile:String = ""

  if (collIDs.size > 0){  
    val collID = collIDs.get(0)

    val QTSUrl: String = inputPList.getQTSUrl

    log.info("Step 4")

    // Build message body of CRC request
    val crcPdoqryheader: PdoQryHeaderType = MessageFactory.createPDOHeader()

    val crcReqType: RequestType =
      MessageFactory.createPDORequest(collID, conceptsList)
  
    val i2b2mesFac: com.oda.i2b2.variantview.datavo.i2b2message.ObjectFactory =
      new com.oda.i2b2.variantview.datavo.i2b2message.ObjectFactory()

    val crcBodType: BodyType = i2b2mesFac.createBodyType()

    val pdoQryFac: com.oda.i2b2.variantview.datavo.pdo.query.ObjectFactory =
      new com.oda.i2b2.variantview.datavo.pdo.query.ObjectFactory()

    crcBodType.getAny.add(pdoQryFac.createPdoheader(crcPdoqryheader))

    crcBodType.getAny.add(pdoQryFac.createRequest(crcReqType))

    log.info("Step 5")

    // Assemble request message to CRC cell
    var crcResult: OMElement = null

    val crcReqHeaderType: RequestHeaderType =
      MessageFactory.createRequestHeaderType()

    val crcMesHead: MessageHeaderType = MessageFactory
      .createRequestMessageHeaderType(domain, username, password, project)

    val crcReqMessageType: RequestMessageType = MessageFactory
      .createRequestMessageType(crcMesHead, crcReqHeaderType, crcBodType)

    val crcRequest: OMElement = MessageFactory.convertXMLToOMElement(
      MessageFactory.convertRequestMessageTypeToXML(crcReqMessageType))

    log.info("Step 6")

    // Uncomment for debugging purposes
    log.debug(
      "Message to CRC cell:\n\n\n" +
        MessageFactory.convertRequestMessageTypeToXML(crcReqMessageType) +
        "\n\n\n")

    val options: Options = new Options()

    options.setTo(new EndpointReference(QTSUrl + "pdorequest"))

    options.setTransportInProtocol(Constants.TRANSPORT_HTTP)

    options.setProperty(Constants.Configuration.ENABLE_REST,
                      Constants.VALUE_TRUE)

    // need to ensure this timeout is relatively large
    // other large-ish patient sets, or ones where we ask for
    // concepts may time out
    // also, setting the timeout multiple ways because just using
    // setTimeoutInMilliseconds seemingly wasn't working
    options.setTimeOutInMilliSeconds(180000)
    options.setProperty(HTTPConstants.SO_TIMEOUT, 180000)
    options.setProperty(HTTPConstants.CONNECTION_TIMEOUT, 180000)


    log.info("Step 7")

    var sender: ServiceClient = null

    try {
      sender = new ServiceClient()
      sender.setOptions(options)
      crcResult = sender.sendReceive(crcRequest)
    } catch {
      case e: AxisFault => {
        log.error("Error while sending / receiving message to / from CRC cell")
        throw new I2B2Exception(
          "Error delivered from server: Communication with CRC cell failed")
      }

    }


    log.info("Step 8")

    // Uncomment for debugging purposes
    //  log.info(
    //    "Answer message from CRC cell: \n\n\n\n" + crcResult +
    //      "\n\n\n\n")

    // write this xml to file to pass to the jar
    crcResponseFile = s"crc_${Random.alphanumeric take 10 mkString("")}.xml"
    val crcResponseXML = new java.io.File(crcResponseFile)
    // file will get deleted when VM goes away...so on jboss restart
    crcResponseXML.deleteOnExit()
    val bw = new java.io.BufferedWriter(new java.io.FileWriter(crcResponseXML))
    //bw.write(crcResult.toString)
    // we used to create a string and write that to file
    // but that caused memory/heap space issues with large patient sets
    crcResult.serialize(bw)
    bw.close()

    try {
      // Convert response into JAXB (unmarshall)
      val crcIn: FileInputStream = new java.io.FileInputStream(crcResponseFile)
      val crcRMT: ResponseMessageType =
        MessageFactory.convertXMLTOResponseMessageType(crcIn)
      crcIn.close()
      // Unwrap response message and check for errors
      val crcStatusType: String =
        crcRMT.getResponseHeader.getResultStatus.getStatus.getType
      if (crcStatusType.!=("DONE")) {
        log.error(
          "Status type of CRC response is not 'DONE'!")
        throw new I2B2Exception(
          "Error delivered from server: Status type of CRC response is not 'DONE'. See log files for details.")
      }
    // Unwrap message body
      val pdrt: PatientDataResponseType = unwrapHelper
        .getObjectByClass(crcRMT.getMessageBody.getAny,
                        classOf[PatientDataResponseType])
        .asInstanceOf[PatientDataResponseType]
      if (pdrt.getPatientData == null) {
        throw new NullPointerException()
      }
      log.info("Step 9")
      log.info("HADOOP env:"+System.getenv("HADOOP_CONF_DIR"))
      //patient set
      crcPS = pdrt.getPatientData.getPatientSet
      if (conceptAvailable) {
        crcCS = pdrt.getPatientData.getConceptSet
        var crcOS = pdrt.getPatientData.getObservationSet.map(_.getObservation.map(_.getConceptCd.getValue))
      }
    } catch {
      case e: NullPointerException => {
        log.error("CRC response invalid or imcomplete")
        throw new I2B2Exception(
          "Error delivered from server: CRC response is invalid or incomplete")
      }

      case e: JAXBUtilException => {
        log.error("Error while unmarshalling CRC response")
        throw new I2B2Exception(
          "Error delivered from server: Unmarshalling CRC response")
      }

      case e: Exception => {
        log.error("Issue with the CRC reponse")
        throw new I2B2Exception("Issue with the CRC Response")
      }
    }
  }else{
    if (!analysisType.contains("genomic_only")){
      throw new I2B2Exception("Incomplete input for selected report type.")
    }
  }

  // this is where the spark job is setup and launched
  // the properties are pulled from the property factory
  try{
    val base_args = Seq("--driver_ip", prop.driverip, 
                        "--master_ip", prop.masterip, 
                        "--master", prop.master, 
                        "--hostfile", prop.hostfile,
                        "--loader_file", prop.loader,
                        "--workspace", prop.workspace,
                        "--array", prop.array, 
                        "--dbprops", prop.dbprops, 
                        "--analysis", analysisType.toString, 
                        "--mongoip", prop.mongoip, 
                        "--mongodb", prop.mongodb, 
                        "--mongocollection", messagenum)
   var interm_args: Seq[String] = Seq()
   if(crcResponseFile.nonEmpty){
      val crc_args: Seq[String] = Seq("--crc_xml", crcResponseFile)
      interm_args = base_args ++ crc_args
    }else{
      interm_args = base_args
    }
    val region_args: Seq[String] = "--regions"+:conceptValue
    val variant_type_args: Seq[String] = "--varianttypes"+:conceptParentValue
    var snp_filter_args = Seq[String]()
    if(!(genotypeFilter == null || genotypeFilter.isEmpty)){
      snp_filter_args = "--snp_filter"+:snpFilter
    }
    val arg_list = interm_args ++ region_args ++ variant_type_args ++ snp_filter_args
    val env = Map(
      "SPARK_PRINT_LAUNCH_COMMAND" -> "1"
    )
    var launcher: SparkLauncher = new SparkLauncher(env.asJava)
                 .setMainClass("com.oda.gdbspark.App")
                 .setSparkHome(prop.sparkhome)
                 .setMaster(prop.master)
                 .setDeployMode(prop.deploy)
                 .setAppResource(prop.gdbsparkjar)
                 .addAppArgs(arg_list:_*)		
                 .setConf(SparkLauncher.DRIVER_EXTRA_CLASSPATH, prop.sparkhome+"/jars/*")
                 .setConf(SparkLauncher.EXECUTOR_EXTRA_CLASSPATH, prop.sparkhome+"/jars/*")
    if (crcResponseFile != null){
      launcher.addFile(crcResponseFile)
    }             
    val handle: SparkAppHandle = launcher.startApplication()
    val countDownLatch: CountDownLatch = new CountDownLatch(1);
    handle.addListener(new SparkAppHandle.Listener() {
      override def stateChanged(handle: SparkAppHandle) {
        if (handle.getState().isFinal()) {
          countDownLatch.countDown();
        }
      }

      override def infoChanged(handle: SparkAppHandle) {}
    });
    // start spark job with timeout so that i2b2 users aren't twiddling 
    // their thumbs for too long...
    val clean: Boolean = countDownLatch.await(prop.timeout, TimeUnit.SECONDS);
    println(handle.getAppId() + " ended in state " + handle.getState());
    // if we didn't exit cleanly, we timed out
    if (!clean) {
      handle.kill()
      log.error("Timed out waiting for response from spark job")
      throw new I2B2Exception(
        "Error delivered from server: Timed out waiting for response from spark job.")
    }
  } catch {
    case e: NullPointerException => {
      println(e)
      e.printStackTrace
      log.error("Issue submitting spark job")
      throw new I2B2Exception(
        "Error delivered from server: Unable to submit spark job due to missing input.")
    }
    case e: com.mongodb.MongoSocketReadException => {
      throw new I2B2Exception("Error communicating with MongoDB: "+ e.getMessage)
    }
    case e: JAXBUtilException => {
      log.error("Error while unmarshalling CRC response")
      throw new I2B2Exception(
        "Error delivered from server: Unmarshalling CRC response")
    }
    case e: Exception => {
      throw new I2B2Exception("Error submitting spark job: " + e.getMessage)
    }
  }

  /**
    * Generate an aggregate query to MongoDB based on the report type.
    * Mongo queries are constructed like nested json documnets, this function
    * will make more sense if you read about MongoDB aggregate queries.
    * @retuns MongoDB result document
    */
  def getMongoBuilderByAnalysis(): List[Document] = {
    var builder = new java.util.ArrayList[Document]().asScala;
    if(analysisType contains "allele_count"){
      builder += new Document("$group", new Document(
                  "_id" , new Document("name", "$name")).append(
                  "data", new Document(
                    "$push", new Document(
                      "Contig","$_1").append("Start", "$_2").append("snp_id", "$snp_id").append("RefAlleleCount", "$_3").append("AltAlleleCount","$_4"))))
    
    }else{
      throw new I2B2Exception("Only total allele counts supported in demo.")
    }
    builder
  }

  /**
    * parse analysis types handler
    */
  def getMongoCollectionByAnalysis(): String = {
    analysisType.toString match {
      case analysisType if analysisType contains "allele_counts" => "allele_counts";
      case _ => throw new I2B2Exception("Only total allele counts supported in demo.")
    }
  }

  /**
    * Connets into MongoDB using the MongoFactory, aggregates results, and parses result as JSON
    * Collection is dropped after results are parsed into java. 
    * If you want to look at how results are populated in MongoDB, comment out collection.drop
    */
  def getMongoObject(): MongoObject = {
    val mo = new MongoObject();
    val mongoFactory = new MongoFactory(prop.mongoip, prop.mongodb)    
    val collection = mongoFactory.getCollection(messagenum+"_"+getMongoCollectionByAnalysis);
    val builder = getMongoBuilderByAnalysis();
    val concept_sets = collection.aggregate(builder)
    mo.setMongoObject(com.mongodb.util.JSON.serialize(concept_sets))
    // comment this out if you need to see how results are populated in MongoDB
    collection.drop()
    mo
  }

  def getRequestMessageType(): RequestMessageType = reqMessageType

}

