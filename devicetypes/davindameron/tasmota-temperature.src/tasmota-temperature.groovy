metadata {
	//Based on work by Brett Sheleski for Tasomota-Power

	definition(name: "Tasmota Temperature", namespace: "davindameron", author: "Davin Dameron", ocfDeviceType: "oic.d.smartplug") {
		capability "Polling"
		capability "Contact Sensor"
		capability "Sensor"
		capability "Temperature Measurement"
		capability "Execute"

        
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
	main "temperature"
		details(["temperature"])
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
				sendEvent(name: "temperature", value: json."DS18B20"."Temperature");
			}						
			if (json."StatusSNS"){
				sendEvent(name: "temperature", value: json."StatusSNS"."DS18B20"."Temperature");
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

def poll() {
	doLogging "POLL"
}

def ping() {
	doLogging("ping()")
}
