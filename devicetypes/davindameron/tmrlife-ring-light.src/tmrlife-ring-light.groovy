import groovy.transform.Field

// really? colorUtils is missing black?
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

	definition(name: "TMRLife Ring Light", namespace: "davindameron", author: "Davin Dameron", ocfDeviceType: "oic.d.smartplug") {
		capability "Polling"
		capability "Refresh"
		capability "Switch"
		capability "Color Control"
        capability "Switch Level"

        command "reload"
        command "updateStatus"
        command "ringpush"
     	command "loopOn"
    	command "loopOff"
    	command "setLoopRate", ["number"]
       
	}

	// UI tile definitions
	tiles(scale: 2) {

	standardTile("switch", "device.switch", decoration: "flat", width: 3, height: 3, canChangeIcon: true) {
	    state "off", label:'${name}', action: "switch.on", icon: "st.Lighting.light11", backgroundColor:"#ffffff"
	    state "on", label:'${name}', action: "switch.off", icon: "st.Lighting.light11", backgroundColor:"#00a0dc"
	}        
	
	standardTile("refresh", "device.switch", width: 3, height: 3, inactiveLabel: false, decoration: "flat") {
			state "default", label:'Refresh', action:"refresh", icon:"st.secondary.refresh"
		}

	controlTile("rgbSelector", "device.color", "color", height: 3, width: 2,
	            inactiveLabel: false) {
	    state "color", action: "color control.setColor", label:'Ring Color'
	}

	controlTile("levelSliderControl", "device.level", "slider",
            height: 3, width: 2) {
    	state "level", action:"switch level.setLevel", label:'Ring Level'
	}

	standardTile("colorLoop", "device.colorLoop", decoration: "flat", width: 2, height: 3) {
        state "off", label:'Color Loop', action: "loopOn", icon: "st.Kids.kids2", backgroundColor:"#ffffff"
        state "on", label:'Color Loop', action: "loopOff", icon: "st.Kids.kids2", backgroundColor:"#00a0dc"
    }
    
	main "switch"
		details(["switch", "refresh", "ringswitch", "rgbSelector", "levelSliderControl", "colorLoop"])
	}

    
    preferences {
        
        input(name: "ipAddress", type: "string", title: "IP Address", description: "IP Address of Sonoff", displayDuringSetup: true, required: true)
	input(name: "username", type: "string", title: "Username", description: "Username", displayDuringSetup: false, required: false)
	input(name: "password", type: "password", title: "Password (sent cleartext)", description: "Caution: password is sent cleartext", displayDuringSetup: false, required: false)
        input(
             "loopRate",
             "number",
             title: "Color loop rate (1-20 Fast-Slow)",
             range: "1..20",
             description: "range 1-20",
             defaultValue: 5,
             required: false,
             displayDuringSetup: true
            )           
	input(name: "debugLogging", type: "boolean", title: "Turn on debug logging?", displayDuringSetup:true, required: false)
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

	def commandName = "Power2";
	def payload = power;

	doLogging("COMMAND: $commandName ($payload)");

	def command = createCommand(commandName, payload, "setPowerCallback");;

    	sendHubCommand(command);
}

def setPowerCallback(physicalgraph.device.HubResponse response){
	
	
	doLogging("Finished Setting power (channel: 2), JSON: ${response.json}");

   	def on = response.json."POWER2" == "ON";
    setSwitchState(on);
}

def setLevel(level){
	doLogging("Setting level to: $level");

	def commandName = "Dimmer";
	def payload = level;

	doLogging("COMMAND: $commandName ($payload)");

	def command = createCommand(commandName, payload, "setLevelCallback");;

   	sendHubCommand(command);
}

def setLevelCallback(physicalgraph.device.HubResponse response){
	
	
	doLogging("Finished Setting level (channel: 2), JSON: ${response.json}");

   	def on = response.json."POWER2" == "ON";
    def level = response.json."Dimmer";
    doLogging("SendEvent level to $level");
    sendEvent(name:"level", value:level);

	setSwitchState(on);
}

private Map buildColorHSMap(hue, saturation) {
	doLogging("Executing 'buildColorHSMap(${hue}, ${saturation})'");
    Map colorHSMap = [hue: 0, saturation: 0]
    try {
        colorHSMap.hue = hue.toFloat().toInteger()
        colorHSMap.saturation = saturation.toFloat().toInteger()
    } catch (NumberFormatException nfe) {
        doLogging("Couldn't transform one of hue ($hue) or saturation ($saturation) to integers: $nfe");
    }
    doLogging(colorHSMap);
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
    doLogging("Executing 'setSaturation' ${saturationPercent}/100");
    Integer currentHue = device.currentValue("hue")
    setColor(currentHue, saturationPercent)
    // setColor will call done() for us
}

def setHue(huePercent) {
    doLogging("Executing 'setHue' ${huePercent}/100");
    Integer currentSaturation = device.currentValue("saturation")
    setColor(huePercent, currentSaturation)
    // setColor will call done() for us
}

def setColor(Integer huePercent, Integer saturationPercent) {
    doLogging("Executing 'setColor' from separate values hue: $huePercent, saturation: $saturationPercent");
    //Map colorHSMap = buildColorHSMap(huePercent, saturationPercent)
    setColor(buildColorHSMap(huePercent, saturationPercent)) // call the capability version method overload
}

def setColor(String rgbHex) {
    doLogging("Executing 'setColor' from hex $rgbHex");
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
    doLogging("Executing 'setColor(Map)' ${colorHSMap}");
    Integer boundedHue = boundInt(colorHSMap?.hue?:0, PERCENT_RANGE)
    Integer boundedSaturation = boundInt(colorHSMap?.saturation?:0, PERCENT_RANGE)
    String rgbHex = colorUtil.hsvToHex(boundedHue, boundedSaturation)
    doLogging("bounded hue and saturation: $boundedHue, $boundedSaturation; hex conversion: $rgbHex");

	def commandName = "Color";
	def payload = rgbHex;

	doLogging("COMMAND: $commandName ($payload)");

	def command = createCommand(commandName, payload, "setColorCallback");

    sendHubCommand(command);

	sendEvent(name: "hue", value: boundedHue)
    sendEvent(name: "saturation", value: boundedSaturation)
    sendEvent(name: "color", value: rgbHex)
}

def setColorCallback(physicalgraph.device.HubResponse response){
	
	
	doLogging("Finished Setting color (channel: 2), JSON: ${response.json}");

    def on = response.json."POWER2" == "ON";
	setSwitchState(on);
}
def updateStatus(status){

	//refresh();
	// Device power status(es) are reported back by the Status.Power property
	// The Status.Power property contains the on/off state of all channels (in case of a Sonoff 4CH or Sonoff Dual)
	// This is binary-encoded where each bit represents the on/off state of a particular channel
	// EG: 7 in binary is 0111.  In this case channels 1, 2, and 3 are ON and channel 4 is OFF

	def powerMaskRing = 0b0001;

	def powerChannelRing = 2;

	powerMaskRing = powerMaskRing << ("$powerChannelRing".toInteger() - 1); // shift the bits over 

	def on = (powerMaskRing & status.Status.Power);

    setSwitchState(on);
	doLogging("Scheme [${status.StatusSTS.Scheme}]");
    on = status.StatusSTS.Scheme == 2;
  
    setLoopState(on);
}

def setSwitchState(on){
	doLogging("Setting switch to ${on ? 'ON' : 'OFF'}");

	sendEvent(name: "switch", value: on ? "on" : "off", displayed: true);
}


def ping() {
	doLogging("ping()");
	return refresh()
}

def loopOn() {
	doLogging("loopOn()");
	def loopRate = loopRate ?: settings?.loopRate ?: device.latestValue("loopRate");
	setSpeed(loopRate);
	setLoop("2");
    
    //if (switch.state=="off")
    //{
    	on();
    //}
}

def loopOff() {
	doLogging("loopOff()");
	setLoop("0");
}

def setLoop(power){
	doLogging("Setting Fade to: $power");

	def commandName = "Scheme";
	def payload = power;

	doLogging("COMMAND: $commandName ($payload)");

	def command = createCommand(commandName, payload, "setLoopCallback");;

    	sendHubCommand(command);
}

def setLoopCallback(physicalgraph.device.HubResponse response){
	
	
	doLogging("Finished Setting Fade , JSON: ${response.json}");
	doLogging("Scheme [${response.json.Scheme}]");
   	def on = response.json.Scheme == 2;
    setLoopState(on);
}

def setLoopState(on){
	doLogging("Setting Loop to ${on ? 'ON' : 'OFF'}");

	sendEvent(name: "colorLoop", value: on ? "on" : "off", displayed: true, isStateChange: true);
}

def setSpeed(value){
	doLogging("Setting Speed to: $value");

	def commandName = "Speed";
	def payload = value;

	doLogging("COMMAND: $commandName ($payload)");

	def command = createCommand(commandName, payload, "setSpeedCallback");;

    	sendHubCommand(command);
}

def setSpeedCallback(physicalgraph.device.HubResponse response){
	
	
	doLogging("Finished Setting Speed , JSON: ${response.json}");

}

