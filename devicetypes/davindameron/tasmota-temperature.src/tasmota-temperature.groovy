metadata {
	definition(name: "Tasmota Temperature", namespace: "davindameron", author: "Davin Dameron", mnmn: "SmartThings", vid: "generic-temperature-measurement", ocfDeviceType: "oic.d.thermostat") {
		capability "Temperature Measurement"
		capability "Execute"
		capability "Signal Strength"
		capability "Sensor"
        	capability "Health Check"
		capability "Contact Sensor"
	}

	// UI tile definitions
	tiles(scale: 2) {

        valueTile("temperature", "device.temperature", width: 2, height: 2) {
            state("temperature", label:'${currentValue}', unit:"F",
                backgroundColors:[
                    [value: 31, color: "#153591"],
                    [value: 44, color: "#1e9cbb"],
                    [value: 59, color: "#90d2a7"],
                    [value: 74, color: "#44b621"],
                    [value: 84, color: "#f1d801"],
                    [value: 95, color: "#d04e00"],
                    [value: 96, color: "#bc2323"]
                ]
            )
        }
	valueTile("lqi", "device.lqi", decoration: "flat", width: 2, height: 2) {
		state "default", label: 'Signal Strength ${currentValue}%'
	}
	main "temperature"
		details(["temperature", "lqi"])
	}

    
	preferences {
		input(name: "debugLogging", type: "boolean", title: "Turn on debug logging?", displayDuringSetup:true, required: false)
		input(name: "useDev", type: "boolean", title: "Use Dev Versions for Upgrade?", displayDuringSetup: true, required: false)
		input(name: "doUpgrade", type: "boolean", title: "Perform Upgrade?", displayDuringSetup: true, required: false)
		input(name: "SensorName", type: "string", title: "Sensor Name", description: "Sensor Name", displayDuringSetup: true, required: false)
	}
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

			if (json."${settings.SensorName}"){
				setTemperature(json."${settings.SensorName}"."Temperature");
			}						
			if (json."StatusSNS"){
				if (json."StatusSNS"."${settings.SensorName}"){
					setTemperature(json."StatusSNS"."${settings.SensorName}"."Temperature");
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
}

def updated(){
	doLogging "updated()"
	if (doUpgrade=="true"){
		doLogging "doUpgrade is true"
		setOTAURL()
		doUpgrade()
		device.updateSetting("doUpgrade", false)
		//settings[doUpgrade]="false"
	}
	setOption57(1)
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

def setTemperature(value) {
    def map = [:]
    map.name = "temperature"
    map.value = value
    map.unit = "F"
    doLogging "Temperature Report: $map.value"
    doLogging "Temperature Scale: $map.unit"

    sendEvent(map)
}
