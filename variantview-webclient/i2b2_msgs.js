/**
 * @projectDescription	Messages used by the VARVIEW cell communicator object.
 * @inherits 	i2b2.VARVIEW.cfg
 * @namespace	i2b2.VARVIEW.cfg.msgs
 * @version 	1.3
 * ----------------------------------------------------------------------------------------
 */


// create the communicator Object
i2b2.VARVIEW.ajax = i2b2.hive.communicatorFactory("VARVIEW");
i2b2.VARVIEW.cfg.msgs = {};
i2b2.VARVIEW.cfg.parsers = {};

// ================================================================================================== //
i2b2.VARVIEW.cfg.msgs.plugin_VARVIEW_Request = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\r'+
'<ns6:request xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/"\r'+
'  xmlns:ns7="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/"\r'+
'  xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/pdo/1.1/"\r'+
'  xmlns:ns5="http://www.i2b2.org/xsd/hive/plugin/"\r'+
'  xmlns:ns2="http://www.i2b2.org/xsd/hive/pdo/1.1/"\r'+
'  xmlns:ns8="http://www.i2b2.org/xsd/cell/varview/1.0/"\r'+
'  xmlns:ns6="http://www.i2b2.org/xsd/hive/msg/1.1/">\r'+
'	<message_header>\n'+
'		{{{proxy_info}}}'+
'		<sending_application>\n'+
'			<application_name>i2b2_QueryTool</application_name>\n'+
'			<application_version>0.2</application_version>\n'+
'		</sending_application>\n'+
'		<sending_facility>\n'+
'			<facility_name>PHS</facility_name>\n'+
'		</sending_facility>\n'+
'		<receiving_application>\n'+
'			<application_name>i2b2_DataRepositoryCell</application_name>\n'+
'			<application_version>0.2</application_version>\n'+
'		</receiving_application>\n'+
'		<receiving_facility>\n'+
'			<facility_name>PHS</facility_name>\n'+
'		</receiving_facility>\n'+
'		<message_type>\n'+
'			<message_code>Q04</message_code>\n'+
'			<event_type>EQQ</event_type>\n'+
'		</message_type>\n'+
'		<security>\n'+
'			<domain>{{{sec_domain}}}</domain>\n'+
'			<username>{{{sec_user}}}</username>\n'+
'			{{{sec_pass_node}}}\n'+
'		</security>\n'+
'		<message_control_id>\n'+
'			<message_num>{{{header_msg_id}}}</message_num>\n'+
'			<instance_num>0</instance_num>\n'+
'		</message_control_id>\n'+
'		<processing_id>\n'+
'			<processing_id>P</processing_id>\n'+
'			<processing_mode>I</processing_mode>\n'+
'		</processing_id>\n'+
'		<accept_acknowledgement_type>messageId</accept_acknowledgement_type>\n'+
'		<project_id>{{{sec_project}}}</project_id>\n'+
'	</message_header>\n'+
'	<request_header>\n'+
'		<result_waittime_ms>{{{result_wait_time}}}000</result_waittime_ms>\n'+
'	</request_header>\n'+
'	<message_body>\n'+
'		<ns8:VARRequest>\n'+
'			<QTSUrl>{{{qts_url}}}</QTSUrl>\n'+
'			<patientSets>{{{patient_sets}}}</patientSets>\n'+
'			<Concepts>{{{concepts}}}</Concepts>\n'+
'                       <filter_list>{{{filter_list}}}</filter_list>\n'+
'                       <Analyses>{{{analysis}}}</Analyses>\n'+
'                       <genotype_filter>{{{genotype_filter}}}</genotype_filter>\n'+
'		</ns8:VARRequest>\n'+
'	</message_body>\n'+
'</ns6:request>';
i2b2.VARVIEW.cfg.parsers.plugin_VARVIEW_Request = function() {
	if (!this.error) {
		this.model = {
                        concept_data: [],
			patients: [],
			events: [],
			observations: []
		};		
		// extract event records
		var ps = this.refXML.getElementsByTagName('event');
		for(var i1=0; i1<ps.length; i1++) {
			var o = new Object;
			o.xmlOrig = ps[i1];
			o.event_id = i2b2.h.getXNodeVal(ps[i1],'event_id');
			o.patient_id = i2b2.h.getXNodeVal(ps[i1],'patient_id');
			o.start_date = i2b2.h.getXNodeVal(ps[i1],'start_date');
			o.end_date = i2b2.h.getXNodeVal(ps[i1],'end_date');
			// need to process param columns 
			//o. = i2b2.h.getXNodeVal(ps[i1],'');
			this.model.events.push(o);
		}
		// extract observation records
		var ps = this.refXML.getElementsByTagName('observation');
		for(var i1=0; i1<ps.length; i1++) {
			var o = new Object;
			o.xmlOrig = ps[i1];
			o.event_id = i2b2.h.getXNodeVal(ps[i1],'event_id');
			o.patient_id = i2b2.h.getXNodeVal(ps[i1],'patient_id');
			o.concept_cd = i2b2.h.getXNodeVal(ps[i1],'concept_cd');
			o.observer_cd = i2b2.h.getXNodeVal(ps[i1],'observer_cd');
			o.start_date = i2b2.h.getXNodeVal(ps[i1],'start_date');
			o.modifier_cd = i2b2.h.getXNodeVal(ps[i1],'modifier_cd');
			o.tval_char = i2b2.h.getXNodeVal(ps[i1],'tval_char');
			o.nval_num = i2b2.h.getXNodeVal(ps[i1],'nval_num');
			o.valueflag_cd = i2b2.h.getXNodeVal(ps[i1],'valueflag_cd');
			o.units_cd = i2b2.h.getXNodeVal(ps[i1],'units_cd');
			o.end_date = i2b2.h.getXNodeVal(ps[i1],'end_date');
			o.location_cd = i2b2.h.getXNodeVal(ps[i1],'location_cd');
			this.model.observations.push(o);
		}
		var ps = this.refXML.getElementsByTagName('patient');
		for(var i1=0; i1<ps.length; i1++) {
			var o = new Object;
			o.xmlOrig = ps[i1];
			o.patient_id = i2b2.h.getXNodeVal(ps[i1],'patient_id');
			var params = i2b2.h.XPath(ps[i1], 'descendant::param[@column]/text()/..');
			for (var i2 = 0; i2 < params.length; i2++) {
				var name = params[i2].getAttribute("column");
				o[name] = params[i2].firstChild.nodeValue;
			}		
			this.model.patients.push(o);
		}

                // this is an example of how we can pass mongo data without casting into java object
                // somewhat of a hack, but avoids a lot of unnecessary data transforms to pass
                // to the plotting function
                var cs = this.refXML.getElementsByTagName('mongo_object');
                var cs_o = new Object;
                if(cs.length > 0){
                  cs_o.xmlOrig = cs[0];
                  cs_o.json_rep = JSON.parse(i2b2.h.getXNodeVal(cs[0], 'mongo_object')); 
                  this.model.concept_data.push(cs_o);
                }
	} else {
          this.model = false;
        }
	return this;
}
i2b2.VARVIEW.ajax._addFunctionCall(	"plugin_VARVIEW_Request",
								i2b2.VARVIEW.cfg.cellURL + 'getVariantData', //"http://webservices.i2b2.org/i2b2/rest/VaraintService/getVariantData",
								i2b2.VARVIEW.cfg.msgs.plugin_VARVIEW_Request,
								["patient_sets", "qts_url", "concepts", "filter_list", "analysis", "genotype_filter"],
								i2b2.VARVIEW.cfg.parsers.plugin_VARVIEW_Request);

