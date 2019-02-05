metadata {
	//Based on work by Brett Sheleski for Tasomota-Power

	definition(name: "TMRLife Plug", namespace: "DavinDameron", author: "Davin Dameron", ocfDeviceType: "oic.d.smartplug") {
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

	standardTile("switch", "device.switch", decoration: "flat", width: 3, height: 3) {
	    state "off", label:'${name}', action: "switch.on", icon: "st.switches.switch.on", backgroundColor:"#ffffff"
	    state "on", label:'${name}', action: "switch.off", icon: "st.switches.switch.off", backgroundColor:"#dcdcdc"
	}        
	
	standardTile("refresh", "device.switch", width: 3, height: 3, inactiveLabel: false, decoration: "flat") {
			state "default", label:'Refresh', action:"refresh", icon:"st.secondary.refresh"
		}

	main "switch"
		details(["switch", "refresh"])
	}

    
    preferences {
        
        input(name: "ipAddress", type: "string", title: "IP Address", description: "IP Address of Sonoff", displayDuringSetup: true, required: true)

		section("Sonoff Host") {
			
		}

		section("Authentication") {
			input(name: "username", type: "string", title: "Username", description: "Username", displayDuringSetup: false, required: false)
			input(name: "password", type: "password", title: "Password (sent cleartext)", description: "Caution: password is sent cleartext", displayDuringSetup: false, required: false)
		}
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
    setPower("on")
}

def off(){
    setPower("off")
}

def setPower(power){
	log.debug "Setting power to: $power"

	def commandName = "Power1";
	def payload = power;

	log.debug "COMMAND: $commandName ($payload)"

	def command = createCommand(commandName, payload, "setPowerCallback");;

    	sendHubCommand(command);
}

def setPowerCallback(physicalgraph.device.HubResponse response){
	
	
	log.debug "Finished Setting power (channel: 1), JSON: ${response.json}"

   	def on = response.json."POWER1" == "ON";
	//Sometimes channel 1 will just say POWER, not POWER1
	on = on || response.json."POWER" == "ON";
    setSwitchState(on);
}

def updateStatus(status){

	//refresh();
	// Device power status(es) are reported back by the Status.Power property
	// The Status.Power property contains the on/off state of all channels (in case of a Sonoff 4CH or Sonoff Dual)
	// This is binary-encoded where each bit represents the on/off state of a particular channel
	// EG: 7 in binary is 0111.  In this case channels 1, 2, and 3 are ON and channel 4 is OFF

	def powerMask = 0b0001;

	def powerChannel = 1;

	powerMask = powerMask << ("$powerChannel".toInteger() - 1); // shift the bits over 

	def on = (powerMask & status.Status.Power);

    setSwitchState(on);
}

def setSwitchState(on){
	log.debug "Setting switch to ${on ? 'ON' : 'OFF'}";

	sendEvent(name: "switch", value: on ? "on" : "off", displayed: true);
}

def ping() {
	log.debug "ping()"
	return refresh()
}