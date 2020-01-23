### Installation

#### GDB-Mapping

The gdb-spark-api will require connection to the gdb-mapping database. 
* For postgres run: `createdb test`
* Update the alembic.ini to point to this database
* In the alembic folder run `alembic upgrade head` to run the migrations. 
* Use the importer to populate the datbase from the genomicsdb configuration jsons:
```{bash}
gdb-mapping-0.4/importer/importer/importer postgresql:///test /path/to/loader/json \
-r hg19 -o 1 -g /path/to/a/gene/mapping/file
```
* For more information on the importer run:
```
gdb-mapping-0.4/importer/importer/importer --help
```
* Provide a map file to register callsets with ehr_map. Register callsets with a callset_description file 
which is a tab-delimited file with two columns: ehr_map id (first column) and the callset name (second column).
```{bash}
gdb-mapping-0.4/importer/importer/importer postgresql:///test /path/to/loader/json \
-r hg19 -o 1 --ehr_map /path/to/callset/description/file \
--callset_description /path/to/callset/description/file
```
* Once the database is populated, update `variantview-cell/cfg/db-properties.flat` with the relevant information.

#### i2b2 Plugin

This section assumes that i2b2 has been installed and populated with data. 
Installation steps are:
* Git clone the variantview-webclient repo. Create a link (or copy the repo) to a subfolder 
(js-i2b2/cells/plugin/community) under the i2b2 webclient with the name VARVIEW. 
For instance, if the webclient is at /var/www/html/webclient put the repo under 
/var/www/html/webclient/js-i2b2/cells/plugin/community/VARVIEW
* Add information to the js-i2b2/i2b2_loader.js file to ensure the plugin FE is loaded correctly
```{bash}
sudo sed -i 's@i2b2.hive.tempCellsList = \[@i2b2.hive.tempCellsList = \[\n{ code:"VARVIEW",\n  forceloading: true,\n  forceConfigMsg: { params: \[\] },\n  roles: \[ "DATA_LDS", "DATA_DEID", "DATA_PROT" \],\n  forceDir: "cells/plugins/community"\n},@' i2b2_loader.js
```
* Install scala compiler (scalac): https://www.scala-lang.org/download/2.11.8.html
* Please download Spark version 2.1.1, prebuilt for Hadoop 2.7 and later from https://spark.apache.org/downloads.html and 
unzip it in /usr/lib/spark. Change the spark configuration files:
```{bash}
cd /usr/lib/spark/conf
cp spark-defaults.conf.template spark-defaults.conf
echo “spark.yarn.jars hdfs://spark-master-ip:8020/user/shrine/sparklib/*jar” >> spark.defaults.conf
```
* This repo also contains copies of edu.harvard.i2b2.xml and edu.harvard.i2b2.server-common (from the 1.7.08 version of i2b2). 
These are needed in order to compile the plugin. If you use a different version of i2b2, 
feel free to replace these with the correct version. 
However, you must copy over the edu.harvard.i2b2.xml/xsd/cell/varview directory since this defines a xml message used by the plugin.
* Copy (or use Apache Maven to download) the following list of jars to com.oda.i2b2.variantview/lib:
  * spark-launcher: http://central.maven.org/maven2/org/apache/spark/spark-launcher_2.11/2.1.1/spark-launcher_2.11-2.1.1.jar
  * spark-core: http://central.maven.org/maven2/org/apache/spark/spark-core_2.11/2.1.1/spark-core_2.11-2.1.1.jar
  * casbah-gridfs: http://central.maven.org/maven2/org/mongodb/casbah-gridfs_2.11/3.1.1/casbah-gridfs_2.11-3.1.1.jar
  * casbah-core: http://central.maven.org/maven2/org/mongodb/casbah-core_2.11/3.1.1/casbah-core_2.11-3.1.1.jar
  * casbah-commons: http://central.maven.org/maven2/org/mongodb/casbah-commons_2.11/3.1.1/casbah-commons_2.11-3.1.1.jar
  * casbah-query: http://central.maven.org/maven2/org/mongodb/casbah-query_2.11/3.1.1/casbah-query_2.11-3.1.1.jar
  * mongo-spark-connector: http://central.maven.org/maven2/org/mongodb/spark/mongo-spark-connector_2.10/2.1.1/mongo-spark-connector_2.10-2.1.1.jar
  * mongo-java-driver: http://central.maven.org/maven2/org/mongodb/mongo-java-driver/3.7.1/mongo-java-driver-3.7.1.jar
  * joda-time: http://central.maven.org	/maven2/joda-time/joda-time/2.9.9/joda-time-2.9.9.jar
  * scalaj-time: http://central.maven.org/maven2/org/scalaj/scalaj-time_2.11/0.8/scalaj-time_2.11-0.8.jar
  * jcifs: http://central.maven.org/maven2/org/samba/jcifs/jcifs/1.3.3/jcifs-1.3.3.jar
  * jersey-bundle: http://central.maven.org/maven2/com/sun/jersey/jersey-bundle/1.19.1/jersey-bundle-1.19.1.jar
  * scala-parser-combinators: http://central.maven.org/maven2/org/scala-lang/modules/scala-parser-combinators_2.11/1.1.1/scala-parser-combinators_2.11-1.1.1.jar
  * genomicsdb (if you haven't built yourself): https://repo1.maven.org/maven2/org/genomicsdb/genomicsdb/0.9.2/genomicsdb-0.9.2.jar
* Copy the demo gdbsparkapi `genomicsdb/gdb-spark-api-1.0-demo.jar` to /usr/lib/spark/jars. 
* Copy the jersey-bundle jar from the above to /usr/lib/spark/jars
* The variantview-cell repo has some hadoop configuration files under the `variant-cell/cfg`(core-site.xml, yarn-site.xml and launch-properties.flat). 
Update these config files for your system. The driverip and masterip should point to the EMR cluster master IP. 
Workspace should point to the S3 bucket prefix where the genomicsDB array resides, and 
mongoip should point to the IP/address of the mongoDB instance. Now add the following to your .bash_profile:
```{bash}
export HADOOP_CONF_DIR=<path-to-variantview-cell>/cfg
```
* This portion requires Apache Ant to be built. Build the plugin BE by issuing the following command within the repo. 
Note: i2b2 uses the wildfly application server. Make sure to stop wildfly application server before building and 
restart it after the build completes: `ant -f master_build.xml build-all`.
* Get and install the sequence ontology. This offers a way to describe genomic concepts using i2b2’s ontology format
* Download the Genomics Import Package for users from https://community.i2b2.org/wiki/display/GIT/Home. 
Extract sequenceOntologyData.zip and unzip the file. 
Apache Ant build can then be used to install and create the appropriate tables to describe the genomics ontology. 
Please ask your local i2b2 experts for details on how to install the ontology – an example script follows:
```
# bug fix! looks like the sql files this uses has a bug
# it tries to insert into dbo.<table_name> which doesn't work
# for our postgres install. Go through and remove all instances
for sqlfile in $INSTALL_DIR/data/demo/scripts/postgresql/*.sql; do
    echo "Fixing up $sqlfile by removing all instances of dbo."
    sed -i 's/INSERT INTO dbo\./INSERT INTO /' $sqlfile
done
cd $INSTALL_DIR/data
echo "db.type=postgresql" > db.properties
echo "db.username=i2b2metadata" >> db.properties
echo "db.password=demouser" >> db.properties
echo "db.driver=org.postgresql.Driver" >> db.properties
echo "db.url=jdbc:postgresql://localhost:5432/i2b2metadata" >> db.properties
echo "db.project=demo" >> db.properties

echo "Creating genomics tables"
ant -f data_build.xml create_genomics_metadata_table
echo "Loading genomics metadata"
ant -f data_build.xml load_genomics_metadata2

echo "db.type=postgresql" > db.properties
echo "db.username=i2b2demodata" >> db.properties
echo "db.password=demouser" >> db.properties
echo "db.driver=org.postgresql.Driver" >> db.properties
echo "db.url=jdbc:postgresql://localhost:5432/i2b2demodata" >> db.properties
echo "db.project=demo" >> db.properties

echo "Loading genomics data"
ant -f data_build.xml load_genomics_data
•	We update the sequence ontology slightly in order to support using dbSNP/rsids as inputs. Example SQL with the changes follow. Please amend according to your i2b2 install.

INSERT INTO i2b2metadata.SEQUENCE_ONTOLOGY(C_HLEVEL, C_FULLNAME, C_NAME, C_SYNONYM_CD, C_VISUALATTRIBUTES, C_TOTALNUM, C_BASECODE, C_METADATAXML, C_FACTTABLECOLUMN, C_TABLENAME, C_COLUMNNAME, C_COLUMNDATATYPE, C_OPERATOR, C_DIMCODE, C_COMMENT, C_TOOLTIP, UPDATE_DATE, DOWNLOAD_DATE, IMPORT_DATE, SOURCESYSTEM_CD, VALUETYPE_CD, m_applied_path, m_exclusion_cd, C_PATH, C_SYMBOL)
VALUES(1, '\RSIDSymbol\', 'DBSNP ID', 'N', 'RA', NULL, 'SEQ:RSID', '<?xml version="1.0"?><ValueMetadata><Version>3.03</Version><CreationDateTime>04/01/2013 09:53:45</CreationDateTime><TestID>DBSNP</TestID><TestName>DBSNP ID</TestName><Help><ButtonName>ID Assist</ButtonName><URL>https://www.ncbi.nlm.nih.gov/projects/SNP/</URL></Help><DataType>String</DataType><Oktousevalues></Oktousevalues><MaxStringLength>20</MaxStringLength><EnumValues></EnumValues><UnitValues><NormalUnits>Base Pair</NormalUnits></UnitValues></ValueMetadata>', 'modifier_cd', 'modifier_dimension', 'modifier_path', 'T', 'LIKE', '\RSIDSymbol\', NULL, 'DBSNP ID', NULL, NULL, NULL, 'Sequence Ontology', NULL, '\ckie\kq2i\%', NULL, '(NULL)', '(NULL)');
INSERT INTO i2b2metadata.SEQUENCE_ONTOLOGY(C_HLEVEL, C_FULLNAME, C_NAME, C_SYNONYM_CD, C_VISUALATTRIBUTES, C_TOTALNUM, C_BASECODE, C_METADATAXML, C_FACTTABLECOLUMN, C_TABLENAME, C_COLUMNNAME, C_COLUMNDATATYPE, C_OPERATOR, C_DIMCODE, C_COMMENT, C_TOOLTIP, UPDATE_DATE, DOWNLOAD_DATE, IMPORT_DATE, SOURCESYSTEM_CD, VALUETYPE_CD, m_applied_path, m_exclusion_cd, C_PATH, C_SYMBOL)
VALUES(1, '\RSIDSymbol\', 'DBSNP ID', 'N', 'RA', NULL, 'SEQ:RSID', '<?xml version="1.0"?><ValueMetadata><Version>3.03</Version><CreationDateTime>04/01/2013 09:53:45</CreationDateTime><TestID>DBSNP</TestID><TestName>DBSNP ID</TestName><Help><ButtonName>ID Assist</ButtonName><URL>https://www.ncbi.nlm.nih.gov/projects/SNP/</URL></Help><DataType>String</DataType><Oktousevalues></Oktousevalues><MaxStringLength>10</MaxStringLength><EnumValues></EnumValues><UnitValues><NormalUnits>Base Pair</NormalUnits></UnitValues></ValueMetadata>', 'modifier_cd', 'modifier_dimension', 'modifier_path', 'T', 'LIKE', '\RSIDSymbol\', NULL, 'DBSNP ID', NULL, NULL, NULL, 'Sequence Ontology', NULL, '\ckie\kq2i\', 'X', NULL, NULL);
INSERT INTO i2b2metadata.SEQUENCE_ONTOLOGY(C_HLEVEL, C_FULLNAME, C_NAME, C_SYNONYM_CD, C_VISUALATTRIBUTES, C_TOTALNUM, C_BASECODE, C_METADATAXML, C_FACTTABLECOLUMN, C_TABLENAME, C_COLUMNNAME, C_COLUMNDATATYPE, C_OPERATOR, C_DIMCODE, C_COMMENT, C_TOOLTIP, UPDATE_DATE, DOWNLOAD_DATE, IMPORT_DATE, SOURCESYSTEM_CD, VALUETYPE_CD, m_applied_path, m_exclusion_cd, C_PATH, C_SYMBOL)
VALUES(1, '\RSIDSymbol\', 'DBSNP ID', 'N', 'RA', NULL, 'SEQ:RSID', NULL, 'modifier_cd', 'modifier_dimension', 'modifier_path', 'T', 'LIKE', '\RSIDSymbol\', NULL, NULL, '20130130 17:45:51', NULL, NULL, 'lcp5', NULL, '\ckie\kq2i\refgen\', 'X', NULL, NULL);

INSERT INTO i2b2demodata.modifier_dimension(MODIFIER_PATH, MODIFIER_CD, NAME_CHAR, MODIFIER_BLOB, UPDATE_DATE, DOWNLOAD_DATE, IMPORT_DATE, SOURCESYSTEM_CD, UPLOAD_ID)
VALUES('\RSIDSymbol\', 'SEQ:RSID', 'DBSNP ID', NULL, '20121022 16:07:34', NULL, NULL, NULL, NULL);
```
* Insert into the i2b2pm database to recognize the plugin service. An example follows:
```{bash}
psql -d i2b2pm -c "insert into i2b2pm.pm_cell_data (cell_id, project_path, name, method_cd, url, can_override, status_cd) values ('VARVIEW', '/', 'Variant viewer', 'REST', 'http://localhost:9090/i2b2/services/VariantService/', 1, 'A');"
```

That’s it! Logging onto the webclient should now show the plugin under ‘Analysis tools’

### Uninstall 

Steps to uninstall the plugin are basically the inverse of ones taken to install it. 
* Start by modifying <path/to/webclient>/js-i2b2/i2b2_loader.js and remove the block associated with VARVIEW. 
It should look something like:
```{python}
[{ code:"VARVIEW",
 forceloading: true,
 forceConfigMsg: { params: [] },
 roles: [ "DATA_LDS", "DATA_DEID", "DATA_PROT" ],
 forceDir: "cells/plugins/community"},
 ...
 ```
* Next, remove the Variantview jar and aar files from jboss deployment. Specifically, remove (exact paths may vary according to your install):
```
<jbosshome>/standalone/deployments/i2b2.war/WEB-INF/lib/Variantview-core.jar
<jbosshome>/standalone/deployments/i2b2.war/WEB-INF/services/Variantview.aar
```
* Remove the entry for VARVIEW in i2b2pm.pm_cell_data. That should remove VARVIEW from i2b2. 
