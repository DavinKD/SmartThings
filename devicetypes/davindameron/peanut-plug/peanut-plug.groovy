/**
 *  Peanut Plug
 *
 *  Copyright 2017 pakmanw@sbcglobal.net, Davin Dameron
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
 *  Peanut Plug
 *
 *  Author:  Davin Dameron 
 *  based on work by: pakmanw@sbcglobal.net
 *
 *  Change Log
 *  2017-09-17 - v01.01 Created
 *  2018-03-01 - v01.02 fix power accuracy issue
 *  2018-12-23 - v01.03 merging jamesham change to get the calibrated attr from peanut plug,
 *                      add support for new smartthings app
 *  2019-01-17 - v01.04 merging jamesham retain state code
 *  2020-01-22 - Updated to match my style and debug logging logic change
 */

import physicalgraph.zigbee.zcl.DataType

metadata {
	definition (name: "Peanut Plug", namespace: "davindameron", author: "Davin Dameron", ocfDeviceType: "oic.d.switch",
		vid: "generic-switch-power-energy") {
		capability "Energy Meter"
		capability "Switch"
		capability "Power Meter"
		capability "Polling"
		capability "Refresh"
		capability "Configuration"
		capability "Sensor"
		capability "Health Check"
		capability "Voltage Measurement"
        
		attribute "current","number"

		command "reset"
       
		fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0004, 0005, 0006, 0B04, 0B05",
			outClusters: "0000, 0001, 0003, 0004, 0005, 0006, 0019, 0B04, 0B05"
	}

	// tile definitions
	tiles {
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
	valueTile("voltage", "device.voltage", decoration: "flat", width: 3, height: 3) {
		state "default", label:'${currentValue} V'
	}
	valueTile("current", "device.current", decoration: "flat", width: 3, height: 3) {
		state "default", label:'${currentValue} A'
	}
	standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat", width: 3, height: 3) {
		state "default", label:'reset kWh', action:"reset"
	}
	standardTile("refresh", "device.switch", width: 3, height: 3, inactiveLabel: false, decoration: "flat") {
			state "default", label:'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main(["switch","power","energy","voltage","current"])
		details(["switch","power","energy","voltage","current","refresh","reset"])
	}

	preferences {
		input(name: "retainState", type: "bool", title: "Retain State?", description: "Retain state on power loss?", required: false, displayDuringSetup: false, defaultValue: true)
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


def parse(String description) {

	doLogging "description is $description"
	def event = zigbee.getEvent(description)
	if (event) {
		if (event.name == "power") {
			def powerValue
			powerValue = (event.value as Integer) * getPowerMultiplier()
			sendEvent(name: "power", value: powerValue)
			def time = (now() - state.time) / 3600000 / 1000
			state.time = now()
			doLogging "powerValues is $state.powerValue"
			state.energyValue = state.energyValue + (time * state.powerValue)
			state.powerValue = powerValue
			// doLogging "energyValue is $state.energyValue"
			sendEvent(name: "energy", value: state.energyValue)
		} else {
			sendEvent(event)
		}
	} else if (description?.startsWith("read attr -")) {
		def descMap = zigbee.parseDescriptionAsMap(description)
		doLogging "Desc Map: $descMap"
		if (descMap.clusterInt == zigbee.ELECTRICAL_MEASUREMENT_CLUSTER) {
			def intVal = Integer.parseInt(descMap.value,16)
			if (descMap.attrInt == 0x0600) {
				doLogging "ACVoltageMultiplier $intVal"
				state.voltageMultiplier = intVal
			} else if (descMap.attrInt == 0x0601) {
				doLogging "ACVoltageDivisor $intVal"
				state.voltageDivisor = intVal
			} else if (descMap.attrInt == 0x0602) {
				doLogging "ACCurrentMultiplier $intVal"
				state.currentMultiplier = intVal
			} else if (descMap.attrInt == 0x0603) {
				doLogging "ACCurrentDivisor $intVal"
				state.currentDivisor = intVal
			} else if (descMap.attrInt == 0x0604) {
				doLogging "ACPowerMultiplier $intVal"
				state.powerMultiplier = intVal
			} else if (descMap.attrInt == 0x0605) {
				doLogging "ACPowerDivisor $intVal"
				state.powerDivisor = intVal
			} else if (descMap.attrInt == 0x0505) {
				def voltageValue = intVal * getVoltageMultiplier()
				doLogging "Voltage ${voltageValue}"
				state.voltage = $voltageValue
				sendEvent(name: "voltage", value: voltageValue)
			} else if (descMap.attrInt == 0x0508) {
				def currentValue = intVal * getCurrentMultiplier()
				doLogging "Current ${currentValue}"
				state.current = $currentValue
				sendEvent(name: "current", value: currentValue)
			}
		} else {
			doLogging "Not an electrical measurement"
		}
	} else {
		doLogging "DID NOT PARSE MESSAGE for description : $description"
		doLogging zigbee.parseDescriptionAsMap(description)
	}
}

def installed() {
	reset()
	configure()
	refresh()
}

def off() {
	zigbee.off()
}

def on() {
	zigbee.on()
}

def refresh() {
	Integer reportIntervalMinutes = 5
	setRetainState() +
	zigbee.onOffRefresh() +
	zigbee.simpleMeteringPowerRefresh() +
	zigbee.electricMeasurementPowerRefresh() +
	zigbee.onOffConfig(0, reportIntervalMinutes * 60) +
	zigbee.simpleMeteringPowerConfig() +
	zigbee.electricMeasurementPowerConfig() +
	voltageMeasurementRefresh() +
	voltageMeasurementConfig() +
	currentMeasurementRefresh() +
	currentMeasurementConfig() +
	zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x0600) +
	zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x0601) +
	zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x0602) +
	zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x0603) +
	zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x0604) +
	zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x0605)
}

def currentMeasurementConfig(minReportTime=1, maxReportTime=600, reportableChange=0x0030) {
	zigbee.configureReporting(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x0508, DataType.UINT16, minReportTime, maxReportTime, reportableChange)
}

def currentMeasurementRefresh() {
	zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x0508);
}

def voltageMeasurementConfig(minReportTime=1, maxReportTime=600, reportableChange=0x0018) {
	zigbee.configureReporting(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x0505, DataType.UINT16, minReportTime, maxReportTime, reportableChange)
}

def voltageMeasurementRefresh() {
	zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, 0x0505);
}

def getCurrentMultiplier() {
	if (state.currentMultiplier && state.currentDivisor) {
		return (state.currentMultiplier / state.currentDivisor)
	} else {
		return 0.001831
	}
}

def getVoltageMultiplier() {
	if (state.voltageMultiplier && state.voltageDivisor) {
		return (state.voltageMultiplier / state.voltageDivisor)
	} else {
		return 0.0045777
	}
}

def getPowerMultiplier() {
	if (state.powerMultiplier && state.powerDivisor) {
		return (state.powerMultiplier / state.powerDivisor)
	} else {
		return 0.277
	}
}

def configure() {
	doLogging "in configure()"
	return configureHealthCheck() + setRetainState()
}

def configureHealthCheck() {
	Integer hcIntervalMinutes = 12
	sendEvent(name: "checkInterval", value: hcIntervalMinutes * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
	return refresh()
}

def updated() {
	doLogging "in updated()"
	// updated() doesn't have it's return value processed as hub commands, so we have to send them explicitly
	def cmds = configureHealthCheck() + setRetainState()
	cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }
}

def ping() {
	return zigbee.onOffRefresh()
}

def setRetainState() {
	doLogging "Setting Retain State: $retainState"
	if (retainState == null || retainState) {
		if (retainState == null) {
			doLogging "retainState is null, defaulting to 'true' behavior"
		}
		return zigbee.writeAttribute(0x0003, 0x0000, DataType.UINT16, 0x0000)
	} else {
		return zigbee.writeAttribute(0x0003, 0x0000, DataType.UINT16, 0x1111)
	}
}

def reset() {
	state.energyValue = 0.0
	state.powerValue = 0.0
	state.voltage = 0.0
	state.current = 0.0
	state.time = now()
	sendEvent(name: "energy", value: state.energyValue)
}
