metadata {
	//Based on work by Brett Sheleski for Tasomota-Power

	definition(name: "Tasmota Multi Outlet", namespace: "DavinDameron", author: "Davin Dameron", ocfDeviceType: "oic.d.smartplug") {
		capability "Polling"
		capability "Refresh"
		capability "Switch"

        	command "reload"
        	command "updateStatus"
        	command "switch1"
        	command "switch2"
        	command "switch3"
        	command "switch4"
        	command "switch5"
        	command "turnon1"
        	command "turnoff1"
        	command "turnon2"
        	command "turnoff2"
        	command "turnon3"
        	command "turnoff3"
        	command "turnon4"
        	command "turnoff4"
        	command "turnon5"
        	command "turnoff5"
        
	}

	// UI tile definitions
	tiles(scale: 2) {

		standardTile("switch", "device.switch", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
		    state "off", label:'${name}', action: "switch.on", icon: "st.switches.switch.on", backgroundColor:"#ffffff"
		    state "on", label:'${name}', action: "switch.off", icon: "st.switches.switch.off", backgroundColor:"#00a0dc"
		}        

		standardTile("switch1", "device.switch", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
		    state "off", label:'${name}', action: "turnon1", icon: "st.switches.switch.on", backgroundColor:"#ffffff"
		    state "on", label:'${name}', action: "turnoff1", icon: "st.switches.switch.off", backgroundColor:"#00a0dc"
		}        

		standardTile("switch2", "device.switch", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
		    state "off", label:'${name}', action: "turnon2", icon: "st.switches.switch.on", backgroundColor:"#ffffff"
		    state "on", label:'${name}', action: "turnoff2", icon: "st.switches.switch.off", backgroundColor:"#00a0dc"
		}        

		standardTile("switch3", "device.switch", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
		    state "off", label:'${name}', action: "turnon3", icon: "st.switches.switch.on", backgroundColor:"#ffffff"
		    state "on", label:'${name}', action: "turnoff3", icon: "st.switches.switch.off", backgroundColor:"#00a0dc"
		}        

		standardTile("switch4", "device.switch", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
		    state "off", label:'${name}', action: "turnon4", icon: "st.switches.switch.on", backgroundColor:"#ffffff"
		    state "on", label:'${name}', action: "turnoff4", icon: "st.switches.switch.off", backgroundColor:"#00a0dc"
		}        

		standardTile("switch5", "device.switch", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
		    state "off", label:'${name}', action: "turnon5", icon: "st.switches.switch.on", backgroundColor:"#ffffff"
		    state "on", label:'${name}', action: "turnoff5", icon: "st.switches.switch.off", backgroundColor:"#00a0dc"
		}        

		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
				state "default", label:'Refresh', action:"refresh", icon:"st.secondary.refresh"
		}


		main "switch"
			details(["switch", "switch1", "switch2", "switch3", "switch4", "switch5", "refresh"])
	}

	preferences {

		input(name: "ipAddress", type: "string", title: "IP Address", description: "IP Address of Sonoff", displayDuringSetup: true, required: true)
		input(name: "username", type: "string", title: "Username", description: "Username", displayDuringSetup: false, required: false)
		input(name: "password", type: "password", title: "Password (sent cleartext)", description: "Caution: password is sent cleartext", displayDuringSetup: false, required: false)
	}
}

def installed(){
	log.debug "installed()"
    reload();
}

def updated(){
	log.debug "updated()"
    reload();
	runEvery5Minutes(refresh)
}

def reload(){
	log.debug "reload()"
    refresh();
}

def poll() {
	log.debug "POLL"
	sendCommand("Status", "0", refreshCallback)
}

def refresh() {
	log.debug "refresh()"
	sendCommand("Status", "0", refreshCallback)
}


def refreshCallback(physicalgraph.device.HubResponse response){
	log.debug "refreshCallback()"
    def jsobj = response?.json;

    log.debug "JSON: ${jsobj}";
    updateStatus(jsobj);

}

def sendCommand(String command, callback) {
    return sendCommand(command, null);
}

def sendCommand(String command, payload, callback) {
	sendHubCommand(createCommand(command, payload, callback))
}

private String convertIPtoHex(ipAddress) { 
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format('%04x', port.toInteger())
	return hexport
}

def createCommand(String command, payload, callback){

    def ipAddress = ipAddress ?: settings?.ipAddress ?: device.latestValue("ipAddress");
    def username = username ?: settings?.username ?: device.latestValue("username");
    def password = password ?: settings?.password ?: device.latestValue("password");

    log.debug "createCommandAction(${command}:${payload}) to device at ${ipAddress}:80"

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
    setPower("1", "on")
    setPower("2", "on")
    setPower("3", "on")
    setPower("4", "on")
    setPower("5", "on")
}

def off(){
    setPower("1", "off")
    setPower("2", "off")
    setPower("3", "off")
    setPower("4", "off")
    setPower("5", "off")
}

def turnon1(){
	setPower("1", "on")
}

def turnoff1(){
	setPower("1", "off")
}

def turnon2(){
	setPower("2", "on")
}

def turnoff2(){
	setPower("2", "off")
}

def turnon3(){
	setPower("3", "on")
}

def turnoff3(){
	setPower("3", "off")
}

def turnon4(){
	setPower("4", "on")
}

def turnoff4(){
	setPower("4", "off")
}

def turnon5(){
	setPower("5", "on")
}

def turnoff5(){
	setPower("5", "off")
}

def setPower(channel, power){
	log.debug "Setting power for channel $channel to $power"

	def commandName = "Power$channel";
	def payload = power;

	log.debug "COMMAND: $commandName ($payload)"

	def command = createCommand(commandName, payload, "setPowerCallback");;

    	sendHubCommand(command);
}

def setPowerCallback(physicalgraph.device.HubResponse response){
	
	
	log.debug "Finished Setting power, JSON: ${response.json}"

   	def on = response.json."POWER1" == "ON";
	//Sometimes channel 1 will just say POWER, not POWER1
	on = on || response.json."POWER" == "ON";
    	setSwitchState("1", on);
    	
   	on = response.json."POWER2" == "ON";
    	setSwitchState("2", on);
   	
   	on = response.json."POWER3" == "ON";
    	setSwitchState("3", on);
   	
   	on = response.json."POWER4" == "ON";
    	setSwitchState("4", on);
   	
   	on = response.json."POWER5" == "ON";
    	setSwitchState("5", on);
   	
    	
}

def updateStatus(status){

	//refresh();
	// Device power status(es) are reported back by the Status.Power property
	// The Status.Power property contains the on/off state of all channels (in case of a Sonoff 4CH or Sonoff Dual)
	// This is binary-encoded where each bit represents the on/off state of a particular channel
	// EG: 7 in binary is 0111.  In this case channels 1, 2, and 3 are ON and channel 4 is OFF

	def powerMask1 = 0b0001;
	def powerMask2 = 0b0001;
	def powerMask3 = 0b0001;
	def powerMask4 = 0b0001;
	def powerMask5 = 0b0001;

	def powerChannel1 = 1;
	def powerChannel2 = 2;
	def powerChannel3 = 3;
	def powerChannel4 = 4;
	def powerChannel5 = 5;

	powerMask1 = powerMask1 << ("$powerChannel1".toInteger() - 1); // shift the bits over 
	powerMask2 = powerMask2 << ("$powerChannel2".toInteger() - 1); // shift the bits over 
	powerMask3 = powerMask3 << ("$powerChannel3".toInteger() - 1); // shift the bits over 
	powerMask4 = powerMask4 << ("$powerChannel4".toInteger() - 1); // shift the bits over 
	powerMask5 = powerMask5 << ("$powerChannel5".toInteger() - 1); // shift the bits over 

	def on = (powerMask1 & status.Status.Power);
	setSwitchState("1", on)
	
	on = (powerMask2 & status.Status.Power);
	setSwitchState("2", on)

	on = (powerMask3 & status.Status.Power);
	setSwitchState("3", on)

	on = (powerMask4 & status.Status.Power);
	setSwitchState("4", on)

	on = (powerMask5 & status.Status.Power);
	setSwitchState("5", on)

}

def setSwitchState(channel, on){
	log.debug "Setting switch for channel $channel to ${on ? 'ON' : 'OFF'}";

	sendEvent(name: "switch$channel", value: on ? "on" : "off", displayed: true);
}

def ping() {
	log.debug "ping()"
	return refresh()
}
