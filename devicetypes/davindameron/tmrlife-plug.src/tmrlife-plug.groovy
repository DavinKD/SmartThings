metadata {
	//Based on work by Brett Sheleski for Tasomota-Power

	definition(name: "TMRLife Plug", namespace: "davindameron", author: "Davin Dameron", ocfDeviceType: "oic.d.smartplug") {
		capability "Polling"
		capability "Refresh"
		capability "Switch"

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
        input(name: "PowerChannel", type: "integer", title: "Power Channel (1-8)", description: "Power Channel of the Relay", displayDuringSetup: true, required: true)
        input(name: "PowerChannelRed", type: "integer", title: "Red LED Channel (1-8)", description: "Power Channel of the Red LED", displayDuringSetup: true, required: true)
        input(name: "PowerChannelBlue", type: "integer", title: "Blue LED Channel (1-8)", description: "Power Channel of the Blue LED", displayDuringSetup: true, required: true)
	input(name: "turnOnRed", type: "boolean", title: "Turn on Red Light with Switch?", displayDuringSetup: true, required: false)
        input(name: "turnOnBlue", type: "boolean", title: "Turn on Blue Light with Switch?", displayDuringSetup: true, required: false)
	input(name: "debugLogging", type: "boolean", title: "Turn on debug logging?", displayDuringSetup:true, required: false)

		section("Sonoff Host") {
			
		}

		section("Authentication") {
			input(name: "username", type: "string", title: "Username", description: "Username", displayDuringSetup: false, required: false)
			input(name: "password", type: "password", title: "Password (sent cleartext)", description: "Caution: password is sent cleartext", displayDuringSetup: false, required: false)
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
	doLogging("installed()");
    reload();
}

def updated(){
	doLogging("updated()");
    reload();
	runEvery5Minutes(refresh)
}

def reload(){
	doLogging("reload()");
    refresh();
}


def poll() {
	doLogging("POLL");
	sendCommand("Status", "0", refreshCallback)
}

def refresh() {
	doLogging("refresh()");
	sendCommand("Status", "0", refreshCallback)
}


def refreshCallback(physicalgraph.device.HubResponse response){
	doLogging("refreshCallback()");
    def jsobj = response?.json;

    doLogging("JSON: ${jsobj}");
    updateStatus(jsobj);

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
    log.debug path;

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
	def commandName = "Power${PowerChannel}";
	def payload = power;

	doLogging("COMMAND: $commandName ($payload)");

	def command = createCommand(commandName, payload, "setPowerCallback");;

    	sendHubCommand(command);
}

def setPowerCallback(physicalgraph.device.HubResponse response){
	
	
	doLogging("Finished Setting power (channel: 1), JSON: ${response.json}");

	def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");
   	def on = response.json."POWER${PowerChannel}" == "ON";
	//Sometimes channel 1 will just say POWER, not POWER1
	if(PowerChannel==1){
		on = on || response.json."POWER" == "ON";
	}
    setSwitchState(on);
}

def setPowerRed(power){
	doLogging("Setting power2 to: $power");

	def PowerChannel = PowerChannelRed ?: settings?.PowerChannelRed ?: device.latestValue("PowerChannelRed");
	def commandName = "Power${PowerChannel}";
	def payload = power;

	doLogging("COMMAND: $commandName ($payload)");

	def command = createCommand(commandName, payload, "setPowerRedCallback");;

    	sendHubCommand(command);
}

def setPowerRedCallback(physicalgraph.device.HubResponse response){
	doLogging("Finished Setting power (channel: 2), JSON: ${response.json}");

	def PowerChannel = PowerChannelRed ?: settings?.PowerChannelRed ?: device.latestValue("PowerChannelRed");
   	def on = response.json."POWER${PowerChannel}" == "ON";
}

def setPowerBlue(power){
	doLogging("Setting power3 to: $power");

	def PowerChannel = PowerChannelBlue ?: settings?.PowerChannelBlue ?: device.latestValue("PowerChannelBlue");
	def commandName = "Power${PowerChannel}";
	def payload = power;

	doLogging("COMMAND: $commandName ($payload)");

	def command = createCommand(commandName, payload, "setPowerBlueCallback");;

    	sendHubCommand(command);
}

def setPowerBlueCallback(physicalgraph.device.HubResponse response){
	doLogging("Finished Setting power (channel: 3), JSON: ${response.json}");

	def PowerChannel = PowerChannelBlue ?: settings?.PowerChannelBlue ?: device.latestValue("PowerChannelBlue");
	def on = response.json."POWER${PowerChannel}" == "ON";
}

def updateStatus(status){

	//refresh();
	// Device power status(es) are reported back by the Status.Power property
	// The Status.Power property contains the on/off state of all channels (in case of a Sonoff 4CH or Sonoff Dual)
	// This is binary-encoded where each bit represents the on/off state of a particular channel
	// EG: 7 in binary is 0111.  In this case channels 1, 2, and 3 are ON and channel 4 is OFF

	def powerMask = 0b0001;

	def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");

	powerMask = powerMask << ("$PowerChannel".toInteger() - 1); // shift the bits over 

	def on = (powerMask & status.Status.Power);

    setSwitchState(on);
}

def setSwitchState(on){
	doLogging("Setting switch to ${on ? 'ON' : 'OFF'}");

	sendEvent(name: "switch", value: on ? "on" : "off", displayed: true);
    if (on==true)
    {
	if (turnOnRed=="true")
		{setPowerRed("on")
	    }
    }

    if (on==true)
    {
	if (turnOnBlue=="true")
		{setPowerBlue("on")
	    }
	}    

	if (!on)
    {
	setPowerRed("off");
	setPowerBlue("off");
    }
}

def ping() {
	doLogging("ping()")
	return refresh()
}
