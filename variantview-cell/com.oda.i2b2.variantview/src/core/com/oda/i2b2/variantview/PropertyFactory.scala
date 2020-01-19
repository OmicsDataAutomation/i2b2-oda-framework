package com.oda.i2b2.variantview

import java.util.Properties
import java.io.File
import java.io.FileInputStream

class PropertyFactory() {

  val hadoopconf = System.getenv("HADOOP_CONF_DIR")
  var proppath = "launch-properties.flat"
  if(hadoopconf != null){
    proppath = hadoopconf + File.separator + "launch-properties.flat"
  }  
  val prop = new Properties
  val propfile = new File(proppath)
  val fsin = new FileInputStream(propfile)
  prop.load(fsin)
  
  // mongo
  val mongoip = prop.getProperty("mongoip")
  val mongodb = prop.getProperty("mongodb")
  
  // spark
  val sparktime = prop.getProperty("sparktimeout")
  val driverip = prop.getProperty("driverip")
  val masterip = prop.getProperty("masterip")
  val master = prop.getProperty("master")
  val sparkhome = prop.getProperty("sparkhome")
  val deploy = prop.getProperty("deploy")
  
  // genomicsdb
  val hostfile =  prop.getProperty("hostfile")
  val loader = prop.getProperty("loader_file")
  val workspace = prop.getProperty("workspace")
  val array = prop.getProperty("array")
  val dbprops = prop.getProperty("dbprops")
  val gdbsparkjar = prop.getProperty("gdbspark")
  
  // i2b2
  val timeout = prop.getProperty("timeout").toInt
  
  fsin.close()  

}

object PropertyFactory {
  def apply():PropertyFactory = {
    new PropertyFactory()
  }

}
