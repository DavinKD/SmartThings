metadata {
	//Based on work by Brett Sheleski for Tasomota-Power

	definition(name: "Tasmota Sprinkler Controller", namespace: "DavinDameron", author: "Davin Dameron", ocfDeviceType: "oic.d.smartplug") {
		capability "Polling"
		capability "Refresh"
		capability "Switch"
		capability "Execute"

        	command "reload"
        	command "updateStatus"
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
        	command "updateSchedule"
        
	}

	// UI tile definitions
	tiles(scale: 2) {

		standardTile("switch", "device.switch", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
		    state "off", label:'${name}', action: "switch.on", icon: "st.Health & Wellness.health7", backgroundColor:"#ffffff"
		    state "on", label:'${name}', action: "switch.off", icon: "st.Health & Wellness.health7", backgroundColor:"#00a0dc"
		}        

		standardTile("switch1", "switch1", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
		    state "off", label:'${name}', action: "turnon1", icon: "st.Outdoor.outdoor12", backgroundColor:"#ffffff"
		    state "on", label:'${name}', action: "turnoff1", icon: "st.Outdoor.outdoor12", backgroundColor:"#00a0dc"
		}        

		standardTile("switch2", "switch2", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
		    state "off", label:'${name}', action: "turnon2", icon: "st.Outdoor.outdoor12", backgroundColor:"#ffffff"
		    state "on", label:'${name}', action: "turnoff2", icon: "st.Outdoor.outdoor12", backgroundColor:"#00a0dc"
		}        

		standardTile("switch3", "switch3", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
		    state "off", label:'${name}', action: "turnon3", icon: "st.Outdoor.outdoor12", backgroundColor:"#ffffff"
		    state "on", label:'${name}', action: "turnoff3", icon: "st.Outdoor.outdoor12", backgroundColor:"#00a0dc"
		}        

		standardTile("switch4", "switch4", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
		    state "off", label:'${name}', action: "turnon4", icon: "st.Outdoor.outdoor12", backgroundColor:"#ffffff"
		    state "on", label:'${name}', action: "turnoff4", icon: "st.Outdoor.outdoor12", backgroundColor:"#00a0dc"
		}        

		standardTile("switch5", "switch5", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
		    state "off", label:'${name}', action: "turnon5", icon: "st.Outdoor.outdoor12", backgroundColor:"#ffffff"
		    state "on", label:'${name}', action: "turnoff5", icon: "st.Outdoor.outdoor12", backgroundColor:"#00a0dc"
		}        

		standardTile("refresh", "refresh", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
				state "default", label:'Refresh', action:"refresh", icon:"st.secondary.refresh"
		}


		main("switch")
			details(["switch", "switch1", "switch2", "switch3", "switch4", "switch5", "refresh"])
	}

	preferences {

		input(name: "ipAddress", type: "string", title: "IP Address", description: "IP Address of Sonoff", displayDuringSetup: true, required: true)
		input(name: "username", type: "string", title: "Username", description: "Username", displayDuringSetup: false, required: false)
		input(name: "password", type: "password", title: "Password (sent cleartext)", description: "Caution: password is sent cleartext", displayDuringSetup: false, required: false)
		input(name: "debugLogging", type: "boolean", title: "Turn on debug logging?", displayDuringSetup:true, required: true)
		input(name: "useMQTT", type: "boolean", title: "Use MQTT for Updates?", displayDuringSetup: true, required: false)
		input(name: "zone1RunTime", type: "number", title: "Zone 1 Run Time", displayDuringSetup:true, required:true)
		input(name: "zone2RunTime", type: "number", title: "Zone 2 Run Time", displayDuringSetup:true, required:true)
		input(name: "zone3RunTime", type: "number", title: "Zone 3 Run Time", displayDuringSetup:true, required:true)
		input(name: "zone4RunTime", type: "number", title: "Zone 4 Run Time", displayDuringSetup:true, required:true)
		input(name: "zone5RunTime", type: "number", title: "Zone 5 Run Time", displayDuringSetup:true, required:true)
		input(name: "StartHour", type: "number", title: "Start Hour", displayDuringSetup:true, required: false)
		input(name: "StartMinute", type: "number", title: "Start Minute", displayDuringSetup:true, required: false)
		input(name: "StartSunrise", type: "boolean", title: "Start at Sunrise?", displayDuringSetup:true, required: false)
		input(name: "StartSunset", type: "boolean", title: "Start at Sunset?", displayDuringSetup:true, required: false)
		input(name: "StartOffSetMinutes", type: "number", title: "Start Offset Minutes", displayDuringSetup:true, required:true)
		input(name: "doSunday", type: "boolean", title: "Water Sunday", displayDuringSetup:true, required: false)
		input(name: "doMonday", type: "boolean", title: "Water Monday", displayDuringSetup:true, required: false)
		input(name: "doTuesday", type: "boolean", title: "Water Tuesday", displayDuringSetup:true, required: false)
		input(name: "doWednesday", type: "boolean", title: "Water Wednesday", displayDuringSetup:true, required: false)
		input(name: "doThursday", type: "boolean", title: "Water Thursday", displayDuringSetup:true, required: false)
		input(name: "doFriday", type: "boolean", title: "Water Friday", displayDuringSetup:true, required: false)
		input(name: "doSaturday", type: "boolean", title: "Water Saturday", displayDuringSetup:true, required: false)
		
		
		
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
	updateSchedule();
}

def reload(){
	doLogging "reload()"
}

def poll() {
	doLogging "POLL"
}

def refresh() {
	doLogging "refresh()"
	sendCommand("Status", "11", refreshCallback)
}

def refresh2() {
	doLogging "refresh2()"
	sendCommand("Status", "0", refreshCallback)
}




def refreshCallback(physicalgraph.device.HubResponse response){
	doLogging "refreshCallback()"
	def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");
	doLogging "Finished Setting power (channel: ${PowerChannel}), JSON: ${response.json}"
	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		    def jsobj = response?.json;

		    doLogging "JSON: ${jsobj}";
		    updateStatus(jsobj);
	}
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

    doLogging "createCommandAction(${command}:${payload}) to device at ${ipAddress}:80"

	if (!ipAddress) {
		doLogging "aborting. ip address of device not set"
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
    doLogging path;

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
	setSwitchState("", true)
	updateSchedule();
}

def off(){
	setSwitchState("", false)
	updateSchedule();
}

def updateSchedule() {
	doLogging "Setting schedule"

	def commandName = "Timer1";
	
	def zone1RunTime = zone1RunTime ?: settings?.zone1RunTime ?: device.latestValue("zone1RunTime");
	def zone2RunTime = zone2RunTime ?: settings?.zone2RunTime ?: device.latestValue("zone2RunTime");
	def zone3RunTime = zone3RunTime ?: settings?.zone3RunTime ?: device.latestValue("zone3RunTime");
	def zone4RunTime = zone4RunTime ?: settings?.zone4RunTime ?: device.latestValue("zone4RunTime");
	def zone5RunTime = zone5RunTime ?: settings?.zone5RunTime ?: device.latestValue("zone5RunTime");
	def StartHour = StartHour ?: settings?.StartHour ?: device.latestValue("StartHour");
	def StartMinute = StartMinute ?: settings?.StartMinute ?: device.latestValue("StartMinute");
	def StartSunrise = StartSunrise ?: settings?.StartSunrise ?: device.latestValue("StartSunrise");
	def StartSunset = StartSunset ?: settings?.StartSunset ?: device.latestValue("StartSunset");
	def StartOffSetMinutes = StartOffSetMinutes ?: settings?.StartOffSetMinutes ?: device.latestValue("StartOffSetMinutes");
	def doSunday = doSunday ?: settings?.doSunday ?: device.latestValue("doSunday");
	def doMonday = doMonday ?: settings?.doMonday ?: device.latestValue("doMonday");
	def doTuesday = doTuesday ?: settings?.doTuesday ?: device.latestValue("doTuesday");
	def doWednesday = doWednesday ?: settings?.doWednesday ?: device.latestValue("doWednesday");
	def doThursday = doThursday ?: settings?.doThursday ?: device.latestValue("doThursday");
	def doFriday = doFriday ?: settings?.doFriday ?: device.latestValue("doFriday");
	def doSaturday = doSaturday ?: settings?.doSaturday ?: device.latestValue("doSaturday");

	def timeVal;
	
	def timerVal = "{'Arm':";
	
	doLogging device.currentValue("switch");
	if(device.currentValue("switch") =="on"){
		timerVal += "1";
	}
	else{
		timerVal += "0";
	}
	timerVal += ",'Mode':";
	if(StartSunrise=="true"){
		timerVal +="1";
		if(StartOffSetMinutes < 10){
			timeVal = "00:0" + StartOffSetMinutes;
		}
		else{
			timeVal = "00:" + StartOffSetMinutes;
		}
	}
	else if(StartSunset=="true"){
		timerVal +="2";
		if(StartOffSetMinutes < 10){
			timeVal = "00:0" + StartOffSetMinutes;
		}
		else{
			timeVal = "00:" + StartOffSetMinutes;
		}
	}
	else{
		timerVal +="0";
		timeVal = "";
		if(StartHour < 10){
			timeVal += "0" + StartHour;
		}
		else{
			timeVal += StartHour;
		}
		if(StartMinute < 10){
			timeVal += ":0" + StartMinute;
		}
		else{
			timeVal += ":" + StartMinute;
		}
	}
	timerVal += ",'Time':'" + timeVal + "','Window':0,'Days':'";
	if(doSunday=="true"){
		timerVal += "1";
	}
	else{
		timerVal += "0";
	}
	if(doMonday=="true"){
		timerVal += "1";
	}
	else{
		timerVal += "0";
	}
	if(doTuesday=="true"){
		timerVal += "1";
	}
	else{
		timerVal += "0";
	}
	if(doWednesday=="true"){
		timerVal += "1";
	}
	else{
		timerVal += "0";
	}
	if(doThursday=="true"){
		timerVal += "1";
	}
	else{
		timerVal += "0";
	}
	if(doFriday=="true"){
		timerVal += "1";
	}
	else{
		timerVal += "0";
	}
	if(doSaturday=="true"){
		timerVal += "1";
	}
	else{
		timerVal += "0";
	}
	timerVal += "','Repeat':1,'Action':3}"
	doLogging timerVal;


	//{"Arm":1,"Mode":1,"Time":"00:10","Window":0,"Days":"0101010","Repeat":1,"Output":1,"Action":3}


	
	def payload = timerVal;

	doLogging "COMMAND: $commandName ($payload)"

	def command = createCommand(commandName, payload, "updateScheduleCallback");;

    	sendHubCommand(command);
    	
    	updateDurations();
}

def updateScheduleCallback(physicalgraph.device.HubResponse response) {
	doLogging "Finished Setting Schedule, JSON: ${response.json}"

}

def updateDurations(){
	updateDuration(1);
	updateDuration(2);
	updateDuration(3);
	updateDuration(4);
	updateDuration(5);
}

def updateDuration(Zone){

	def commandName = "Mem$Zone";
	def payload;
	
	def runtime;
	
	switch(Zone){
		case 1:
			runtime = zone1RunTime * 60;
			break
		case 2:
			runtime = zone2RunTime * 60;
			break
		case 3:
			runtime = zone3RunTime * 60;
			break
		case 4:
			runtime = zone4RunTime * 60;
			break
		case 5:
			runtime = zone5RunTime * 60;
			break
		default:
			runtime = 5*60;
	}
	if(runtime ==0){
		runtime==30;
	}
	payload = runtime;
			
	doLogging "COMMAND: $commandName ($payload)"

	def command = createCommand(commandName, payload, "updateScheduleCallback");;

    	sendHubCommand(command);
}
def updateDurationCallBack(physicalgraph.device.HubResponse response){
	doLogging "Finished Setting Duration, JSON: ${response.json}"
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
	doLogging "Setting power for channel $channel to $power"
	
	def commandName;
	def payload;
	def command;
	if(power=="on"){
		commandName = "PulseTime$channel";
		def runtime;

		switch(channel){
			case "1":
				runtime = zone1RunTime * 60;
				break
			case "2":
				runtime = zone2RunTime * 60;
				break
			case "3":
				runtime = zone3RunTime * 60;
				break
			case "4":
				runtime = zone4RunTime * 60;
				break
			case "5":
				runtime = zone5RunTime * 60;
				break
			default:
				runtime = 5*60;
		}

		if(runtime==0){
			runtime = 30;
		}
		runtime = runtime + 100;

		payload=runtime;

		doLogging "COMMAND: $commandName ($payload)"

		command = createCommand(commandName, payload, "setPulseTimeCallback");;

		sendHubCommand(command);
	}
	commandName = "Power$channel";
	payload = power;

	doLogging "COMMAND: $commandName ($payload)"

	command = createCommand(commandName, payload, "setPowerCallback");;

    	sendHubCommand(command);
}

def setPowerCallback(physicalgraph.device.HubResponse response){
	
	def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");
	doLogging "Finished Setting power (channel: ${PowerChannel}), JSON: ${response.json}"
	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
	   	refresh();
	}
    	
}

def setPulseTimeCallback(physicalgraph.device.HubResponse response){
	
	doLogging "Finished Setting PulseTimer, JSON: ${response.json}"
    	
}

def updateStatus(status){

	//refresh();
	// Device power status(es) are reported back by the Status.Power property
	// The Status.Power property contains the on/off state of all channels (in case of a Sonoff 4CH or Sonoff Dual)
	// This is binary-encoded where each bit represents the on/off state of a particular channel
	// EG: 7 in binary is 0111.  In this case channels 1, 2, and 3 are ON and channel 4 is OFF

	def on = status.StatusSTS."POWER1" == "ON";
	setSwitchState("1", on)
	
	on = status.StatusSTS."POWER2" == "ON";
	setSwitchState("2", on)

	on = status.StatusSTS."POWER3" == "ON";
	setSwitchState("3", on)

	on = status.StatusSTS."POWER4" == "ON";
	setSwitchState("4", on)

	on = status.StatusSTS."POWER5" == "ON";
	setSwitchState("5", on)
	
}

def setSwitchState(channel, on){
	doLogging "Setting switch for channel $channel to ${on ? 'ON' : 'OFF'}";

	sendEvent(name: "switch$channel", value: on ? "on" : "off", displayed: true);
}

def ping() {
	doLogging "ping()"
	return refresh()
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
				def on;
				if (json."POWER1"!=null) {
					on = json."POWER1" == "ON";
					setSwitchState("1", on);
				}
				if (json."POWER2"!=null) {
					on = json."POWER2" == "ON";
					setSwitchState("2", on);
				}
				if (json."POWER3"!=null) {
					on = json."POWER3" == "ON";
					setSwitchState("3", on);
				}
				if (json."POWER4"!=null) {
					on = json."POWER4" == "ON";
					setSwitchState("4", on);
				}
				if (json."POWER5"!=null) {
					on = json."POWER5" == "ON";
					setSwitchState("5", on);
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

