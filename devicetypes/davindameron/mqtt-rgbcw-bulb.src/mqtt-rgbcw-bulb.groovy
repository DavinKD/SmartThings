import groovy.transform.Field

@Field final Map      BLACK = [name: "Black", rgb: "#000000", h: 0, s: 0, l: 0]

@Field final IntRange PERCENT_RANGE = (0..100)

@Field final IntRange HUE_RANGE = PERCENT_RANGE
@Field final Integer  HUE_STEP = 5
@Field final IntRange SAT_RANGE = PERCENT_RANGE
@Field final Integer  SAT_STEP = 20
@Field final Integer  HUE_SCALE = 1000
@Field final Integer  COLOR_OFFSET = HUE_RANGE.getTo() * HUE_SCALE

@Field final IntRange COLOR_TEMP_RANGE = (2200..7000)
@Field final Integer  COLOR_TEMP_DEFAULT = COLOR_TEMP_RANGE.getFrom() + ((COLOR_TEMP_RANGE.getTo() - COLOR_TEMP_RANGE.getFrom())/2)
@Field final Integer  COLOR_TEMP_STEP = 50 // Kelvin
@Field final List     COLOR_TEMP_EXTRAS = []
@Field final List     COLOR_TEMP_LIST = buildColorTempList(COLOR_TEMP_RANGE, COLOR_TEMP_STEP, COLOR_TEMP_EXTRAS)

@Field final Map MODE = [
    COLOR:	"Color",
    WHITE:	"White",
    OFF: 	"Off"
]
metadata {
	//Based on work by Brett Sheleski for Tasomota-Power

	definition(name: "MQTT RGBCW Bulb", namespace: "davindameron", author: "Davin Dameron", ocfDeviceType: "oic.d.light", mnmn:"SmartThings", vid:"generic-rgbw-color-bulb") {
		capability "Polling"
		capability "Refresh"
		capability "Switch"
		capability "Color Control"
		capability "Color Temperature"
        	capability "Switch Level"
		capability "Execute"
		capability "Signal Strength"

		command "reload"
		command "updateStatus"
		command "ringpush"
		command "loopOn"
		command "loopOff"
		command "setLoopRate", ["number"]

		attribute "colorLabel", "string"
		attribute "tempLabel", "string"
		attribute "dimmerLabel", "string"
       
	}

	// UI tile definitions
	tiles(scale: 2) {

	standardTile("switch", "device.switch", decoration: "flat", width: 3, height: 3, canChangeIcon: true) {
	    state "off", label:'${name}', action: "switch.on", icon: "st.Lighting.light11", backgroundColor:"#ffffff"
	    state "on", label:'${name}', action: "switch.off", icon: "st.Lighting.light11", backgroundColor:"#00a0dc"
	}        
	valueTile("lqi", "device.lqi", decoration: "flat", width: 3, height: 3) {
		state "default", label: 'Signal Strength ${currentValue}%'
	}
	
	controlTile("rgbSelector", "device.color", "color", height: 3, width: 2,
	            inactiveLabel: false) {
	    state "color", action: "color control.setColor", label:'Ring Color'
	}

	controlTile("levelSliderControl", "device.level", "slider",
            height: 3, width: 2) {
    	state "level", action:"switch level.setLevel", label:'Ring Level'
	}

		controlTile("colorTempSliderControl", "device.colorTemperature", "slider", width: 2, height: 3, inactiveLabel: false, range: "(2200..7000)") {
        state "colorTemperature", action: "setColorTemperature", label:"Color Temp"
    }

	standardTile("colorLoop", "device.colorLoop", decoration: "flat", width: 2, height: 3) {
        state "off", label:'Color Loop', action: "loopOn", icon: "st.Kids.kids2", backgroundColor:"#ffffff"
        state "on", label:'Color Loop', action: "loopOff", icon: "st.Kids.kids2", backgroundColor:"#00a0dc"

    }
	standardTile("refresh", "device.switch", width: 2, height: 3, inactiveLabel: false, decoration: "flat") {
			state "default", label:'Refresh', action:"refresh", icon:"st.secondary.refresh"
		}
        
    
	main "switch"
		details(["switch", "lqi", "rgbSelector", "levelSliderControl", "colorTempSliderControl", "colorLoop", "refresh"])
	}

    
    preferences {
        
        input(name: "ipAddress", type: "string", title: "IP Address", description: "IP Address of Sonoff", displayDuringSetup: true, required: true)
	input(name: "username", type: "string", title: "Username", description: "Username", displayDuringSetup: false, required: false)
	input(name: "password", type: "password", title: "Password (sent cleartext)", description: "Caution: password is sent cleartext", displayDuringSetup: false, required: false)
	input(name: "useMQTTCommands", type: "boolean", title: "Use MQTT for Commands?", displayDuringSetup: true, required: false)
	input(name: "useMQTT", type: "boolean", title: "Use MQTT for Updates?", displayDuringSetup: true, required: false)
	input(name: "MQTTProxy", type: "string", title: "MQTT Proxy Web Server", description: "MQTT Proxy Web Server", displayDuringSetup: true, required: false)
	input(name: "MQTTTopic", type: "string", title: "MQTT Topic", description: "MQTT Topic", displayDuringSetup: true, required: false)
        input(name: "simCT", type: "boolean", title: "Simulate Color Temp?", displayDuringSetup: true, required: false)
        input(name: "setVarForPower", type: "boolean", title: "Use Var1 for Power On?", displayDuringSetup: true, required: false)
	input(name: "debugLogging", type: "boolean", title: "Turn on debug logging?", displayDuringSetup:true, required: false)
        input(name: "PowerChannel", type: "number", title: "Power Channel (1-8)", description: "Power Channel of the Light", displayDuringSetup: true, required: true)
        input(name: "redLevel", type: "number", title: "Red Level (0-255)", range: "0..255", description: "Level of red LEDs", displayDuringSetup: true, required: true)
        input(name: "greenLevel", type: "number", title: "Green Level (0-255)", range: "0..255", description: "Level of green LEDs", displayDuringSetup: true, required: true)
        input(name: "blueLevel", type: "number", title: "Blue Level (0-255)", range: "0..255", description: "Level of blue LEDs", displayDuringSetup: true, required: true)
        input(name: "warmLevel", type: "number", title: "Warm White Level (0-255)", range: "0..255", description: "Level of warm white LEDs", displayDuringSetup: true, required: true)
        input(name: "coldLevel", type: "number", title: "Cold White Level (0-255)", range: "0..255", description: "Level of cold white LEDs", displayDuringSetup: true, required: true)
        input(name: "loopRate", type: "number", title: "Color loop rate (1-20 Fast-Slow)", range: "1..20", description: "range 1-20", defaultValue: 5, required: false, displayDuringSetup: true)           
	input(name: "useDev", type: "boolean", title: "Use Dev Versions for Upgrade?", displayDuringSetup: true, required: false)
	input(name: "doUpgrade", type: "boolean", title: "Perform Upgrade?", displayDuringSetup: true, required: false)
	}
}

private getCOLOR_TEMP_MAX() { 6500 }
private getCOLOR_TEMP_MIN() { 2700 }
private getCOLOR_TEMP_DIFF() { COLOR_TEMP_MAX - COLOR_TEMP_MIN }

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
				if (json."POWER${PowerChannel}"!=null) {
					doLogging("execute: got power channel")
					def on = json."POWER${PowerChannel}".toString().contains("ON");
					doLogging("execute: setting switch state")
					setSwitchState(on);
				}
				if(PowerChannel==1) {
					if (json."POWER"!=null) {
						doLogging("execute: got power channel")
						def on = json."POWER".toString().contains("ON");
						doLogging("execute: setting switch state")
						setSwitchState(on);
					}
				}
				if (json."Wifi"){
					doLogging("execute: got WIFI")
					def ss = json."Wifi"."RSSI";
					//ss = (ss*255)/100;
					sendEvent(name: "lqi", value: ss);

					def rssi = json."Wifi"."Signal";
					sendEvent(name: "rssi", value: rssi);
				}						
				//Color Temp
				if (json."CT"!=null) {
					def kelvin = Math.round((((json.CT + 6)*-1)+653)*13.84)
					doLogging "Kelvin is ${kelvin}"
					sendEvent(name: "colorTemperature", value: kelvin)
				}
				//level
				if (json."Dimmer"!=null) {
					def level = json."Dimmer";
					doLogging "SendEvent level to $level";
					sendEvent(name:"level", value:level);
				}
				//color
				if (json."Color"!=null) {
					doLogging "SendEvent Color to ${json."Color".substring(0,6)}"
					//sendEvent(name: "color", value: json."Color".substring(0,6))
					
				}
				if (json."HSBColor"!=null) {
					def values = json."HSBColor".split(',')
					Integer iHue = values[0].toInteger()
					Integer iSaturation = values[1].toInteger()
					iHue = iHue / 360 * 100
					doLogging "SendEvent hue to ${iHue}"
					doLogging "SendEvent saturation to ${iSaturation}"
					String rgbHex = colorUtil.hsvToHex(iHue, iSaturation)					

					doLogging "SendEvent Color to ${rgbHex}"
					sendEvent(name: "hue", value: iHue)
					sendEvent(name: "saturation", value: iSaturation)
					sendEvent(name: "color", value: rgbHex)
				}
				//Loop
				if (json."Scheme"!=null) {
					doLogging "Scheme [${json.Scheme}]"
					def on = json.Scheme == 2;
					setLoopState(on);
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

def setLoopRate(def nValue) {
	settings.loopRate = nValue
    state.loopRate = hex((settings.loopRate),2)
}

def installed(){
	doLogging "installed()"
}

def updated(){
	doLogging "updated()"
	def rgbwwvalue = "${settings.redLevel},${settings.greenLevel},${settings.blueLevel},${settings.warmLevel},${settings.coldLevel}"
	setRgbww(rgbwwvalue)
	if (doUpgrade=="true"){
		doLogging "doUpgrade is true"
		setOTAURL()
		doUpgrade()
		device.updateSetting("doUpgrade", false)
		//settings[doUpgrade]="false"
	}
	setOption57(1)
}

def setOption56(value){
	sendCommand("setOption56", value, setOption56Callback);
}

def setOption56Callback(physicalgraph.device.HubResponse response){
	doLogging "setOption56Callback(${response})"
	def jsobj = response?.json;

	doLogging "JSON: ${jsobj}";
}

def setOption57(value){
	sendCommand("setOption57", value, setOption56Callback);
}

def setOption57Callback(physicalgraph.device.HubResponse response){
	doLogging "setOption57Callback(${response})"
	def jsobj = response?.json;

	doLogging "JSON: ${jsobj}";
}


def setOTAURL(){
	if (useDev=="true"){
		sendCommand("OtaUrl", "http://192.168.0.40/tasmota.bin", setOTAURLCallback);
	}
	else {
		sendCommand("OtaUrl", "http://thehackbox.org/tasmota/release/tasmota.bin", setOTAURLCallback);
	}
}

def setOTAURLCallback(physicalgraph.device.HubResponse response){
	doLogging "setOTAURLCallback(${response})"
}

def doUpgrade(){
	sendCommand("Upgrade", "1", doUpgradeCallback)
}

def doUpgradeCallback(physicalgraph.device.HubResponse response){
	doUpgradeCallback "doUpgradeCallback(${response})"
}

def reload(){
	doLogging "reload()"
}

def poll() {
	doLogging "POLL"
	refresh()
}

def refresh() {
	doLogging "refresh()"
	sendCommand("Status", "11", refreshCallback)
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
	if(settings.useMQTTCommands=="true"){
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
	else{

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
}

def setColorTemperature(kelvin) {
	doLogging "executing 'setColorTemperature' ${kelvin}K"
	def simCT = simCT ?: settings?.simCT ?: device.latestValue("simCT");
	if (simCT!="true"){	
		def bulbValue = Math.round((((kelvin/13.84)-6)*-1)+653) 
		doLogging "bulb value ${bulbValue}"

		def commandName = "CT";
		def payload = bulbValue;

		doLogging "COMMAND: $commandName ($payload)"

		def command = createCommand(commandName, payload, "setColorTemperatureCallback");;

		sendHubCommand(command);
		def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
		if (useMQTT!="true"){

			sendEvent(name: "colorTemperature", value: kelvin)
		}
	}
	else {
			setColor("FFFFFF")
			sendEvent(name: "colorTemperature", value: kelvin)
	}
}

def setColorTemperatureCallback(physicalgraph.device.HubResponse response){
	def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");
	
	doLogging "Finished Setting Color Temperature (channel: ${PowerChannel}), JSON: ${response.json}"
	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		def kelvin = Math.round((((response.json.CT + 6)*-1)+653)*13.84)
		doLogging "Kelvin is ${kelvin}"


		def on = response.json."POWER${PowerChannel}" == "ON";
		if(PowerChannel==1){
			on = on || response.json."POWER" == "ON";
		}
		def level = response.json."Dimmer";
		doLogging "SendEvent level to $level";
		sendEvent(name:"level", value:level);
		setSwitchState(on);
	}
}

def on(){
	
	def setVarForPower = setVarForPower ?: settings?.setVarForPower ?: device.latestValue("setVarForPower");
	if (setVarForPower!="true"){
    		setPower("on")
	}
	else {
		setVar1("Var1%201.000;RuleTimer1%202")
	}
}

def off(){
    setPower("off")
}


def setPower(power){
	def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");
	doLogging "Setting power${PowerChannel} to: $power"

	def commandName = "set/state";
	def payload = power;

	doLogging "COMMAND: $commandName ($payload)"

	def command = createCommand(commandName, payload, "setPowerCallback");;

    	sendHubCommand(command);
}
def setPowerCallback(physicalgraph.device.HubResponse response){
	def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");
	doLogging "Finished Setting power (channel: ${PowerChannel}), JSON: ${response.json}"
	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		def on = response.json."POWER${PowerChannel}".toString().contains("ON");
		if(PowerChannel==1){
			on = on || response.json."POWER".toString().contains("ON");
		}
		setSwitchState(on);
	}
}

def setVar1(value){
	doLogging "Setting Var1 to: $value"

	def commandName = "Backlog";
	def payload = value;

	doLogging "COMMAND: $commandName ($payload)"

	def command = createCommand(commandName, payload, "setVar1Callback");;

    	sendHubCommand(command);
}
def setVar1Callback(physicalgraph.device.HubResponse response){
	doLogging "Finished Setting Var1, JSON: ${response.json}"
	//Has to use MQTT
}

def setLevel(level){
	doLogging "Setting level to: $level"

	def commandName = "Dimmer";
	def payload = level;

	doLogging "COMMAND: $commandName ($payload)"

	def command = createCommand(commandName, payload, "setLevelCallback");;

   	sendHubCommand(command);
}

def setLevelCallback(physicalgraph.device.HubResponse response){
	
	
	def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");
	doLogging "Finished Setting level (channel: ${PowerChannel}), JSON: ${response.json}"
	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		def on = response.json."POWER${PowerChannel}" == "ON";
		if(PowerChannel==1){
			on = on || response.json."POWER" == "ON";
		}
		def level = response.json."Dimmer";
		doLogging "SendEvent level to $level";
		sendEvent(name:"level", value:level);
		setSwitchState(on);
	}
}

private Map buildColorHSMap(hue, saturation) {
	doLogging "Executing 'buildColorHSMap(${hue}, ${saturation})'"
    Map colorHSMap = [hue: 0, saturation: 0]
    try {
        colorHSMap.hue = hue.toFloat().toInteger()
        colorHSMap.saturation = saturation.toFloat().toInteger()
    } catch (NumberFormatException nfe) {
        doLogging "Couldn't transform one of hue ($hue) or saturation ($saturation) to integers: $nfe"
    }
    doLogging colorHSMap
    return colorHSMap
}

private List buildColorTempList(IntRange kRange, Integer kStep, List kExtras) {
    List colorTempList = [kRange.getFrom()] // start with range lower bound
    Integer kFirstNorm = kRange.getFrom() + kStep - (kRange.getFrom() % kStep) // find the first value within thr range which is a factor of kStep
    colorTempList += (kFirstNorm..kRange.getTo()).step(kStep) // now build the periodic list
    colorTempList << kRange.getTo() // include range upper bound
    colorTempList += kExtras // add in extra values
    return colorTempList.sort().unique() // sort and de-dupe
	
}

private Integer boundInt(Number value, IntRange theRange) {
    value = Math.max(theRange.getFrom(), value)
    value = Math.min(theRange.getTo(), value)
    return value.toInteger()
}

def setSaturation(saturationPercent) {
    doLogging "Executing 'setSaturation' ${saturationPercent}/100"
    Integer currentHue = device.currentValue("hue")
    setColor(currentHue, saturationPercent)
    // setColor will call done() for us
}

def setHue(huePercent) {
    doLogging "Executing 'setHue' ${huePercent}/100"
    Integer currentHue = huePercent.toInteger()
    Integer currentSaturation = device.currentValue("saturation")
    setColor(currentHue, currentSaturation)
    // setColor will call done() for us
}

def setColor(Integer huePercent, Integer saturationPercent) {
    doLogging "Executing 'setColor' from separate values hue: $huePercent, saturation: $saturationPercent"
    //Map colorHSMap = buildColorHSMap(huePercent, saturationPercent)
    setColor(buildColorHSMap(huePercent, saturationPercent)) // call the capability version method overload
}

def setColor(String rgbHex) {
    doLogging "Executing 'setColor' from hex $rgbHex"
    if (hex == "#000000") {
        // setting to black? turn it off.
        off()
    } else {
        List hsvList = colorUtil.hexToHsv(rgbHex)
        Map colorHSMap = buildColorHSMap(hsvList[0], hsvList[1])
        setColor(colorHSMap) // call the capability version method overload
    }
}

def setColor(Map colorHSMap) {
    doLogging "Executing 'setColor(Map)' ${colorHSMap}"
    Integer boundedHue = boundInt(colorHSMap?.hue?:0, PERCENT_RANGE)
    Integer boundedSaturation = boundInt(colorHSMap?.saturation?:0, PERCENT_RANGE)
    String rgbHex = colorUtil.hsvToHex(boundedHue, boundedSaturation)
    doLogging "bounded hue and saturation: $boundedHue, $boundedSaturation; hex conversion: $rgbHex"

	def commandName = "HSBColor";
    
    Integer tasHue = boundedHue*3.6
	def payload = "${tasHue},${boundedSaturation},${device.currentValue("level")}";

	doLogging "COMMAND: $commandName ($payload)"

	def command = createCommand(commandName, payload, "setColorCallback");

    sendHubCommand(command);

	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		sendEvent(name: "hue", value: boundedHue)
		sendEvent(name: "saturation", value: boundedSaturation)
		sendEvent(name: "color", value: rgbHex)
	}
}

def setColorCallback(physicalgraph.device.HubResponse response){
	def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");
	
	doLogging "Finished Setting color (channel: ${PowerChannel}), JSON: ${response.json}"

	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		def on = response.json."POWER${PowerChannel}" == "ON";
		if(PowerChannel==1){
			on = on || response.json."POWER" == "ON";
		}
		setSwitchState(on);
	}
}
def updateStatus(status){
	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		def PowerChannel = PowerChannel ?: settings?.PowerChannel ?: device.latestValue("PowerChannel");
		def on = status.StatusSTS."POWER${PowerChannel}".toString().contains("ON");
		if(PowerChannel==1){
			on = on || status.StatusSTS."POWER".toString().contains("ON");
		}
		setSwitchState(on);
		doLogging "Scheme [${status.StatusSTS.Scheme}]"
		on = status.StatusSTS.Scheme == 2;
		setLoopState(on);
	}
}

def setSwitchState(on){
	doLogging "Setting switch to ${on ? 'ON' : 'OFF'}";

	sendEvent(name: "switch", value: on ? "on" : "off", displayed: true);
}


def ping() {
	doLogging "ping()"
	return refresh()
}

def loopOn() {
    def loopRate = loopRate ?: settings?.loopRate ?: device.latestValue("loopRate");
	setSpeed(loopRate);
	setLoop("2,0");
    
    //if (switch.state=="off")
    //{
    	on();
    //}
}

def loopOff() {
	setLoop("0");
}

def setRgbww(value){
	doLogging "Setting rgbwwtable to: $value"

	def commandName = "rgbwwtable";
	def payload = value;

	doLogging "COMMAND: $commandName ($payload)"

	def command = createCommand(commandName, payload, "setRgbwwCallback");;

    	sendHubCommand(command);
}

def setRgbwwCallback(physicalgraph.device.HubResponse response){
	doLogging "Finished Setting rgbwwtable , JSON: ${response.json}"
}

def setLoop(power){
	doLogging "Setting Fade to: $power"

	def commandName = "Scheme";
	def payload = power;

	doLogging "COMMAND: $commandName ($payload)"

	def command = createCommand(commandName, payload, "setLoopCallback");;

    	sendHubCommand(command);
}

def setLoopCallback(physicalgraph.device.HubResponse response){
	
	
	doLogging "Finished Setting Fade , JSON: ${response.json}"
	def useMQTT = useMQTT ?: settings?.useMQTT ?: device.latestValue("useMQTT");
	if (useMQTT!="true"){
		doLogging "Scheme [${response.json.Scheme}]"
		def on = response.json.Scheme == 2;
		setLoopState(on);
	}
}

def setLoopState(on){
	doLogging "Setting Loop to ${on ? 'ON' : 'OFF'}";

	sendEvent(name: "colorLoop", value: on ? "on" : "off", displayed: true);
}

def setSpeed(value){
	doLogging "Setting Speed to: $value"

	def commandName = "Speed";
	def payload = value;

	doLogging "COMMAND: $commandName ($payload)"

	def command = createCommand(commandName, payload, "setSpeedCallback");;

    	sendHubCommand(command);
}

def setSpeedCallback(physicalgraph.device.HubResponse response){
	
	
	doLogging "Finished Setting Speed , JSON: ${response.json}"

}
