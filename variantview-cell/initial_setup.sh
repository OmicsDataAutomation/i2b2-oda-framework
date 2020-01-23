#!/bin/bash

# Make changes in i2b2pm database so that we recognize the service
#sudo -u postgres psql -U postgres -d i2b2pm -c "insert into i2b2pm.pm_cell_data (cell_id, project_path, name, method_cd, url, can_override, status_cd) values ('VARVIEW', '/', 'Variant viewer', 'REST', 'http://localhost:9090/i2b2/services/VariantService/', 1, 'A');"
cd com.oda.i2b2.variantview/lib
wget http://central.maven.org/maven2/org/apache/spark/spark-launcher_2.11/2.0.2/spark-launcher_2.11-2.0.2.jar
wget http://central.maven.org/maven2/org/apache/spark/spark-core_2.11/2.0.2/spark-core_2.11-2.0.2.jar
