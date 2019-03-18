metadata {
	//Based on work by Brett Sheleski for Tasomota-Power

	definition(name: "SP201 Dual Energy Monitoring Plug", namespace: "davindameron", author: "Davin Dameron", ocfDeviceType: "oic.d.smartplug", mnmn:"SmartThings", vid:"generic-switch-power-energy") {
		capability "Switch"
		capability "Polling"
		capability "Refresh"
        	capability "Power Meter"
		capability "Energy Meter"
		capability "Execute"

        	command "reload"
        	command "updateStatus"
		command "reset"
        
	}

	// UI tile definitions
	tiles(scale: 2) {

	standardTile("switch", "device.switch", decoration: "flat", width: 3, height: 3, canChangeIcon: true) {
	    	state "off", label:'${name}', action: "switch.on", icon: "st.switches.switch.on", backgroundColor:"#ffffff"
		state "on", label:'${name}', action: "switch.off", icon: "st.switches.switch.off", backgroundColor:"#00a0dc"
	}        
	valueTile("power", "device.power", decoration: "flat", width: 3, height: 3) {
		state "default", label:'${currentValue} W'
	}
	valueTile("energy", "device.energy", decoration: "flat", width: 3, height: 3) {
		state "default", label: '${currentValue} kWh'
	}
	standardTile("reset", "device.switch", inactiveLabel: false, decoration: "flat", width: 3, height: 3) {
		state "default", label: 'reset kWh', action: "reset"
	}
	standardTile("refresh", "device.switch", width: 3, height: 3, inactiveLabel: false, decoration: "flat") {
			state "default", label:'Refresh', action:"refresh", icon:"st.secondary.refresh"
		}

	main("switch")
		details(["switch", "power", "energy", "reset", "refresh"])
	}

    
    preferences {
        
        input(name: "ipAddress", type: "string", title: "IP Address", description: "IP Address of Sonoff", displayDuringSetup: true, required: true)
        input(name: "turnOnBlue", type: "boolean", title: "Turn on Blue Light with Switch?", displayDuringSetup: true, required: false)
        input(name: "usePlug2", type: "boolean", title: "Control Plug 2 (Leave off for plug 1)?", displayDuringSetup: true, required: false)
	input(name: "debugLogging", type: "boolean", title: "Turn on debug logging?", displayDuringSetup:true, required: false)

		section("Sonoff Host") {
			
		}

		section("Authentication") {
			input(name: "username", type: "string", title: "Username", description: "Username", displayDuringSetup: false, required: false)
			input(name: "password", type: "password", title: "Password (sent cleartext)", description: "Caution: password is sent cleartext", displayDuringSetup: false, required: false)
		}
	}
}

def installed(){
	doLogging("installed()");
    reload();
}

def Execute(string command, string value){
	log.debug "Command: $command";
	log.debug "Value: $value";
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

def reset() {
	doLogging("refresh()");
	sendCommand("EnergyReset3", "0", resetCallBack)
}

def resetCallBack(physicalgraph.device.HubResponse response) {
	doLogging("refreshCallback($response)");
	sendEvent(name: "energy", value: response.json.EnergyReset.Total);

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
		log.warn "aborting. ip address of device not set"
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

def pause(millis) {
   def passed = 0
   def now = new Date().time
   doLogging("pausing... at Now: $now");
   /* This loop is an impolite busywait. We need to be given a true sleep() method, please. */
   while ( passed < millis ) {
       passed = new Date().time - now
   }
   doLogging("... DONE pausing.");
}

def setPower(power){
	doLogging("Setting power to: $power");
	def commandName = "Power1";

	def usePlug2 = usePlug2 ?: settings?.usePLug2 ?: device.latestValue("usePlug2");
    doLogging("usePlug2 [${usePlug2}]");
	if (usePlug2=="true")
    {
		commandName = "Power2";
    }
	def payload = power;

	doLogging("COMMAND: $commandName ($payload)");

	def command = createCommand(commandName, payload, "setPowerCallback");;

    	sendHubCommand(command);
}

def doLogging(value){
	def debugLogging = debugLogging ?: settings?.debugLogging ?: device.latestValue("debugLogging");
	if (debugLogging=="true")
	{
		log.debug value;
	}
}

def setPowerCallback(physicalgraph.device.HubResponse response){
	def usePlug2 = usePlug2 ?: settings?.usePLug2 ?: device.latestValue("usePlug2");
	def on = response.json."POWER1" == "ON";
	if (usePlug2=="true")
	{
		doLogging("Finished Setting power (channel: 2), JSON: ${response.json}");
        on = response.json."POWER2" == "ON";
    }
    else
    {
		doLogging("Finished Setting power (channel: 1), JSON: ${response.json}");
        
        //Sometimes channel 1 will just say POWER, not POWER1
        on = on || response.json."POWER" == "ON";
   	}
    setSwitchState(on);
    pause(2000);
    refresh();
}

def setPowerBlue(power){
	doLogging("Setting power3 to: $power");

	def commandName = "Power3";
	def payload = power;

	doLogging("COMMAND: $commandName ($payload)");

	def command = createCommand(commandName, payload, "setPowerBlueCallback");;

    	sendHubCommand(command);
}

def setPowerBlueCallback(physicalgraph.device.HubResponse response){
	doLogging("Finished Setting power (channel: 3), JSON: ${response.json}");

   	def on = response.json."POWER3" == "ON";
}

def updateStatus(status){

	//refresh();
	// Device power status(es) are reported back by the Status.Power property
	// The Status.Power property contains the on/off state of all channels (in case of a Sonoff 4CH or Sonoff Dual)
	// This is binary-encoded where each bit represents the on/off state of a particular channel
	// EG: 7 in binary is 0111.  In this case channels 1, 2, and 3 are ON and channel 4 is OFF

	def powerMask = 0b0001;
    def powerChannel = 1;
	def usePlug2 = usePlug2 ?: settings?.usePLug2 ?: device.latestValue("usePlug2");
    
    if (usePlug2 == "true")
    {
    	powerChannel = 2;
    }

	powerMask = powerMask << ("$powerChannel".toInteger() - 1); // shift the bits over 

	def on = (powerMask & status.Status.Power);
    
    
    //log.debug "Watts: ${status.StatusSNS.ENERGY.Power}";
	sendEvent(name: "power", value: status.StatusSNS.ENERGY.Power);
	sendEvent(name: "energy", value: status.StatusSNS.ENERGY.Total);
    setSwitchState(on);
}

def setSwitchState(on){
	def turnOnRed = false;
    def turnOnBlue = false;
	turnOnBlue = turnOnBlue ?: settings?.turnOnBlue ?: device.latestValue("turnOnBlue");

	doLogging("Setting switch to ${on ? 'ON' : 'OFF'}");
    doLogging(turnOnBlue);
    if (on==true)

	{
    	if (turnOnBlue=="true")
        	{setPowerBlue("on")
            }
	}    

	if (!on)
    {
        setPowerBlue("off");
    }

	sendEvent(name: "switch", value: on ? "on" : "off", displayed: true);
}

def ping() {
	doLogging("ping()");
	return refresh()
}
