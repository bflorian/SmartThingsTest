metadata {
	// Automatically generated. Make future change here.
	definition (name: "ZigBee Dimmer", namespace: "smartthings", author: "SmartThings") {
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
		capability "Configuration"
		capability "Sensor"
		capability "Refresh"

		fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,0B05", outClusters: "0019"
	}

	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"

		// reply messages
		reply "zcl on-off on": "on/off: 1"
		reply "zcl on-off off": "on/off: 0"
	}

	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
			state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
			state "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
			state "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
			state "level", action:"switch level.setLevel"
		}
        valueTile("level", "device.level", inactiveLabel: false, decoration: "flat") {
			state "level", label:'${currentValue} %', unit:"%", backgroundColor:"#ffffff"
		}
		main "switch"
		details(["switch", "refresh", "level", "levelSliderControl"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.info description
	if (description?.startsWith("catchall:")) {
		def msg = zigbee.parse(description)
		log.trace msg
		log.trace "data: $msg.data"
	}
	else {
		def name = description?.startsWith("on/off: ") ? "switch" : null
		def value = name == "switch" ? (description?.endsWith(" 1") ? "on" : "off") : null
		def result = createEvent(name: name, value: value)
		log.debug "Parse returned ${result?.descriptionText}"
		return result
	}
}

// Commands to device
def on() {
	log.debug "on()"
	sendEvent(name: "switch", value: "on")
	"st cmd 0x${device.deviceNetworkId} ${endpointId} 6 1 {}"
}

def off() {
	log.debug "off()"
	sendEvent(name: "switch", value: "off")
	"st cmd 0x${device.deviceNetworkId} ${endpointId} 6 0 {}"
}
def setLevel(value) {
	log.trace "setLevel($value)"
	def cmds = []

	if (value == 0) {
		sendEvent(name: "switch", value: "off")
		cmds << "st cmd 0x${device.deviceNetworkId} ${endpointId} 6 0 {}"
	}
	else if (device.latestValue("switch") == "off") {
        sendEvent(name: "switch", value: "on")
        cmds << "st cmd 0x${device.deviceNetworkId} ${endpointId} 6 1 {}"
        
	}

	sendEvent(name: "level", value: value)
	def level = new BigInteger(Math.round(value * 255 / 100).toString()).toString(16)
	cmds << "st cmd 0x${device.deviceNetworkId} ${endpointId} 8 4 {${level} 0000}"

	//log.debug cmds
	cmds
}

def refresh() {
	[
		"st wattr 0x${device.deviceNetworkId} 1 6 0", "delay 200",
		"st wattr 0x${device.deviceNetworkId} 1 8 0"
	]
}

def configure() {

	/*log.debug "binding to switch and level control cluster"
	[
		"zdo bind 0x${device.deviceNetworkId} 1 1 6 {${device.zigbeeId}} {}", "delay 200",
		"zdo bind 0x${device.deviceNetworkId} 1 1 8 {${device.zigbeeId}} {}"
	]
    */

	//set transition time to 2 seconds. Not currently working.
	"st wattr 0x${device.deviceNetworkId} 1 8 0x10 0x21 {1400}"
}



private hex(value, width=2) {
	def s = new BigInteger(Math.round(value).toString()).toString(16)
	while (s.size() < width) {
		s = "0" + s
	}
	s
}

private getEndpointId() {
	new BigInteger(device.endpointId, 16).toString()
}
