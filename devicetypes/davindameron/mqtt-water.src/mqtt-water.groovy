import groovy.transform.Field

metadata {
	//Based on work by Brett Sheleski for Tasomota-Power

	definition(name: "MQTT Water", namespace: "davindameron", author: "Davin Dameron", ocfDeviceType: "x.com.st.d.sensor.moisture", mnmn:"SmartThings", vid:"SmartThings-smartthings-SmartSense_Moisture_Sensor") {
		capability "Water Sensor"
		capability "Sensor"
		capability "Execute"
		capability "Battery"

        command "wet"
        command "dry"
       
	}

	tiles {
		standardTile("water", "device.water", width: 2, height: 2) {
			state "dry", icon:"st.alarm.water.dry", backgroundColor:"#ffffff", action: "wet"
			state "wet", icon:"st.alarm.water.wet", backgroundColor:"#00A0DC", action: "dry"
		}
		standardTile("wet", "device.water", inactiveLabel: false, decoration: "flat") {
			state "default", label:'Wet', action:"wet", icon: "st.alarm.water.wet"
		}         
		standardTile("dry", "device.water", inactiveLabel: false, decoration: "flat") {
			state "default", label:'Dry', action:"dry", icon: "st.alarm.water.dry"
		}  
		main "water"
		details(["water","wet","dry"])
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
				//Color Temp
				if (json."water_leak"!=null) {
					if (json."water_leak"=="true") {
						sendEvent(name: "water", value: "wet")
					}
					if (json."water_leak"=="false") {
						sendEvent(name: "water", value: "dry")
					}
					doLogging json."water_leak"
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

