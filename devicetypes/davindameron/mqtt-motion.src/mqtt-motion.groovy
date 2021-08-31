import groovy.transform.Field

metadata {
	//Based on work by Brett Sheleski for Tasomota-Power

	definition(name: "MQTT Motion", namespace: "davindameron", author: "Davin Dameron", ocfDeviceType: "x.com.st.d.sensor.motion", mnmn:"SmartThings", vid:"SmartThings-smartthings-SmartSense_Motion") {
		capability "Motion Sensor"
		capability "Temperature Measurement"
		capability "Sensor"
		capability "Execute"
		capability "Battery"

	}

	tiles(scale: 2) {
		multiAttributeTile(name: "motion", type: "generic", width: 6, height: 4) {
			tileAttribute("device.motion", key: "PRIMARY_CONTROL") {
				attributeState "active", label: 'motion', icon: "st.motion.motion.active", backgroundColor: "#00A0DC"
				attributeState "inactive", label: 'no motion', icon: "st.motion.motion.inactive", backgroundColor: "#cccccc"
			}
		}
		valueTile("temperature", "device.temperature", width: 2, height: 2) {
			state("temperature", label: '${currentValue}°', unit: "F",
					backgroundColors: [
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
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
			state "battery", label: '${currentValue}% battery', unit: ""
		}
		main(["motion", "temperature"])
		details(["motion", "temperature", "battery"])
	}    
	preferences {
        
	input(name: "MQTTProxy", type: "string", title: "MQTT Proxy Web Server", description: "MQTT Proxy Web Server", displayDuringSetup: true, required: false)
	input(name: "MQTTTopic", type: "string", title: "MQTT Topic", description: "MQTT Topic", displayDuringSetup: true, required: false)
	input(name: "debugLogging", type: "boolean", title: "Turn on debug logging?", displayDuringSetup:true, required: false)
	}
}


def execute(String command){
	doLogging "execute($command)";
	
		if (command) {
			def json = new groovy.json.JsonSlurper().parseText(command);
			if (json) {
				doLogging("execute: Values received: ${json}")
				if (json."occupancy"!=null) {
					if (json."occupancy"==true) {
						sendEvent(name: "motion", value: "active")
					}
					if (json."occupancy"==false) {
						sendEvent(name: "motion", value: "inactive")
					}
				}
				if (json."battery"!=null) {
					sendEvent(name: "battery", value: json."battery")
				}
				if (json."temperature"!=null) {
					def fTemp as Double
					fTemp =  json."temperature"
					fTemp = fTemp * 9/5
					fTemp = fTemp + 32

					setTemperature(fTemp)
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

def setTemperature(value) {
    def map = [:]
    map.name = "temperature"
    map.value = value
    map.unit = "F"
    doLogging "Temperature Report: $map.value"
    doLogging "Temperature Scale: $map.unit"

    sendEvent(map)
}

def doLogging(value){
	def debugLogging = debugLogging ?: settings?.debugLogging ?: device.latestValue("debugLogging");
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
}

def reload(){
	doLogging "reload()"
}

def poll() {
	doLogging "POLL"
}


def sendCommand(String command, callback) {
    return sendCommand(command, null);
}

def sendCommand(String command, payload, callback) {
	sendHubCommand(createCommand(command, payload, callback))
}

def createCommand(String command, payload, callback){
		def dni = null;
		def path="/?topic=zigbee2mqtt/${settings.MQTTTopic}/${command}&payload=${payload}"
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


def ping() {
	doLogging "ping()"
}
