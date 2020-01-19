package com.oda.i2b2.variantview

import com.mongodb.client.MongoCollection
import com.mongodb.MongoClientOptions
import com.mongodb.MongoClient
import com.mongodb.ServerAddress
import org.bson.Document
import scala.collection.JavaConverters._

class MongoFactory(server: String, database: String) {

  // ssl disabled
  val mongoClientOptions = MongoClientOptions.builder.sslEnabled(false).sslInvalidHostNameAllowed(true).build()
  val mongoServer = new ServerAddress(server)
  val mongoClient = new MongoClient(List(mongoServer).asJava, mongoClientOptions)
  val mongoDB = mongoClient.getDatabase(database)
  
  def getCollection(collection: String): MongoCollection[Document]  = {
    mongoDB.getCollection(collection)
  }

}
