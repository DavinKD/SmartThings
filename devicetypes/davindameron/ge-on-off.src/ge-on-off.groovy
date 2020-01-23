/**
 *  GE/Jasco Z-Wave Plus On/Off Switch
 *
 *  Copyright 2017 Chris Nussbaum
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
 *	Date: 03/07/2019
 *
 *
 *   Button Mappings:
 *
 *   ACTION          BUTTON#    BUTTON ACTION
 *   Double-Tap Up     1        pressed
 *   Double-Tap Down   2        pressed
 *
 */
metadata {
	definition (name: "GE On Off", namespace: "davindameron", author: "Davin Dameron") {
		capability "Configuration"
		capability "Refresh"
		capability "Switch"

        
        command "doubleUp"
        command "doubleDown"
        
        // These include version because there are older firmwares that don't support double-tap or the extra association groups
		fingerprint mfr:"0063", prod:"4952", model: "3036", ver: "5.22", deviceJoinName: "GE Z-Wave Plus Wall Switch"
		fingerprint mfr:"0063", prod:"4952", model: "3037", ver: "5.20", deviceJoinName: "GE Z-Wave Plus Toggle Switch"
		fingerprint mfr:"0063", prod:"4952", model: "3038", ver: "5.20", deviceJoinName: "GE Z-Wave Plus Toggle Switch"
		fingerprint mfr:"0063", prod:"4952", model: "3130", ver: "5.20", deviceJoinName: "Jasco Z-Wave Plus Wall Switch"
		fingerprint mfr:"0063", prod:"4952", model: "3131", ver: "5.20", deviceJoinName: "Jasco Z-Wave Plus Toggle Switch"
		fingerprint mfr:"0063", prod:"4952", model: "3132", ver: "5.20", deviceJoinName: "Jasco Z-Wave Plus Toggle Switch"
	}

	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"

		// reply messages
		reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
		reply "200100,delay 5000,2602": "command: 2603, payload: 00"
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
	                "always": "Always On",
                	"never": "Never On"], title: "Indicator", defaultVale:"whenOff",required:true)

    }

	tiles(scale:2) {
		standardTile("switch", "device.switch", decoration: "flat", width: 3, height: 2, canChangeIcon: true) {
		    state "off", label:'${name}', action: "switch.on", icon: "st.switches.switch.on", backgroundColor:"#ffffff"
		    state "on", label:'${name}', action: "switch.off", icon: "st.switches.switch.off", backgroundColor:"#00a0dc"
		}        

		standardTile("refresh", "device.switch", width: 3, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("doubleUp", "device.switch", width: 3, height: 2, decoration: "flat") {
				state "default", label: "Tap ??", backgroundColor: "#ffffff", action: "doubleUp", icon: "https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOnIcon.png"
		}     

		standardTile("doubleDown", "device.switch", width: 3, height: 2, decoration: "flat") {
				state "default", label: "Tap ??", backgroundColor: "#ffffff", action: "doubleDown", icon: "https://raw.githubusercontent.com/nuttytree/Nutty-SmartThings/master/devicetypes/nuttytree/SwitchOffIcon.png"
		} 
		main(["switch"])
		details(["switch", "refresh", "doubleUp", "doubleDown"])
	}
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
    doLogging "description: $description"
    def result = null
    def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x56: 1, 0x70: 2, 0x72: 2, 0x85: 2])
    if (cmd) {
        result = zwaveEvent(cmd)
        doLogging "Parsed ${cmd} to ${result.inspect()}"
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
		return zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    doLogging "---BASIC REPORT V1--- ${device.displayName} sent ${cmd}"
	createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "physical")
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    doLogging "---BASIC SET V1--- ${device.displayName} sent ${cmd}"
	if (cmd.value == 255) {
    	createEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "Double-tap up (button 1) on $device.displayName", isStateChange: true, type: "physical")
    }
	else if (cmd.value == 0) {
    	createEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "Double-tap down (button 2) on $device.displayName", isStateChange: true, type: "physical")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	doLogging "---ASSOCIATION REPORT V2--- ${device.displayName} sent groupingIdentifier: ${cmd.groupingIdentifier} maxNodesSupported: ${cmd.maxNodesSupported} nodeId: ${cmd.nodeId} reportsToFollow: ${cmd.reportsToFollow}"
    if (cmd.groupingIdentifier == 3) {
    	if (cmd.nodeId.contains(zwaveHubNodeId)) {
        	createEvent(name: "numberOfButtons", value: 2, displayed: false)
        }
        else {
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()))
			sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationGet(groupingIdentifier: 3).format()))
        	createEvent(name: "numberOfButtons", value: 0, displayed: false)
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    doLogging "---CONFIGURATION REPORT V2--- ${device.displayName} sent ${cmd}"
	def name = ""
    def value = ""
    def reportValue = cmd.configurationValue[0]
    switch (cmd.parameterNumber) {
        case 3:
            value = reportValue == 1 ? "whenOn" : reportValue == 2 ? "never" : reportValue == 3 ? "always" : "whenOff"
            settings.indicator = value
            doLogging "indicator [${value}]"
            break
        case 4:
            value = reportValue == 1 ? "true" : "false"
            settings.inverted = value
            doLogging "inverted [${value}]"
            break
        default:
            break
    }
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    doLogging "---BINARY SWITCH REPORT V1--- ${device.displayName} sent ${cmd}"
    createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
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
    createEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
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
    
    // Add the hub to association group 3 to get double-tap notifications
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 3).format()
    
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
        case "always":
        	cmds << zwave.configurationV2.configurationSet(configurationValue: [3], parameterNumber: 3, size: 1)
            	break
        }
        if(settings.inverted=="true"){
        	cmds << zwave.configurationV2.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1)
        }
        else{
        	cmds << zwave.configurationV2.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1)
        }

	sendHubCommand(cmds.collect{ new physicalgraph.device.HubAction(it.format()) }, 500)
}


def doubleUp() {
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "Double-tap up (button 1) on $device.displayName", isStateChange: true, type: "digital")
}

def doubleDown() {
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "Double-tap down (button 2) on $device.displayName", isStateChange: true, type: "digital")
}


def refresh() {
	def cmds = []
	cmds << zwave.switchBinaryV1.switchBinaryGet().format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 3).format()
    cmds << zwave.configurationV2.configurationGet(parameterNumber: 4).format()
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 3).format()
	if (getDataValue("MSR") == null) {
		cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	}
	delayBetween(cmds,500)
}

def on() {
	doLogging "Turning On"
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	], 100)
}

def off() {
	doLogging "Turning Off"
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format()
	], 100)
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
