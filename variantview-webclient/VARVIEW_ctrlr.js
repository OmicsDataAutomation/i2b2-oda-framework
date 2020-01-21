/**
 * Based on example tutorial plugin by Nick Benik, Griffin Weber MD PhD
 */

i2b2.VARVIEW.Init = function(loadedDiv) {
	// register DIV as valid DragDrop target for Patient Record Sets (PRS) objects
	var op_trgt = {dropTarget:true};
	i2b2.sdx.Master.AttachType("VARVIEW-PRSDROP", "PRS", op_trgt);
	i2b2.sdx.Master.AttachType("VARVIEW-CLINDROP", "CONCPT", op_trgt);
        // drop event handlers used by this plugin
	i2b2.sdx.Master.setHandlerCustom("VARVIEW-PRSDROP", "PRS", "DropHandler", i2b2.VARVIEW.prsDropped);
	i2b2.sdx.Master.setHandlerCustom("VARVIEW-CLINDROP", "CONCPT", "DropHandler", i2b2.VARVIEW.clinicalDropped);

	// setup model variables for this plugin
	i2b2.VARVIEW.model.requestString = '';
	i2b2.VARVIEW.model.resulttable = '';
	i2b2.VARVIEW.model.responseXML = '';
	i2b2.VARVIEW.model.responseString = '';
        i2b2.VARVIEW.model.highestConcDDIndex = 0;
        i2b2.VARVIEW.model.conceptRecords = [];
        i2b2.VARVIEW.model.clinRecords = [];
        i2b2.VARVIEW.createNewCONCDDField();
        i2b2.VARVIEW.model.genotypeFilter = '';
        i2b2.VARVIEW.model.allele_counts = null;
        i2b2.VARVIEW.model.categories = [];
        i2b2.VARVIEW.model.result_data = null;
        i2b2.VARVIEW.model.analysis_type = null;

        // manage YUI tabs
	this.yuiTabs = new YAHOO.widget.TabView("VARVIEW-TABS", {activeIndex:0});
};

i2b2.VARVIEW.Unload = function() {
    // purge old data
        i2b2.VARVIEW.model.prsRecord = false;
        i2b2.VARVIEW.model.dirtyResultsData = true;
	i2b2.VARVIEW.model = {};
	i2b2.VARVIEW.model.requestString = '';
	i2b2.VARVIEW.model.resulttable = '';
	i2b2.VARVIEW.model.responseXML = '';
	i2b2.VARVIEW.model.responseString = '';
        i2b2.VARVIEW.model.highestConcDDIndex = 0;
        i2b2.VARVIEW.model.conceptRecords = [];
        i2b2.VARVIEW.model.clinRecords = [];
        i2b2.VARVIEW.model.genotypeFilter = '';
        i2b2.VARVIEW.model.allele_counts = null;
        i2b2.VARVIEW.model.result_data = null;
        i2b2.VARVIEW.model.analysis_type = null;
	try { i2b2.VARVIEW.yuiPanel.destroy(); } catch(e) {}
        return true;

};


// Helper function: It creates & registers a new drag&drop field for a concept
// Borrowed heavily from GIRI plugin for I2B2
i2b2.VARVIEW.createNewCONCDDField = function() {
     // Increment highest field counter
     var ind = ++i2b2.VARVIEW.model.highestConcDDIndex;
     // Get handles and create a new visible field by cloning the prototype
     var concFieldProt = $("VARVIEW-CONCPTDROP-PROT");
     var concFieldContainer = $("varview-droptrgt-conc-fields");
     var newNode = concFieldProt.cloneNode(true);
     newNode.className = "varview-droptrgt SDX-CONCEPT";
     newNode.id = "VARVIEW-CONCPTDROP-" + ind;
     newNode.innerHTML = "Concept " + (ind);
     concFieldContainer.appendChild(newNode);
     Element.show(newNode);
     // Register as drag&drop target
     i2b2.sdx.Master._sysData["VARVIEW-CONCPTDROP-" + ind] = {}; // hack to get an old dd field unregistered as there's no function for it...
     var op_trgt = {dropTarget:true};
     i2b2.sdx.Master.AttachType("VARVIEW-CONCPTDROP-" + ind, "CONCPT", op_trgt);
     i2b2.sdx.Master.setHandlerCustom("VARVIEW-CONCPTDROP-" + ind, "CONCPT", "DropHandler", i2b2.VARVIEW.conceptDropped);
};

i2b2.VARVIEW.conceptDropped = function(sdxData, droppedOnID) {
	// Check if something was dropped on the lowest field (=field with highest id). If yes create a new field under it
	var sdxConcept = i2b2.sdx.TypeControllers.CONCPT.MakeObject(sdxData[0].origData.xmlOrig, sdxData[0].origData.isModifier, null, sdxData[0].origData.parent, sdxData[0].sdxInfo.sdxType);
        var fieldIndex = parseInt(droppedOnID.slice(19,20));
	if (i2b2.VARVIEW.model.highestConcDDIndex == fieldIndex) {
	     // Timeout to prevent a browser error that would occur when a new dd field is created too fast here
	     // The error is harmless -> so this pseudo-fix is sufficient
	     window.setTimeout(i2b2.VARVIEW.createNewCONCDDField,200);
	}
        //sdxData = sdxData[0];	// only interested in first record
	// Check for lab / modifier value, open popup etc. (see function)
        i2b2.VARVIEW.bringPopup(sdxConcept, fieldIndex);
	// save the info to our local data model
	i2b2.VARVIEW.model.conceptRecords[fieldIndex] = sdxConcept;

	// temporarly change background color to give GUI feedback of a successful drop occuring
	$("VARVIEW-CONCPTDROP-" + fieldIndex).innerHTML = i2b2.VARVIEW.buildConceptDisplayName(fieldIndex);
	$("VARVIEW-CONCPTDROP-" + fieldIndex).style.background = "#CFB";
	// optimization to prevent requerying the hive for new results if the input dataset has not changed
	i2b2.VARVIEW.model.dirtyResultsData = true;		
};


// This function brings a popup if a (lab) value or a modifier concept was dropped
i2b2.VARVIEW.bringPopup = function(sdxData, fieldIndex) {
     // Currently not supported to define modifier values via a popup
     // well somewhat supported I guess...this needs more testing etc. TODO
/*
     if (!sdxData.origData.isModifier) {
          alert("Caution: Modifiers are only partly supported. It is not possible to define modifier values in this version.");		
          return;
     } else {
*/
          // This code is from Timeline_ctrlr.js. It checks if values should be specified in a popupta[0]
          var cdetails = i2b2.ONT.ajax.GetTermInfo("CRC:QueryTool", {concept_key_value:sdxData.origData.key, ont_synonym_records: true, ont_hidden_records: true} );
          var c = i2b2.h.XPath(cdetails.refXML, 'descendant::concept');
          if (c.length > 0) {
               sdxData.origData.xmlOrig = c[0];					
          }
          var lvMetaDatas1 = i2b2.h.XPath(sdxData.origData.xmlOrig, 'metadataxml/ValueMetadata[string-length(Version)>0]');
          if (lvMetaDatas1.length > 0) {
               // Bring up popup
               i2b2.VARVIEW.view.modalLabValues.show(this, sdxData.origData.key, sdxData, false, fieldIndex);			
          } else {
               // No values available
               return;
          }
//     }
};

i2b2.VARVIEW.prsDropped = function(sdxData) {
	sdxData = sdxData[0];	// only interested in first record
	// save the info to our local data model
	i2b2.VARVIEW.model.prsRecord = sdxData;
	// let the user know that the drop was successful by displaying the name of the patient set
	$("VARVIEW-PRSDROP").innerHTML = i2b2.h.Escape(sdxData.sdxInfo.sdxDisplayName);
	// temporarly change background color to give GUI feedback of a successful drop occuring
	$("VARVIEW-PRSDROP").style.background = "#CFB";
	setTimeout("$('VARVIEW-PRSDROP').style.background='#DEEBEF'", 250);	
	// optimization to prevent requerying the hive for new results if the input dataset has not changed
	i2b2.VARVIEW.model.dirtyResultsData = true;		
};

i2b2.VARVIEW.format = function (str, col) {
  col = typeof col === 'object' ? col : Array.prototype.slice.call(arguments, 1);
  return str.replace(/\{\{|\}\}|\{(\w+)\}/g, function (m, n) {
    if ("{{" == m) { return "{"; }
    if ("}}" == m) { return "}"; }
    return col[n];
  });
}

i2b2.VARVIEW.clinicalDropped = function(sdxData) {
  var nameD, n, newConcept, nameC;
  sdxData = sdxData[0];                       // Consider first record only
  if (sdxData.origData.isModifier) {
    alert("Modifier item being dropped in is not supported.");
    return false;
  }
  nameD = sdxData.sdxInfo.sdxDisplayName;         // Save to local data model
  n = i2b2.VARVIEW.model.clinRecords.length;
  if (n) {
    newConcept = true;
    if (19 < n) {
      alert("20 concepts has already been selected (no more concept would be accepted)!!");
      newConcept = false;
    } else {
      for (var i = 0; i < n; i ++) {
        nameC = i2b2.VARVIEW.model.clinRecords[i].sdxInfo.sdxDisplayName;
        if (nameC == nameD)     {
          alert("This concept has already been selected (duplicate concept would not be accepted)!!");
          newConcept = false;
          break;
        }
      }
    }
    if(newConcept) {
      i2b2.VARVIEW.ConceptAdd(sdxData);
    }
  } else {
    i2b2.VARVIEW.ConceptAdd(sdxData);
  }
//  i2b2.CAREcncptDem.CheckUpdateCohortSizeUsageMeg(); // in case user skips to drag-drop concpts after setting startPatNum
}

i2b2.VARVIEW.ConceptAdd = function(sdxData) {
  i2b2.VARVIEW.model.clinRecords.push(sdxData);
  i2b2.VARVIEW.clinicalRender();                           // Sort and display concepts
}

i2b2.VARVIEW.clinicalRender = function() {
  var i, s = '', conDiv = '<div class="clinicalDiv"></div>';
  var newCon = '<a class="clinicalItem">{1}</a>';
  if (i2b2.VARVIEW.model.clinRecords.length) { // If there are any concepts in the list
    i2b2.VARVIEW.model.clinRecords.sort (    // Sort the concepts in alphabetical order
        function() { return arguments[0].sdxInfo.sdxDisplayName > arguments[1].sdxInfo.sdxDisplayName }
        );
    for (i = 0; i < i2b2.VARVIEW.model.clinRecords.length; i ++) { // List the concepts
      if (i > 0) { s += conDiv; }
      s += i2b2.VARVIEW.format(newCon, i, i2b2.h.Escape(i2b2.h.HideBreak(i2b2.VARVIEW.model.clinRecords[i].sdxInfo.sdxDisplayName)));
    }
  } else { // No concepts selected yet
    s = '<div class="clinicalItem">Drop one or more Concepts here</div>';
  }
  $("VARVIEW-CLINDROP").innerHTML = s; // Update html
} // end of ConceptsRender()

i2b2.VARVIEW.callVARVIEW = function() {
	//remove old results
	i2b2.VARVIEW.model.responseXML = '';
	i2b2.VARVIEW.model.responseString = '';
        var e = document.getElementById("analysis_select");
        i2b2.VARVIEW.model.analysis_type =  e.options[e.selectedIndex].value;
	$$("DIV#VARVIEW-mainDiv DIV#VARVIEW-TABS DIV.results-error")[0].hide();
        $$("DIV#VARVIEW-mainDiv DIV#VARVIEW-TABS DIV.results-directions")[0].hide();
	$$("DIV#VARVIEW-mainDiv DIV#VARVIEW-TABS DIV.results-finished")[0].hide();
	
	// show wait instructions
	$$("DIV#VARVIEW-mainDiv DIV#VARVIEW-TABS DIV.results-progress")[0].show();		
	this.yuiTabs.selectTab(1);

        // Start of debug section...
/*
        //var t = i2b2.VARVIEW.model.conceptRecords[1].origData.xmlOrig;
        var t = i2b2.VARVIEW.model.conceptRecords[1].origData.parent.xmlOrig;
        var debug_string = "level:" + i2b2.h.getXNodeVal(t, "level") + "\n";
        debug_string += "fullname:" + i2b2.h.getXNodeVal(t, "fullname") + "\n";
        debug_string += "name:" + i2b2.h.getXNodeVal(t, "name")+ "\n";
        debug_string += "synonym_cd" + ":" + i2b2.h.getXNodeVal(t, "synonym_cd")+ "\n";
        debug_string += "visualattributes" + ":" + i2b2.h.getXNodeVal(t, "visualattributes")+ "\n";
        debug_string += "totalnum" + ":" + i2b2.h.getXNodeVal(t, "totalnum")+ "\n";
        debug_string += "basecode" + ":" + i2b2.h.getXNodeVal(t, "basecode")+ "\n";
        debug_string += "metadataxml" + ":" + i2b2.h.getXNodeVal(t, "metadataxml")+ "\n";
        debug_string += "facttablecolumn" + ":" + i2b2.h.getXNodeVal(t, "facttablecolumn")+ "\n";
        debug_string += "tablename" + ":" + i2b2.h.getXNodeVal(t, "tablename")+ "\n";
        debug_string += "columnname" + ":" + i2b2.h.getXNodeVal(t, "columnname")+ "\n";
        debug_string += "columndatatype" + ":" + i2b2.h.getXNodeVal(t, "columndatatype")+ "\n";
        debug_string += "operator" + ":" + i2b2.h.getXNodeVal(t, "operator")+ "\n";
        debug_string += "dimcode" + ":" + i2b2.h.getXNodeVal(t, "dimcode")+ "\n";
        debug_string += "valuetype_cd" + ":" + i2b2.h.getXNodeVal(t, "valuetype_cd")+ "\n";
        debug_string += "applied_path" + ":" + i2b2.h.getXNodeVal(t, "applied_path")+ "\n";
        debug_string += "exclusion_cd" + ":" + i2b2.h.getXNodeVal(t, "exclusion_cd")+ "\n";
        debug_string += "path" + ":" + i2b2.h.getXNodeVal(t, "path")+ "\n";
        debug_string += "symbol" + ":" + i2b2.h.getXNodeVal(t, "symbol")+ "\n";
        debug_string += "alternate val:" + i2b2.VARVIEW.buildConceptDisplayName(1) + "\n";

        $('varview-debug-string').innerHTML = debug_string;
*/
        // End of debug section...

	// give a brief pause for the GUI to catch up
	setTimeout('i2b2.VARVIEW.getResults();', 50);
};

i2b2.VARVIEW.getModValue = function(index) {
     var sdxData = i2b2.VARVIEW.model.conceptRecords[index];
     var lvd = sdxData.LabValues;
     var retval = "lvd undefined\n";
     if ( ! Object.isUndefined(lvd) ) {
          switch(lvd.MatchBy) {
               case "FLAG": retval = '<value>' + lvd.ValueFlag + '</value>\n'; break;
               case "VALUE": 
                    if (lvd.GeneralValueType=="LARGESTRING" || lvd.GeneralValueType=="TEXT" || lvd.GeneralValueType=="STRING") {
                         retval = '<value>' + lvd.ValueString + '</value>\n';
                    }
                    else if (lvd.GeneralValueType=="ENUM") {
                         try {
                              var sEnum = [];
                              for (var i2=0;i2<lvd.ValueEnum.length;i2++) {
                                   sEnum.push(i2b2.h.Escape(lvd.ValueEnum[i2]));
                              }
                              sEnum = sEnum.join("\", \"");
                              sEnum = ' ("'+sEnum+'")';
                              retval = '<enumval>' + sEnum + '</enumval>\n';
                         } catch (e) {
                         
                         }
                    } else {
                         if (lvd.NumericOp == 'BETWEEN') {
                              retval = '<lowval>' + lvd.ValueLow + '</lowval>\n' + 
                                       '<highval>' + lvd.ValueHigh + '</highval>\n';
                         } else {
                              switch(lvd.NumericOp) {
                                   case "LT": var numericOp = '<'; break;
                                   case "LE": var numericOp = '<='; break;
                                   case "EQ": var numericOp = '='; break;
                                   case "GT": var numericOp = '>'; break;
                                   case "GE": var numericOp = '>='; break;
                                   
                                   case "": break;
                              }
                              retval = '<comparator>' + numericOp + '</comparator>\n' +
                                       '<comparator_val>' + lvd.Value + '</comparator_val>\n';
                         }
                    }
                    break;
            }
     }
     //return i2b2.h.Escape(retval);
     return retval;
};

// Below function is used to generate display name for 
// concepts dropped in as plugin input. Leveraged some code from
// CRC_ctrlr_QryPanel.js
i2b2.VARVIEW.buildConceptDisplayName = function(index) {
     var sdxData = i2b2.VARVIEW.model.conceptRecords[index];
     if ( ! Object.isUndefined(sdxData.LabValues) ) {
          var lvd = sdxData.LabValues;
          // we could go through in a while loop and iterate till we
          // find a parent that isn't a modifier. Assuming below
          // that just going one level up to parents is good enough
          // for our sequence ontology. 
          var _name = sdxData.origData.parent.name;
          switch(lvd.MatchBy) {
               case "FLAG":
                    _name += ' [' + sdxData.sdxInfo.sdxDisplayName + ' = ' + i2b2.h.Escape(lvd.ValueFlag) + ']';
                    break;
               case "VALUE":
                    _name += ' [' + sdxData.sdxInfo.sdxDisplayName;
                    if (lvd.GeneralValueType=="LARGESTRING" || lvd.GeneralValueType=="TEXT" || lvd.GeneralValueType=="STRING") {
                         _name += ' = ' + i2b2.h.Escape(lvd.ValueString);
                    }
                    else if (lvd.GeneralValueType=="ENUM") {
                         try {
                              var sEnum = [];
                              for (var i2=0;i2<lvd.ValueEnum.length;i2++) {
                                   sEnum.push(i2b2.h.Escape(lvd.ValueEnum[i2]));
                              }
                              sEnum = sEnum.join("\", \"");
                              sEnum = ' =  ("'+sEnum+'")';
                              _name += sEnum;
                         } catch (e) {
                         
                         }
                    } else {
                         if (lvd.NumericOp == 'BETWEEN') {
                              _name +=  ' '+i2b2.h.Escape(lvd.ValueLow)+' - '+i2b2.h.Escape(lvd.ValueHigh);
                         } else {
                              switch(lvd.NumericOp) {
                                   case "LT": var numericOp = " < "; break;
                                   case "LE": var numericOp = " <= "; break;
                                   case "EQ": var numericOp = " = "; break;
                                   case "GT": var numericOp = " > "; break;
                                   case "GE": var numericOp = " >= "; break;
                                   
                                   case "": break;
                              }
                              _name += numericOp +i2b2.h.Escape(lvd.Value);
                              if (!Object.isUndefined(lvd.UnitsCtrl)) {
                                   _name += " " + lvd.UnitsCtrl;
                              }
                        }
                    }
                    _name += ']';
                    break;

               case "": _name = sdxData.sdxInfo.sdxDisplayName + ' unspecified'; break;
          }
          return _name;
     }
     else {
          return sdxData.origData.parent.name + ' [' + sdxData.sdxInfo.sdxDisplayName + ' undefined]';
     }
};

i2b2.VARVIEW.buildConceptsForQuery = function() {
     var concepts = '';
     for (var i=1; i<i2b2.VARVIEW.model.conceptRecords.length; i++) {
          var t = i2b2.VARVIEW.model.conceptRecords[i].origData.xmlOrig;
          var parent = i2b2.VARVIEW.model.conceptRecords[i].origData.parent;
          concepts += '<concept>\n' +
                      '     <parent_variant_name>'+i2b2.h.Escape(parent.name)+'</parent_variant_name>\n' +
                      '     <name>'+i2b2.h.Escape(i2b2.h.getXNodeVal(t, "name"))+'</name>\n' +
                      '     <concept_value>\n'+i2b2.VARVIEW.getModValue(i)+'     </concept_value>\n' +
                      '</concept>\n';
     }
     return concepts;
};

i2b2.VARVIEW.buildClinicalConcepts = function() {
  var clinrecords = '';
  for (var i=0; i<i2b2.VARVIEW.model.clinRecords.length; i++){
    var cr = i2b2.VARVIEW.model.clinRecords[i].origData;
    clinrecords += '<panel name="'+cr.name+'">\n<panel_number>0</panel_number>\n<panel_accuracy_scale>0</panel_accuracy_scale>\n<invert>0</invert>\n<item>\n<hlevel>1</hlevel>\n' +
                  ' <item_key>'+cr.key+'</item_key>\n'+
                  ' <dim_tablename>'+cr.table_name+'</dim_tablename>\n'+
                  ' <dim_dimcode>'+cr.dim_code+'</dim_dimcode>\n<item_is_synonym>N</item_is_synonym>\n'+
                  '</item></panel>\n';
  }
    return clinrecords
}

i2b2.VARVIEW.getResults = function() {

        var msg_filter = {};
        if(i2b2.VARVIEW.model.prsRecord != null){
          msg_filter['patient_sets'] = '			    <patient_set_coll_id>' + i2b2.VARVIEW.model.prsRecord.sdxInfo.sdxKeyValue + '</patient_set_coll_id>\n';
        }
        msg_filter['qts_url'] = i2b2.h.Escape(i2b2['CRC'].cfg.cellURL);
        msg_filter['concepts'] = i2b2.VARVIEW.buildConceptsForQuery();
        msg_filter['filter_list'] = i2b2.VARVIEW.buildClinicalConcepts();
        msg_filter['analysis'] = '<analysis>\n'+i2b2.VARVIEW.model.analysis_type+'</analysis>\n';
        msg_filter['genotype_filter'] = $$("DIV#varview-genotype-filter")[0].select('TEXTAREA')[0].value;
	// callback processor
	var scopedCallback = new i2b2_scopedCallback();
	scopedCallback.scope = this;
	scopedCallback.callback = function(results) {
		// check for errors
		if (results.error) {
			//alert('The results from the server could not be understood.  Press F12 for more information.');
	  	  $$("DIV#VARVIEW-mainDiv DIV#VARVIEW-TABS DIV.results-progress")[0].hide();	
                  $$("DIV#VARVIEW-mainDiv DIV#VARVIEW-TABS DIV.results-error")[0].show();
                  i2b2.VARVIEW.model.result_data = results
                  var errorReport = i2b2.VARVIEW.model.result_data.refXML.getElementsByTagName('status')[0].innerHTML;
                  $$("DIV#VARVIEW-mainDiv DIV#VARVIEW-TABS DIV.results-error")[0].innerHTML = '<pre>'+ errorReport + '</pre>';
                  return false;
		}
                results.parse();

		// extract data from response message and update model
                i2b2.VARVIEW.model.dirtyResultsData = false;
                i2b2.VARVIEW.model.result_data = results.model
                document.getElementById("region_specific_options").innerHTML = "";
                document.getElementById("position_specific_options").innerHTML = "";
                document.getElementById("plot_specific_options").innerHTML = "";
                 try{
                  var region_select = document.createElement("select");
                  region_select.id = "regionSelector";
                  for(var i = 0; i<i2b2.VARVIEW.model.result_data.concept_data[0].json_rep.length;i++){
                    var option = document.createElement("option");
                    option.text = i2b2.VARVIEW.model.result_data.concept_data[0].json_rep[i]._id.name
                    option.value = i
                    region_select.add(option) 
                  }
                  region_select.on("change", function(){
                    document.getElementById("position_specific_options").innerHTML = "";
                    document.getElementById("plot_specific_options").innerHTML = "";
                    i2b2.VARVIEW.loadSelectedPlot(this.value, i2b2.VARVIEW.model.analysis_type, 0, true);
                  });
                  var tableholder = document.getElementById("region_specific_options");
                  tableholder.innerHTML = '<table id="region_options"><tr><td colspan="2" style="text-align:justify">'+"Select genomic region to display:"+'</td></tr></table>';

                  var table = document.getElementById("region_options");
                  table.insertRow().insertCell().innerHTML = "<hr/>";
                  table.insertRow().insertCell().append(region_select); 
                 
                  i2b2.VARVIEW.loadSelectedPlot(document.getElementById("regionSelector")[0].value, i2b2.VARVIEW.model.analysis_type, 0, true);
 		  // update the view results tab
		  $$("DIV#VARVIEW-mainDiv DIV#VARVIEW-TABS DIV.results-progress")[0].hide();
		  $$("DIV#VARVIEW-mainDiv DIV#VARVIEW-TABS DIV.results-finished")[0].show();
               }catch(err){
                  $$("DIV#VARVIEW-mainDiv DIV#VARVIEW-TABS DIV.results-error")[0].innerHTML = "Dataset came back as null, make sure all report inputs are provided: "+err.message;
                  $$("DIV#VARVIEW-mainDiv DIV#VARVIEW-TABS DIV.results-progress")[0].hide();
                  $$("DIV#VARVIEW-mainDiv DIV#VARVIEW-TABS DIV.results-error")[0].show();
               }
        }

	// AJAX CALL USING THE EXISTING CRC CELL COMMUNICATOR
	i2b2.VARVIEW.ajax.plugin_VARVIEW_Request("Plugin:VARVIEW", msg_filter, scopedCallback);
		
}


i2b2.VARVIEW.loadSelectedPlot = function(region, load_data, concept_value, first_call, from_concept){
  from_concept = (from_concept === undefined) ? false : from_concept;
    i2b2.VARVIEW.model.allele_counts = c3.generate({
      data: {
        json: i2b2.VARVIEW.model.result_data.concept_data[0].json_rep[region].data, //i2b2.VARVIEW.model.result_data.variant_data,
        type: 'bar',
        keys: {
          x: 'Start',
          value: ['RefAlleleCount', 'AltAlleleCount']
        },
        groups: [["RefAlleleCount", "AltAlleleCount"]]
      },
      size: {
        height: 650,
        width: 1000
      },
      axis: {
        x: {
        type: 'index', // this is needed to load string x value,
        }
      },
      zoom: {
        enabled: true
      },
      tooltip: {
        contents: function (d, defaultTitleFormat, defaultValueFormat, color) {
          var $$ = this, config = $$.config,
          titleFormat = config.tooltip_format_title || defaultTitleFormat,
          nameFormat = config.tooltip_format_name || function (name) { return name; },
          valueFormat = config.tooltip_format_value || defaultValueFormat,
          text, i, title, value, name, bgcolor;
          for (i = 0; i < d.length; i++) {
            if (! (d[i] && (d[i].value || d[i].value === 0))) { continue; }

            if (! text) {
              title =  "rs"+i2b2.VARVIEW.model.result_data.concept_data[0].json_rep[region].data[d[i].index].snp_id +
                        "</br>"+i2b2.VARVIEW.model.result_data.concept_data[0].json_rep[region].data[d[i].index].Contig+" : "+d[i].x;
              text = "<table class='" + $$.CLASS.tooltip + "'>" + "<tr><th colspan='2'>" + title + "</th></tr>";
            }

            name = nameFormat(d[i].name);
            value = valueFormat(d[i].value, d[i].ratio, d[i].id, d[i].index);
            bgcolor = $$.levelColor ? $$.levelColor(d[i].value) : color(d[i].id);

            text += "<tr class='" + $$.CLASS.tooltipName + "-" + d[i].id + "'>";
            text += "<td class='name'><span style='background-color:" + bgcolor + "'></span>" + name + "</td>";
            text += "<td class='value'>" + value + "</td>";
            text += "</tr>";
          }
          return text + "</table>";
        }
      },
      bindto: '#variantVis'
    }); 
  //}
}


i2b2.VARVIEW.updateChart = function(data_type){
  var groups = []
  var change_type = data_type
  if(data_type === "stacked-area-spline" || data_type === "stacked-bar"){
    change_type = data_type.slice(8)
    if (i2b2.VARVIEW.model.allele_counts.data().length == 2){
      groups = ["RefAlleleCount", "AltAlleleCount"]
    }else{
      groups = ['homref', 'het', 'homalt', 'nocall']
    }
    i2b2.VARVIEW.model.allele_counts.groups([groups])
  }else{
    i2b2.VARVIEW.model.allele_counts.groups([])
  }
  i2b2.VARVIEW.model.allele_counts.transform(change_type);
}
