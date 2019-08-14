/**
 *  GE/Jasco Z-Wave Plus Dimmer Switch
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Author:  Davin Dameron
 *	Based off work by: Chris Nussbaum https://github.com/nuttytree/Nutty-SmartThings/tree/master/devicetypes/nuttytree
 *      Thanks to bradlee_s on SmartThings Community Forum for the switchMode parameter
 *	Date: 08/14/2019
 *
 *   Button Mappings:
 *
 *   ACTION          BUTTON#    BUTTON ACTION
 *   Double-Tap Up     1        pressed
 *   Double-Tap Down   2        pressed
 *
 */
metadata {
	definition (name: "GE Dimmer", namespace: "davindameron", author: "Davin Dameron", mnmn:"SmartThings", vid:"generic-dimmer") {
		capability "Actuator"
		capability "Button"
		capability "Configuration"
		capability "Health Check"
		capability "Indicator"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"

        
        command "doubleUp"
        command "doubleDown"
        command "inverted"
        command "notInverted"
        command "levelUp"
        command "levelDown"
        command "setZwaveSteps"
        command "setZwaveDelay"
        command "setManualSteps"
        command "setManualDelay"
        command "setAllSteps"
        command "setAllDelay"
        command "modeSwitch"
        command "modeDimmer"
        
        // These include version because there are older firmwares that don't support double-tap or the extra association groups
        fingerprint mfr:"0063", prod:"4944", model:"3038", ver: "5.29", deviceJoinName: "GE Z-Wave Plus Wall Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3038", ver: "5.26", deviceJoinName: "GE Z-Wave Plus Wall Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3039", ver: "5.19", deviceJoinName: "GE Z-Wave Plus 1000W Wall Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3130", ver: "5.21", deviceJoinName: "GE Z-Wave Plus Toggle Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3135", ver: "5.26", deviceJoinName: "Jasco Z-Wave Plus Wall Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3136", ver: "5.21", deviceJoinName: "Jasco Z-Wave Plus 1000W Wall Dimmer"
        fingerprint mfr:"0063", prod:"4944", model:"3137", ver: "5.20", deviceJoinName: "Jasco Z-Wave Plus Toggle Dimmer"
	}


	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"
		status "09%": "command: 2003, payload: 09"
		status "10%": "command: 2003, payload: 0A"
		status "33%": "command: 2003, payload: 21"
		status "66%": "command: 2003, payload: 42"
		status "99%": "command: 2003, payload: 63"

		// reply messages
		reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
		reply "200100,delay 5000,2602": "command: 2603, payload: 00"
		reply "200119,delay 5000,2602": "command: 2603, payload: 19"
		reply "200132,delay 5000,2602": "command: 2603, payload: 32"
		reply "20014B,delay 5000,2602": "command: 2603, payload: 4B"
		reply "200163,delay 5000,2602": "command: 2603, payload: 63"
	}
    
    preferences {
        input (
            type: "paragraph",
            element: "paragraph",
            title: "Configure Association Groups:",
            description: "Devices in association group 2 will receive Basic Set commands directly from the switch when it is turned on or off. Use this to control another device as if it was connected to this switch.\n\n" +
                         "Devices in association group 3 will receive Basic Set commands directly from the switch when it is double tapped up or down.\n\n" +
                         "Devices are entered as a comma delimited list of IDs in hexadecimal format."
        )

        input (
            name: "requestedGroup2",
            title: "Association Group 2 Members (Max of 5):",
            type: "text",
            required: false
        )

        input (
            name: "requestedGroup3",
            title: "Association Group 3 Members (Max of 4):",
            type: "text",
            required: false
        )
	input(name: "debugLogging", type: "boolean", title: "Turn on debug logging?", displayDuringSetup:true, required: false)
	input(name: "inverted", type: "boolean", title: "Switch Inverted", displayDuringSetup:true, required: false)
	input("indicator", "enum", options: [
	                "whenOff": "When Off",
	                "whenOn": "When On",
                	"never": "Never"], title: "Indicator", defaultVale:"whenOff",required:true)
	input("switchMode", "enum", options: [
	                "dimmer": "Dimmer",
                	"switch": "Switch"], title: "Switch Mode", defaultVale:"dimmer",required:true)
	input(name: "zwaveSteps", type: "number", range: "1..99", title: "zWave Dim Steps (1-99)", displayDuringSetup:true, required:true)
	input(name: "zwaveDelay", type: "number", range: "1..255", title: "zWave Delay (1-255)", displayDuringSetup:true, required:true)
	input(name: "manualSteps", type: "number", range: "1..99", title: "Manual Dim Steps (1-99)", displayDuringSetup:true, required:true)
	input(name: "manualDelay", type: "number", range: "1..255", title: "Manual Delay (1-255)", displayDuringSetup:true, required:true)
	input(name: "allSteps", type: "number", range: "1..99", title: "All Dim Steps (1-99)", displayDuringSetup:true, required:true)
	input(name: "allDelay", type: "number", range: "1..255", title: "All Delay (1-255)", displayDuringSetup:true, required:true)
        
    }

	tiles(scale:2) {
        
	standardTile("switch", "device.switch", decoration: "flat", width: 2, height: 2, canChangeIcon: true) {
	    state "off", label:'${name}', action: "switch.on", icon: "st.switches.switch.on", backgroundColor:"#ffffff"
	    state "on", label:'${name}', action: "switch.off", icon: "st.switches.switch.off", backgroundColor:"#00a0dc"
	}        
	controlTile("levelSliderControl", "device.level", "slider",
            height: 2, width: 2) {
    	state "level", action:"switch level.setLevel", label:'Ring Level'
	}
        standardTile("doubleUp", "device.button", width: 3, height: 2, decoration: "flat") {
			state "default", label: "Double Tap Up", backgroundColor: "#ffffff", action: "doubleUp", icon: "https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOnIcon.png"
		}     
 
        standardTile("doubleDown", "device.button", width: 3, height: 2, decoration: "flat") {
			state "default", label: "Double Tap Down", backgroundColor: "#ffffff", action: "doubleDown", icon: "https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOffIcon.png"
		} 

		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

        }

		main "switch"
        details(["switch", "levelSliderControl", "doubleUp", "doubleDown", "refresh", "switchModeSwitch", "switchModeDimmer"])
	}

def doLogging(value){
	def debugLogging = debugLogging ?: settings?.debugLogging ?: device.latestValue("debugLogging");
	if (debugLogging=="true")
	{
		log.debug value;
	}
}

// parse events into attributes
def parse(String description) {
    //doLogging "description: $description"
    def result = null
    def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x26: 3, 0x56: 1, 0x70: 2, 0x72: 2, 0x85: 2])
    if (cmd) {
        result = zwaveEvent(cmd)
        //doLogging "Parsed ${cmd} to ${result.inspect()}"
    } else {
        doLogging "Non-parsed event: ${description}"
    }
    result    
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	doLogging("zwaveEvent(): CRC-16 Encapsulation Command received: ${cmd}")
	def encapsulatedCommand = zwave.commandClass(cmd.commandClass)?.command(cmd.command)?.parse(cmd.data)
	if (!encapsulatedCommand) {
		doLogging("zwaveEvent(): Could not extract command from ${cmd}")
	} else {
		doLogging("zwaveEvent(): Extracted command ${encapsulatedCommand}")
        return zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelSet cmd) {
	dimmerEvents(cmd)
}

private dimmerEvents(physicalgraph.zwave.Command cmd) {
	def value = (cmd.value ? "on" : "off")
	doLogging "dimmerEvents() got ${value}"
	def result = [createEvent(name: "switch", value: value, type: "physical")]
	result << sendEvent(name: "switch", value: value, type: "physical")
	if (cmd.value && cmd.value <= 100) {
		result << createEvent(name: "level", value: cmd.value, unit: "%", type: "physical")
		result << sendEvent(name: "level", value: cmd.value, unit: "%", type: "physical")
	}
	return result
}


def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	if (cmd.value == 255) {
    	createEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "Double-tap up (button 1) on $device.displayName", isStateChange: true, type: "physical")
    }
	else if (cmd.value == 0) {
    	createEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "Double-tap down (button 2) on $device.displayName", isStateChange: true, type: "physical")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	//doLogging "---ASSOCIATION REPORT V2--- ${device.displayName} sent groupingIdentifier: ${cmd.groupingIdentifier} maxNodesSupported: ${cmd.maxNodesSupported} nodeId: ${cmd.nodeId} reportsToFollow: ${cmd.reportsToFollow}"
    state.group3 = "1,2"
    if (cmd.groupingIdentifier == 3) {
    	if (cmd.nodeId.contains(zwaveHubNodeId)) {
        	createEvent(name: "numberOfButtons", value: 2, displayed: false)
        }
        else {
        	sendEvent(name: "numberOfButtons", value: 0, displayed: false)
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()))
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationGet(groupingIdentifier: 3).format()))
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    //doLogging "---CONFIGURATION REPORT V2--- ${device.displayName} sent ${cmd}"
    def value = ""
    def reportValue = cmd.scaledConfigurationValue
    switch (cmd.parameterNumber) {
        case 3:
            value = reportValue == 1 ? "whenOn" : reportValue == 2 ? "never" : "whenOff"
            settings.indicator = value
            doLogging "indicator [${value}]"
            break
        case 4:
            value = reportValue == 1 ? "true" : "false"
            settings.inverted = value
            doLogging "inverted [${value}]"
            break
        case 7:
            value = reportValue
            settings.zwaveSteps = value
            doLogging "zwaveSteps [${value}]"
            break
        case 8:
            value = reportValue
            settings.zwaveDelay = value
            doLogging "zwaveDelay [${value}]"
            break
        case 9:
            value = reportValue
            settings.manualSteps = value
            doLogging "manualSteps [${value}]"
            break
        case 10:
            value = reportValue
            settings.manualDelay = value
            doLogging "manualDelay [${value}]"
            break
        case 11:
            value = reportValue
            settings.allSteps = value
            doLogging "allSteps [${value}]"
            break
        case 12:
            value = reportValue
            settings.allDelay = value
            doLogging "allDelay [${value}]"
            break
        case 16:
            value = reportValue == 1 ? "switch" : "dimmer"
            settings.switchMode = value
            doLogging "switchMode [${value}]"
            break
        default:
            break
    }
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    doLogging "---MANUFACTURER SPECIFIC REPORT V2---"
	doLogging "manufacturerId:   ${cmd.manufacturerId}"
	doLogging "manufacturerName: ${cmd.manufacturerName}"
    state.manufacturer=cmd.manufacturerName
	doLogging "productId:        ${cmd.productId}"
	doLogging "productTypeId:    ${cmd.productTypeId}"
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)	
    sendEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
	def fw = "${cmd.applicationVersion}.${cmd.applicationSubVersion}"
	updateDataValue("fw", fw)
	doLogging "---VERSION REPORT V1--- ${device.displayName} is running firmware version: $fw, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
}


def zwaveEvent(physicalgraph.zwave.Command cmd) {
    doLogging "${device.displayName} received unhandled command: ${cmd}"
}

// handle commands
def configure() {
    def cmds = []
    // Get current config parameter values
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 7).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 8).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 9).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 10).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 11).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 12).format()
    
    // Add the hub to association group 3 to get double-tap notifications
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()
    
    delayBetween(cmds,500)
}

def updated() {
    if (state.lastUpdated && now() <= state.lastUpdated + 3000) return
    state.lastUpdated = now()

	def nodes = []
    def cmds = []

	if (settings.requestedGroup2 != state.currentGroup2) {
        nodes = parseAssocGroupList(settings.requestedGroup2, 2)
        cmds << zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: [])
        cmds << zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes)
        cmds << zwave.associationV2.associationGet(groupingIdentifier: 2)
        state.currentGroup2 = settings.requestedGroup2
    }

    if (settings.requestedGroup3 != state.currentGroup3) {
        nodes = parseAssocGroupList(settings.requestedGroup3, 3)
        cmds << zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: [])
        cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes)
        cmds << zwave.associationV2.associationGet(groupingIdentifier: 3)
        state.currentGroup3 = settings.requestedGroup3
    }

   	doLogging "settings.Switchmode is ${settings.switchMode}"
	if(settings.switchMode == "dimmer"){
    	doLogging "Running modeDimmer()"
		cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 16, size: 1)
	}
	else{
    	doLogging "Running modeSwitch()"
        cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 16, size: 1)
	}


	switch(settings.indicator){
        case "whenOn":
        	cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1)
            	break
        case "whenOff":
        	cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1)
            	break
        case "never":
        	cmds << zwave.configurationV2.configurationSet(configurationValue: [2], parameterNumber: 3, size: 1)
            	break
        }
        if(settings.inverted=="true"){
        	cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1)
        }
        else{
        	cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1)
        }
		cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: settings.zwaveSteps, parameterNumber: 7, size: 1)
		cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: settings.zwaveDelay, parameterNumber: 8, size: 2)
		cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: settings.manualSteps, parameterNumber: 9, size: 1)
		cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: settings.manualDelay, parameterNumber: 10, size: 2)
		cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: settings.allSteps, parameterNumber: 11, size: 1)
		cmds << zwave.configurationV2.configurationSet(scaledConfigurationValue: settings.allDelay, parameterNumber: 12, size: 2)

	sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 500)

}

def doubleUp() {
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "Double-tap up (button 1) on $device.displayName", isStateChange: true, type: "digital")
}

def doubleDown() {
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "Double-tap down (button 2) on $device.displayName", isStateChange: true, type: "digital")
}

def poll() {
	def cmds = []
    cmds << zwave.switchMultilevelV2.switchMultilevelGet().format()
	if (getDataValue("MSR") == null) {
		cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	}
	delayBetween(cmds,600)
}

def ping() {
	refresh()
}

def refresh() {
	def cmds = []
	cmds << zwave.switchMultilevelV2.switchMultilevelGet().format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 7).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 8).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 9).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 10).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 11).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 12).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 16).format()
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 3).format()
	if (getDataValue("MSR") == null) {
		cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	}
	delayBetween(cmds,600)
}


def on() {
	/*
	def cmds = []
    def cmds2 = []
    def delay = (device.currentValue("zwaveSteps") * device.currentValue("zwaveDelay")).longValue() + 1500
    zwave.basicV1.basicSet(value: 0xFF).format()
   	delay(3000)
    zwave.switchMultilevelV2.switchMultilevelGet().format()
	*/
	delayBetween([
			zwave.basicV1.basicSet(value: 0xFF).format(),
			zwave.switchMultilevelV1.switchMultilevelGet().format()
	],2000)
}

def off() {
	delayBetween([
			zwave.basicV1.basicSet(value: 0x00).format(),
			zwave.switchMultilevelV1.switchMultilevelGet().format()
	],2000)
}

def setLevel(value) {
	def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)
	if (level > 0) {
		sendEvent(name: "switch", value: "on")
	} else {
		sendEvent(name: "switch", value: "off")
	}
	sendEvent(name: "level", value: level, unit: "%")
    /*
    def delay = (device.currentValue("zwaveSteps") * device.currentValue("zwaveDelay") * level / 100).longValue() + 1500
    */
    def delay = 5000
	delayBetween ([
    	zwave.basicV1.basicSet(value: level).format(),
        zwave.switchMultilevelV1.switchMultilevelGet().format()
    ], delay )
}

def setLevel(value, duration) {
	doLogging "setLevel >> value: $value, duration: $duration"
	def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)
	def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
    /*
	def getStatusDelay = duration < 128 ? (duration*1000)+2000 : (Math.round(duration / 60)*60*1000)+2000
    */
	def getStatusDelay = 5000
	delayBetween ([zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: dimmingDuration).format(),
				   zwave.switchMultilevelV1.switchMultilevelGet().format()], getStatusDelay)
}

def levelUp() {
    int nextLevel = device.currentValue("level") + 10
    if( nextLevel > 100) {
    	nextLevel = 100
    }
    setLevel(nextLevel)
}
	
def levelDown() {
    int nextLevel = device.currentValue("level") - 10
    if( nextLevel < 0) {
    	nextLevel = 0
    }
    if (nextLevel == 0) {
    	off()
    }
    else {
	    setLevel(nextLevel)
    }
}

// Private Methods

private parseAssocGroupList(list, group) {
    def nodes = group == 2 ? [] : [zwaveHubNodeId]
    if (list) {
        def nodeList = list.split(',')
        def max = group == 2 ? 5 : 4
        def count = 0

        nodeList.each { node ->
            node = node.trim()
            if ( count >= max) {
                doLogging "Association Group ${group}: Number of members is greater than ${max}! The following member was discarded: ${node}"
            }
            else if (node.matches("\\p{XDigit}+")) {
                def nodeId = Integer.parseInt(node,16)
                if (nodeId == zwaveHubNodeId) {
                	doLogging "Association Group ${group}: Adding the hub as an association is not allowed (it would break double-tap)."
                }
                else if ( (nodeId > 0) & (nodeId < 256) ) {
                    nodes << nodeId
                    count++
                }
                else {
                    doLogging "Association Group ${group}: Invalid member: ${node}"
                }
            }
            else {
                doLogging "Association Group ${group}: Invalid member: ${node}"
            }
        }
    }
    
    return nodes
}
