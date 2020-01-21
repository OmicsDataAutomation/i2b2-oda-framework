// this file contains a list of all files that need to be loaded dynamically for this i2b2 Cell
// every file in this list will be loaded after the cell's Init function is called
{
	files:[
		"VARVIEW_ctrlr.js",
		"VARVIEW_modLabRange.js",
		"i2b2_msgs.js"
	],
	css:[ 
		"vwVARVIEW.css"
	],
	config: {
		// additional configuration variables that are set by the system
		short_name: "Variant viewer",
		name: "Variant viewer",
		description: "This plugin helps genomics data queries and cohort creation.",
		category: ["plugin"],
		plugin: {
			isolateHtml: false,  // this means do not use an IFRAME
			isolateComm: true,  // this means to expect the plugin to use AJAX communications provided by the framework
			standardTabs: true, // this means the plugin uses standard tabs at top
			html: {
				source: 'injected_screens.html',
				mainDivId: 'VARVIEW-mainDiv'
			}
		}
	}
}
