metadata {
	definition(name: "Tasmota Pressure", namespace: "davindameron", author: "Davin Dameron", mnmn: "SmartThingsCommunity", vid: "42735677-564a-3a0c-be7d-9baa9836ee70", ocfDeviceType: "oic.d.thermostat") {
		capability "venturecircle58707.pumppressure"
		capability "Execute"
        	capability "Health Check"
	}

	// UI tile definitions
	tiles(scale: 2) {

        valueTile("pressure", "device.pumpPressure", width: 2, height: 2) {
            state("pressure", label:'${currentValue}', unit:"psi")
	}
	main "pressure"
		details(["pressure"])
	}

    
	preferences {
		input(name: "debugLogging", type: "boolean", title: "Turn on debug logging?", displayDuringSetup:true, required: false)
		input(name: "useDev", type: "boolean", title: "Use Dev Versions for Upgrade?", displayDuringSetup: true, required: false)
		input(name: "doUpgrade", type: "boolean", title: "Perform Upgrade?", displayDuringSetup: true, required: false)
		input(name: "SensorName", type: "string", title: "Sensor Name", description: "Sensor Name", displayDuringSetup: true, required: false)
		input(name: "MQTTProxy", type: "string", title: "MQTT Proxy Web Server", description: "MQTT Proxy Web Server", displayDuringSetup: true, required: false)
		input(name: "MQTTTopic", type: "string", title: "MQTT Topic", description: "MQTT Topic", displayDuringSetup: true, required: false)
	}
}
def sendCommand(String command, callback) {
    return sendCommand(command, null);
}

def sendCommand(String command, payload, callback) {
	sendHubCommand(createCommand(command, payload, callback))
}

def createCommand(String command, payload, callback){
	def dni = null;
	def path="/?topic=cmnd/${settings.MQTTTopic}/${command}&payload=${payload}"
	doLogging(path);

	def params = [
	method: "GET",
	path: path,
	headers: [
	    HOST: "${settings.MQTTProxy}:80"
	]
	]
	doLogging(params);

	def options = [
	callback : callback
	];

	def hubAction = new physicalgraph.device.HubAction(params, dni, options);
}



def execute(String command){
	
	doLogging "execute($command)";
	if (command) {
		def json = new groovy.json.JsonSlurper().parseText(command);
		if (json) {
			doLogging("execute: Values received: ${json}")
			if (json."StatusSTS"){
				json = json."StatusSTS"
			}
			if (json."Mult6"){
				setPressure(json."Mult6")
			}

			if (json."${settings.SensorName}"){
				//setTemperature(json."${settings.SensorName}"."Temperature");
				//setDate();
			}						
			if (json."StatusSNS"){
				if (json."StatusSNS"."${settings.SensorName}"){
					//setTemperature(json."StatusSNS"."${settings.SensorName}"."Temperature");
					//setDate();
				}
			}
			if (json."Wifi"){
				doLogging("execute: got WIFI")
				def ss = json."Wifi"."RSSI";
				sendEvent(name: "lqi", value: ss);

				def rssi = json."Wifi"."Signal";
				sendEvent(name: "rssi", value: rssi);
			}						
		}
		else {
			doLogging("execute: No json received: ${command}")
		}
	}
	else {
		doLogging("execute: No command received")
	}
	
}


def doLogging(value){
	if (debugLogging=="true")
	{
		log.debug value;
	}
}

def installed(){
	doLogging "installed()"
	runEvery3Hours(setDate)
}

def updated(){
	doLogging "updated()"
	//if (doUpgrade=="true"){
	//	doLogging "doUpgrade is true"
		//setOTAURL()
		//doUpgrade()
		//device.updateSetting("doUpgrade", false)
		//settings[doUpgrade]="false"
	//}
	//setOption57(1)
}

def setOption56(value){
	sendCommand("setOption56", value, setOption56Callback);
}

def setOption56Callback(physicalgraph.device.HubResponse response){
	doLogging "setOption56Callback(${response})"
	def jsobj = response?.json;

	doLogging "JSON: ${jsobj}";
}

def setOption57(value){
	sendCommand("setOption57", value, setOption56Callback);
}

def setOption57Callback(physicalgraph.device.HubResponse response){
	doLogging "setOption57Callback(${response})"
	def jsobj = response?.json;

	doLogging "JSON: ${jsobj}";
}


def setOTAURL(){
	if (useDev=="true"){
		sendCommand("OtaUrl", "http://192.168.0.40/tasmota.bin", setOTAURLCallback);
	}
	else {
		sendCommand("OtaUrl", "http://thehackbox.org/tasmota/release/tasmota.bin", setOTAURLCallback);
	}
}

def setOTAURLCallback(physicalgraph.device.HubResponse response){
	doLogging "setOTAURLCallback(${response})"
}

def doUpgrade(){
	sendCommand("Upgrade", "1", doUpgradeCallback)
}

def doUpgradeCallback(physicalgraph.device.HubResponse response){
	doUpgradeCallback "doUpgradeCallback(${response})"
}

def ping() {
	doLogging("ping()")
	return "true"
}
def setDate(){
	doLogging "setDate"
	def timeString = new Date().format("MMM dd", location.timeZone).replace(" ", "%20")
	setVar9(timeString)
}

def setPressure(value) {
	doLogging "setting pressure"
	pressure=value
}
