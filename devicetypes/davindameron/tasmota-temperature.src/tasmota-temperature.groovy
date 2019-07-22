metadata {
	//Based on work by Brett Sheleski for Tasomota-Power

	definition(name: "Tasmota Temperature", namespace: "davindameron", author: "Davin Dameron", ocfDeviceType: "oic.d.smartplug") {
		capability "Polling"
		capability "Temperature Measurement"
		capability "Execute"

        command "reload"
        command "updateStatus"
        
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
		input(name: "useMQTT", type: "boolean", title: "Use MQTT for Updates?", displayDuringSetup: true, required: false)
		input(name: "debugLogging", type: "boolean", title: "Turn on debug logging?", displayDuringSetup:true, required: false)
	}
}

def execute(String command){
	
	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT=="true"){
		doLogging "execute($command)";
		if (command) {
			def json = new groovy.json.JsonSlurper().parseText(command);
			if (json) {
				doLogging("execute: Values received: ${json}")
				if (json."StatusSTS"){
					json = json."StatusSTS"
				}
				def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");
				
				def on = false
				def gotPowerState = false

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
    reload();
}

def updated(){
	doLogging "updated()"
	reload();
	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		runEvery5Minutes(refresh)
	}
}

def reload(){
	doLogging "reload()"
	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		refresh();
	}
}

def poll() {
	doLogging "POLL"
	sendCommand("Status", "0", refreshCallback)
}

def refresh() {
	doLogging "refresh()"
}


def refreshCallback(physicalgraph.device.HubResponse response){
	doLogging "refreshCallback()"
	def jsobj = response?.json;

	doLogging "JSON: ${jsobj}";
	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		updateStatus(jsobj);
	}

}



def updateStatus(status){

	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		sendEvent(name: "temperature", value: status.StatusSNS.DS18B20.Temperature);
	}
}

def ping() {
	doLogging("ping()")
	return refresh()
}