metadata {
	definition(name: "Tasmota Pressure", namespace: "davindameron", author: "Davin Dameron", mnmn: "SmartThingsCommunity", vid: "a5e58915-f37a-3adc-9727-f0c6f3e41714") {
		capability "venturecircle58707.filterpumppressure"
		capability "Signal Strength"
		capability "Execute"
        	capability "Health Check"
	}

	// UI tile definitions
	tiles(scale: 2) {

        valueTile("pressure", "device.filterpumppressure", width: 2, height: 2) {
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
			if (json."Sub10"){
				setPressure(json."Sub10")
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

def setPressure(value) {
	doLogging "setting pressure"
    def map = [:]
    map.name = "pressure"
    def x = value as Double
    Math.round(x * 100) / 100
    map.value = x
    map.unit = "psi"

    sendEvent(map)

	//sendEvent(name: "pressure", value: value)
}
