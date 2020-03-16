metadata {
    definition(
        name: "Fan Thermostat",
        namespace: "mbudnek",
        author: "Miles Budnek",
    ) {
        capability "Actuator"
        capability "Switch"
        capability "FanControl"
        capability "ThermostatCoolingSetpoint"
        capability "ThermostatSetpoint"
        capability "TemperatureMeasurement"
        capability "ThermostatMode"

        command "setManualOverride", ["number"]
        command "clearManualOverride"

        attribute "manualOverride", "enum", ["active", "inactive"]
        attribute "defaultManualOverrideTime", "number"
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    setThermostatMode("cool")
    if (currentThermostatSetpoint == null) {
        setCoolingSetpoint(78)
    }
}

def parse(command) {
    def parts = command.split(" ")
    def attr = parts[0]
    def value = parts[1]

    if (attr == "speed") {
        if (value == "off") {
            state.lastSpeed = device.currentSpeed
            sendEvent(name: "switch", value: "off")
        } else {
            if (value == "on" && state.lastSpeed) {
                value = state.lastSpeed
            }
            sendEvent(name: "switch", value: "on")
        }
    }
    sendEvent(name: attr, value: value)
}

def on() {
    parent.on()
}

def off() {
    parent.off()
}

def setSpeed(speed) {
    parent.setSpeed(speed)
}

def setCoolingSetpoint(temperature) {
    sendEvent(name: "thermostatSetpoint", value: temperature)
    sendEvent(name: "coolingSetpoint", value: temperature)
}

def clearManualOverride() {
    unschedule("clearManualOverride")
    sendEvent(name: "manualOverride", value: "inactive")
}

def setManualOverride(overrideSeconds=null) {
    if (overrideSeconds == null) {
        overrideSeconds = settings.defaultManualOverrideTime
    }
    if (overrideSeconds) {
        sendEvent(name: "manualOverride", value: "active")
        runIn(overrideSeconds, "clearManualOverride")
    }
}

def setThermostatMode(mode) {
    sendEvent(name: "thermostatMode", value: "cool")
}

def cool() {
    setThermostatMode("cool")
}

// These are just here to fulfill the contract for the ThermostatMode capability
// These modes aren't actually supported, nor do them make sense for this device
def auto() {
    setThermostatMode("auto")
}

def heat() {
    setThermostatMode("heat")
}

def emergencyHeat() {
    setThermostatMode("emergency heat")
}
