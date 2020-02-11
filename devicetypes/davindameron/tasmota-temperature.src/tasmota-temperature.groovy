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

			if (json."DS18B20"){
				setTemperature(json."DS18B20"."Temperature");
			}						
			if (json."StatusSNS"){
				setTemperature(json."StatusSNS"."DS18B20"."Temperature");
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
