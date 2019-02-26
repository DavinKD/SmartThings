/** Please copy the Device Handler from this line
Modified by Davin Dameron to include loop command
original author is Kevin X at 3A Smart Home
Copyright 2016 SmartThings
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
in compliance with the License. You may obtain a copy of the License at:
 http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
for the specific language governing permissions and limitations under the License.
Updated by Kevin X- 3A Smart Home on 1st Jun 2018
*/
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
definition (name: "3A NUE ZigBee", namespace: "davindameron", author: "Davin Dameron", mnmn:"SmartThings", vid:"generic-rgbw-color-bulb") {

    capability "Color Control"
    capability "Configuration"
    capability "Polling"
    capability "Refresh"
    capability "Switch"
    capability "Switch Level"
    
    command "loopOn"
    command "loopOff"
    command "setLoopRate", ["number"]
    command "setColorTransition", ["number"]


    fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300", outClusters: "0019"
    fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 1000", outClusters: "0019"
    fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 1000", outClusters: "0019", "manufacturer":"3A Feibit", "model":"RGBW Light", deviceJoinName: "3A-Feibit RGBW Light"
	preferences{
       input(
             "switchTransition",
             "number",
             title: "Dim duration for On/Off",
             range: "0..10",
             description: "0-10 seconds",
             defaultValue: 2,
             required: false,
             displayDuringSetup: true
            )
       input(
             "levelTransition",
             "number",
             title: "Dim duration for level change",
             range: "0..10",
             description: "0-10 seconds",
             defaultValue: 4,
             required: false,
             displayDuringSetup: true
            )
       input(
             "colorTransition",
             "number",
             title: "Time to transition color",
             range: "0..10",
             description: "0-10 seconds",
             defaultValue: 2,
             required: false,
             displayDuringSetup: true
            )             
 	   input(
             "pulseDuration",
             "number",
             title: "Pulse dim up/down duration",
             range: "1..10",
             description: "1-10 seconds",
             defaultValue: 4,
             required: false,
             displayDuringSetup: true
            )
 	   input(
             "loopRate",
             "number",
             title: "Color loop rate in steps per second",
             range: "1..20",
             description: "range 1-25",
             defaultValue: 5,
             required: false,
             displayDuringSetup: true
            )  
	}

}

// UI tile definitions
tiles(scale: 2) {
    //multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
    //    tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
    //        attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
    //        attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
    //        attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
    //        attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
    //    }
    //   tileAttribute ("device.level", key: "SLIDER_CONTROL") {
    //   attributeState "level", action:"switch level.setLevel"
    //   }
    //    tileAttribute ("device.color", key: "COLOR_CONTROL") {
    //        attributeState "color", action:"color control.setColor"
    //    }
    //}
   	standardTile("switch", "device.switch", decoration: "flat", width: 3, height: 3, canChangeIcon: true) {
	    state "off", label:'${name}', action: "switch.on", icon: "st.Lighting.light11", backgroundColor:"#ffffff"
	    state "on", label:'${name}', action: "switch.off", icon: "st.Lighting.light11", backgroundColor:"#00a0dc"
	}        
    standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 3, height: 3) {
        state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
    }
	controlTile("rgbSelector", "device.color", "color", height: 3, width: 2,
	            inactiveLabel: false) {
	    state "color", action: "color control.setColor", label:'Color'
	}

	controlTile("levelSliderControl", "device.level", "slider",
            height: 3, width: 2) {
    	state "level", action:"switch level.setLevel", label:'Level'
	}

        controlTile("colorTempSliderControl", "device.colorTemperature", "slider", width: 2, height: 3, inactiveLabel: false, range:"(2700..6500)") {
            state "colorTemperature", action:"color temperature.setColorTemperature"
        }
    standardTile("colorLoop", "device.colorLoop", decoration: "flat", width: 2, height: 3) {
        state "off", label:'Color Loop', action: "loopOn", icon: "st.Kids.kids2", backgroundColor:"#ffffff"
        state "on", label:'Color Loop', action: "loopOff", icon: "st.Kids.kids2", backgroundColor:"#dcdcdc"
    }

    main(["switch"])
    details(["switch", "refresh", "rgbSelector", "levelSliderControl", "colorTempSliderControl", "colorLoop"])
}
}

//Globals
private getATTRIBUTE_HUE() { 0x0000 }
private getATTRIBUTE_SATURATION() { 0x0001 }
private getHUE_COMMAND() { 0x00 }
private getSATURATION_COMMAND() { 0x03 }
private getMOVE_TO_HUE_AND_SATURATION_COMMAND() { 0x06 }
private getCOLOR_CONTROL_CLUSTER() { 0x0300 }
private getATTRIBUTE_COLOR_TEMPERATURE() { 0x0007 }


private getDEFAULT_LEVEL_TRANSITION() {"2800"} //4 secs (little endian)
private getDEFAULT_COLOR_TRANSITION() {"1400"} //2 secs (little endian)
private getDEFAULT_PULSE_DURATION() {"2800"} //4 secs (little endian)
private getDEFAULT_LOOP_RATE() {"05"} //5 steps per sec

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "description is $description"

    def cmds = []
	def finalResult = zigbee.getEvent(description)
	if (finalResult) {
    	log.debug finalResult
    	sendEvent(finalResult)
	}
	else {
    	def zigbeeMap = zigbee.parseDescriptionAsMap(description)
    	log.trace "zigbeeMap : $zigbeeMap"

    	if (zigbeeMap?.clusterInt == COLOR_CONTROL_CLUSTER) {
        	if(zigbeeMap.attrInt == ATTRIBUTE_HUE){  //Hue Attribute
            	def hueValue = Math.round(zigbee.convertHexToInt(zigbeeMap.value) / 254 * 100)
            	cmds << createEvent(name: "hue", value: hueValue, displayed: false)
                Integer boundedHue = boundInt(hueValue, PERCENT_RANGE)
                Integer boundedSaturation = boundInt(device.currentValue("saturation"), PERCENT_RANGE)
				String rgbHex = colorUtil.hsvToHex(boundedHue, boundedSaturation)
                cmds << createEvent(name: "color", value: rgbHex, displayed: false)
            	//sendEvent(name: "hue", value: hueValue, displayed:false)
        	}
        	else if(zigbeeMap.attrInt == ATTRIBUTE_SATURATION){ //Saturation Attribute
            	def saturationValue = Math.round(zigbee.convertHexToInt(zigbeeMap.value) / 254 * 100)
            	cmds << createEvent(name: "saturation", value: saturationValue, displayed: false)
                Integer boundedHue = boundInt(device.currentValue("hue"), PERCENT_RANGE)
                Integer boundedSaturation = boundInt(saturationValue, PERCENT_RANGE)
				String rgbHex = colorUtil.hsvToHex(boundedHue, boundedSaturation)
                cmds << createEvent(name: "color", value: rgbHex, displayed: false)
            	//sendEvent(name: "saturation", value: saturationValue, displayed:false)
			}
    	}
    	else {
        	log.info "DID NOT PARSE MESSAGE for description : $description"
    	}
	}
    return cmds
}

def on() {
device.endpointId ="0B"
zigbee.on()
}

def off() {
device.endpointId ="0B"
zigbee.off()
zigbee.off()

}

def refresh() {
refreshAttributes() + configureAttributes()
}

//def poll() {
//refreshAttributes()
//}

def configure() {
log.debug "Configuring Reporting and Bindings."
configureAttributes() + refreshAttributes()
setColor(5000)
}

def updated() {

     String switchTransition
    if (settings.switchTransition) {
        switchTransition = hex((settings.switchTransition * 10),4) //OnOffTransitionTime in 1/10th sec (big endian)
    }
    else {
        switchTransition = "0014" //2 seconds (big endian)
    }    
    
    if (settings.levelTransition) {
        state.levelTransition = swapEndianHex(hex((settings.levelTransition * 10),4))
    }
    else {
        state.levelTransition = "2800" //4 seconds
    }    
   
    if (settings.colorTransition) {
        state.colorTransition = swapEndianHex(hex((settings.colorTransition * 10),4))
    }
    else {
        state.colorTransition = "1400" //2 seconds
    }

    if (settings.pulseDuration) {
        state.pulseDuration = swapEndianHex(hex((settings.pulseDuration * 10),4))
    }
    else {
        state.pulseDuration = "2800" //4 seconds
    }    
    
    if (settings.loopRate) {
        state.loopRate = hex((settings.loopRate),2)
    }
    else {
        state.loopRate = "05"
    }
    return new physicalgraph.device.HubAction("st wattr 0x${device.deviceNetworkId} ${endpointId} 8 0x0010 0x21 {${switchTransition}}")  // on/off dim duration  
    
}

def setLoopRate(def nValue) {
	settings.loopRate = nValue
    state.loopRate = hex((settings.loopRate),2)
}

def setColorTransition(def nValue) {
	settings.colorTransition = nValue
    state.colorTransition = swapEndianHex(hex((settings.colorTransition * 10),4))
    
}

def configureAttributes() {
zigbee.onOffConfig() + zigbee.levelConfig() + zigbee.colorTemperatureConfig() + zigbee.configureReporting(COLOR_CONTROL_CLUSTER, ATTRIBUTE_HUE, 0x20, 1, 3600, 0x01) + zigbee.configureReporting(COLOR_CONTROL_CLUSTER, ATTRIBUTE_SATURATION, 0x20, 1, 3600, 0x01)
}

def refreshAttributes() {
zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.colorTemperatureRefresh() + zigbee.readAttribute(0x0300, 0x00) + zigbee.readAttribute(0x0300, ATTRIBUTE_HUE) + zigbee.readAttribute(0x0300, ATTRIBUTE_SATURATION)
}

def setColorTemperature(value) {
device.endpointId ="0B"
zigbee.setColorTemperature(value) + ["delay 10"] + zigbee.colorTemperatureRefresh()
}

def setLevel(value) {
device.endpointId = "0B"
def additionalCmds = []
additionalCmds = refresh()
def hexConvertedValue = zigbee.convertToHexString((value/100) * 255)
zigbee.command(0x0008, 0x00, hexConvertedValue, "0000") + additionalCmds
}
/*def setLevel(value) {
device.endpointId ="0B"
zigbee.setLevel(value) + ["delay 100"] + zigbee.levelRefresh()
}
*/
private getScaledHue(value) {
    zigbee.convertToHexString(Math.round(value * 0xfe / 100.0), 2)
}

private getScaledSaturation(value) {
    zigbee.convertToHexString(Math.round(value * 0xfe / 100.0), 2)
}

private Integer boundInt(Double value, IntRange theRange) {
    value = Math.max(theRange.getFrom(), value)
    value = Math.min(theRange.getTo(), value)
    return value.toInteger()
}

def setColor(value){
log.trace "setColor($value)"
device.endpointId ="0B"
zigbee.on() + setHue(value.hue) + ["delay 100"] + setSaturation(value.saturation) + ["delay 100"]+ refreshAttributes()
//sendEvent(name: "color", value: value.hex, isStateChange: true)

}

def setColor2(value){
}

def setHue(value) {
def scaledHueValue = zigbee.convertToHexString(Math.round(value * 0xfe / 100.0), 2)
device.endpointId ="0B"
zigbee.command(COLOR_CONTROL_CLUSTER, HUE_COMMAND, scaledHueValue, "00", "0100")
}

def setSaturation(value) {
def scaledSatValue = zigbee.convertToHexString(Math.round(value * 0xfe / 100.0), 2)
device.endpointId ="0B"
zigbee.command(COLOR_CONTROL_CLUSTER, SATURATION_COMMAND, scaledSatValue, "0100")
}

private List buildColorTempList(IntRange kRange, Integer kStep, List kExtras) {
    List colorTempList = [kRange.getFrom()] // start with range lower bound
    Integer kFirstNorm = kRange.getFrom() + kStep - (kRange.getFrom() % kStep) // find the first value within thr range which is a factor of kStep
    colorTempList += (kFirstNorm..kRange.getTo()).step(kStep) // now build the periodic list
    colorTempList << kRange.getTo() // include range upper bound
    colorTempList += kExtras // add in extra values
    return colorTempList.sort().unique() // sort and de-dupe
}

def loopOn() {
    if (!state.loopRate) state.loopRate = DEFAULT_LOOP_RATE    
    def cmds = []
    cmds << zigbee.command(COLOR_CONTROL_CLUSTER, SATURATION_COMMAND, "fe", "1400") //set saturation to 100% over 2 sec    
    cmds << sendEvent(name: "colorLoop", value: "on", descriptionText: "Color Loop started", displayed: true, isChange: true)
    cmds << sendEvent(name: "colorMode", value: "RGB", displayed: false)
    cmds << zigbee.command(COLOR_CONTROL_CLUSTER, 0x01, "01", state.loopRate) //move hue command is 0x01, up is "01", rate is steps per sec
    cmds
}

def loopOff() {
    def cmds = []
    cmds << sendEvent(name: "colorLoop", value: "off", descriptionText: "Color Loop stopped", displayed: true, isChange: true)
    cmds << zigbee.command(COLOR_CONTROL_CLUSTER, 0x01, "00") //move hue command is 0x01, stop is "00"
    cmds
}
private hex(value, width=2) {
	def result = new BigInteger(Math.round(value).toString()).toString(16)
	while (result.size() < width) {
		result = "0" + result
	}
	return result
}

private String swapEndianHex(String hex) {
	reverseArray(hex.decodeHex()).encodeHex()
}

private byte[] reverseArray(byte[] array) {
    byte tmp;
    tmp = array[1];
    array[1] = array[0];
    array[0] = tmp;
    return array
}

// Please copy the device handler end of this line
