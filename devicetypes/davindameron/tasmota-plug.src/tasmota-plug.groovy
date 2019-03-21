metadata {
	//Based on work by Brett Sheleski for Tasomota-Power

	definition(name: "Tasmota Plug", namespace: "davindameron", author: "Davin Dameron", ocfDeviceType: "oic.d.smartplug") {
		capability "Polling"
		capability "Refresh"
		capability "Switch"
		capability "Execute"

        command "reload"
        command "updateStatus"
        command "ringpush"
        
        attribute "ringpush", "string"
	}

	// UI tile definitions
	tiles(scale: 2) {

	standardTile("switch", "device.switch", decoration: "flat", width: 3, height: 3, canChangeIcon: true) {
	    state "off", label:'${name}', action: "switch.on", icon: "st.switches.switch.on", backgroundColor:"#ffffff"
	    state "on", label:'${name}', action: "switch.off", icon: "st.switches.switch.off", backgroundColor:"#00a0dc"
	}        
	
	standardTile("refresh", "device.switch", width: 3, height: 3, inactiveLabel: false, decoration: "flat") {
			state "default", label:'Refresh', action:"refresh", icon:"st.secondary.refresh"
		}

	main "switch"
		details(["switch", "refresh"])
	}

    
	preferences {
		input(name: "ipAddress", type: "string", title: "IP Address", description: "IP Address of Sonoff", displayDuringSetup: true, required: true)
		input(name: "PowerChannel", type: "number", title: "Power Channel (1-8)", description: "Power Channel of the Relay", displayDuringSetup: true, required: true)
		input(name: "PowerChannelLed1", type: "number", title: "LED 1 Channel (1-8)", description: "Power Channel of LED 1", displayDuringSetup: true, required: true)
		input(name: "PowerChannelLed2", type: "number", title: "LED 2 Channel (1-8)", description: "Power Channel of LED 2", displayDuringSetup: true, required: true)
		input(name: "PowerChannelLed3", type: "number", title: "LED 3 Channel (1-8)", description: "Power Channel of LED 3", displayDuringSetup: true, required: true)
		input(name: "turnOnLed1", type: "boolean", title: "Turn on LED 1 Light with Switch?", displayDuringSetup: true, required: false)
		input(name: "turnOnLed2", type: "boolean", title: "Turn on LED 2 Light with Switch?", displayDuringSetup: true, required: false)
		input(name: "turnOnLed3", type: "boolean", title: "Turn on LED 3 Light with Switch?", displayDuringSetup: true, required: false)
		input(name: "useMQTT", type: "boolean", title: "Use MQTT for Updates?", displayDuringSetup: true, required: false)
		input(name: "debugLogging", type: "boolean", title: "Turn on debug logging?", displayDuringSetup:true, required: false)
		input(name: "username", type: "string", title: "Username", description: "Username", displayDuringSetup: false, required: false)
		input(name: "password", type: "password", title: "Password (sent cleartext)", description: "Caution: password is sent cleartext", displayDuringSetup: false, required: false)
	}
}

def execute(String command){
	doLogging "execute($command)";
	
	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT=="true"){
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

				if (json."POWER${PowerChannel}"!=null) {
					doLogging("execute: got power channel")
					on = json."POWER${PowerChannel}" == "ON";
					doLogging("execute: setting switch state")
					setSwitchState(on);
					gotPowerState = true
				}
				if(PowerChannel==1) {
					if (json."POWER"!=null) {
						doLogging("execute: got power channel")
						on = json."POWER" == "ON";
						doLogging("execute: setting switch state")
						setSwitchState(on);
						gotPowerState = true
					}
				}
				
				if (gotPowerState) {
					if (on) {
						def led1On = false
						def led2On = false
						def led3On = false
						def didRefresh = false
						if (turnOnLed1=="true") {
							if (json."POWER${PowerChannelLed1}"!=null) {
								led1On = json."POWER${PowerChannelLed1}" == "ON"
								if (led1On) {
									//Do Nothing
								}
								else
								{
									setPowerLed("on", PowerChannelLed1)
								}
							}
							else {
								//We didn't get this, so we need to do a status
								didRefresh = true
								refresh()
							}
						}
						if (turnOnLed2=="true") {
							if (json."POWER${PowerChannelLed2}"!=null) {
								led2On = json."POWER${PowerChannelLed2}" == "ON"
								if (led2On) {
									//Do Nothing
								}
								else
								{
									setPowerLed("on", PowerChannelLed2)
								}
							}
							else {
								//We didn't get this, so we need to do a status
								if (didRefresh==false) {
									didRefresh = true
									refresh()
								}
							}
						}
						if (turnOnLed3=="true") {
							if (json."POWER${PowerChannelLed3}"!=null) {
								led3On = json."POWER${PowerChannelLed3}" == "ON"
								if (led3On) {
									//Do Nothing
								}
								else
								{
									setPowerLed("on", PowerChannelLed3)
								}
							}
							else {
								//We didn't get this, so we need to do a status
								if (didRefresh==false) {
									didRefresh = true
									refresh()
								}
							}
						}
					}
					else {
						//off
						def led1On = false
						def led2On = false
						def led3On = false
						def didRefresh = false
						if (turnOnLed1=="true") {
							if (json."POWER${PowerChannelLed1}"!=null) {
								led1On = json."POWER${PowerChannelLed1}" == "ON"
								if (led1On) {
									setPowerLed("off", PowerChannelLed1)
								}
								else
								{
									//Do Nothing
								}
							}
							else {
								//We didn't get this, so we need to do a status
								didRefresh = true
								refresh()
							}
						}
						if (turnOnLed2=="true") {
							if (json."POWER${PowerChannelLed2}"!=null) {
								led2On = json."POWER${PowerChannelLed2}" == "ON"
								if (led2On) {
									setPowerLed("off", PowerChannelLed2)
								}
								else
								{
									//Do Nothing
								}
							}
							else {
								//We didn't get this, so we need to do a status
								if (didRefresh==false) {
									didRefresh = true
									refresh()
								}
							}
						}
						if (turnOnLed3=="true") {
							if (json."POWER${PowerChannelLed3}"!=null) {
								led3On = json."POWER${PowerChannelLed3}" == "ON"
								if (led3On) {
									setPowerLed("off", PowerChannelLed3)
								}
								else
								{
									//Do Nothing
								}
							}
							else {
								//We didn't get this, so we need to do a status
								if (didRefresh==false) {
									didRefresh = true
									refresh()
								}
							}
						}
					}
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
	sendCommand("Status", "0", refreshCallback)
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


def sendCommand(String command, callback) {
    return sendCommand(command, null);
}

def sendCommand(String command, payload, callback) {
	sendHubCommand(createCommand(command, payload, callback))
}

def createCommand(String command, payload, callback){

    def ipAddress = ipAddress ?: settings?.ipAddress ?: device.latestValue("ipAddress");
    def username = username ?: settings?.username ?: device.latestValue("username");
    def password = password ?: settings?.password ?: device.latestValue("password");

    doLogging("createCommandAction(${command}:${payload}) to device at ${ipAddress}:80");

	if (!ipAddress) {
		doLogging("aborting. ip address of device not set");
		return null;
	}

	def path = "/cm"
	if (payload){
		path += "?cmnd=${command}%20${payload}"
	}
	else{
		path += "?cmnd=${command}"
	}

	if (username){
		path += "&user=${username}"
		if (password){
			path += "&password=${password}"
		}
	}

    def dni = null;
    doLogging(path);

    def params = [
        method: "GET",
        path: path,
        headers: [
            HOST: "${ipAddress}:80"
        ]
    ]

    def options = [
        callback : callback
    ];

	def hubAction = new physicalgraph.device.HubAction(params, dni, options);
}

def on(){
    setPower("on")
}

def off(){
    setPower("off")
}

def setPower(power){
	doLogging("Setting power to: $power");

	def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");
	
	def doBacklog = false
	def backlogValue = "Backlog%20Power${PowerChannel}%20${power}"
	if (turnOnLed1=="true") {
		doBacklog = true
		backlogValue += "%20Power${PowerChannelLed1}%20${power}"
	}
	if (turnOnLed2=="true") {
		doBacklog = true
		backlogValue += "%20Power${PowerChannelLed2}%20${power}"
	}
	if (turnOnLed3=="true") {
		doBacklog = true
		backlogValue += "%20Power${PowerChannelLed3}%20${power}"
	}
	if (doBacklog) {
		def commandName = backlogValue;
		def payload = "";
	}
	else {
		def commandName = "Power${PowerChannel}";
		def payload = power;
	}
	doLogging("COMMAND: $commandName ($payload)");

	def command = createCommand(commandName, payload, "setPowerCallback");;

    	sendHubCommand(command);
}

def setPowerCallback(physicalgraph.device.HubResponse response){
	def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");
	doLogging "Finished Setting power (channel: ${PowerChannel}), JSON: ${response.json}"
	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		def on = response.json."POWER${PowerChannel}" == "ON";
		if(PowerChannel==1){
			on = on || response.json."POWER" == "ON";
		}
		setSwitchState(on);
	}}

def setPowerLed(power, ledChannel){
	doLogging("Setting power${ledChannel} to: $power");

	def commandName = "Power${ledChannel}";
	def payload = power;

	doLogging("COMMAND: $commandName ($payload)");

	def command = createCommand(commandName, payload, "setPowerLedCallback");;

    	sendHubCommand(command);
}

def setPowerLedCallback(physicalgraph.device.HubResponse response){
	doLogging("Finished Setting power (channel: 2), JSON: ${response.json}");

	def PowerChannel = PowerChannelRed ?: settings?.PowerChannelRed ?: device.latestValue("PowerChannelRed");
   	def on = response.json."POWER${PowerChannel}" == "ON";
}

def updateStatus(status){

	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		def on = false
		def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");
		def gotPowerState = false
		on = status.StatusSTS."POWER${PowerChannel}" == "ON";
		if(PowerChannel==1){
			on = on || status.StatusSTS."POWER" == "ON";
			gotPowerState = true
		}
		setSwitchState(on);
		def json = status.StatusSTS
		if (gotPowerState) {
			if (on) {
				def led1On = false
				def led2On = false
				def led3On = false
				if (turnOnLed1=="true") {
					if (json."POWER${PowerChannelLed1}"!=null) {
						led1On = json."POWER${PowerChannelLed1}" == "ON"
						if (led1On) {
							//Do Nothing
						}
						else
						{
							setPowerLed("on", PowerChannelLed1)
						}
					}
				}
				if (turnOnLed2=="true") {
					if (json."POWER${PowerChannelLed2}"!=null) {
						led2On = json."POWER${PowerChannelLed2}" == "ON"
						if (led2On) {
							//Do Nothing
						}
						else
						{
							setPowerLed("on", PowerChannelLed2)
						}
					}
				}
				if (turnOnLed3=="true") {
					if (json."POWER${PowerChannelLed3}"!=null) {
						led3On = json."POWER${PowerChannelLed3}" == "ON"
						if (led3On) {
							//Do Nothing
						}
						else
						{
							setPowerLed("on", PowerChannelLed3)
						}
					}
				}
			}
			else {
				//off
				def led1On = false
				def led2On = false
				def led3On = false
				if (turnOnLed1=="true") {
					if (json."POWER${PowerChannelLed1}"!=null) {
						led1On = json."POWER${PowerChannelLed1}" == "ON"
						if (led1On) {
							setPowerLed("off", PowerChannelLed1)
						}
						else
						{
							//Do Nothing
						}
					}
				}
				if (turnOnLed2=="true") {
					if (json."POWER${PowerChannelLed2}"!=null) {
						led2On = json."POWER${PowerChannelLed2}" == "ON"
						if (led2On) {
							setPowerLed("off", PowerChannelLed2)
						}
						else
						{
							//Do Nothing
						}
					}
				}
				if (turnOnLed3=="true") {
					if (json."POWER${PowerChannelLed3}"!=null) {
						led3On = json."POWER${PowerChannelLed3}" == "ON"
						if (led3On) {
							setPowerLed("off", PowerChannelLed3)
						}
						else
						{
							//Do Nothing
						}
					}
				}
			}
		}
	
	}
}

def setSwitchState(on){
	doLogging("Setting switch to ${on ? 'ON' : 'OFF'}");

	sendEvent(name: "switch", value: on ? "on" : "off", displayed: true);
}

def ping() {
	doLogging("ping()")
	return refresh()
}
