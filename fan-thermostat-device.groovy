// Copyright 2020 Miles Budnek
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

// Changelog:
// * Feb 17 2020 - Initial Release
// * Mar 01 2020 - Make manualOverride command paramter optional
// * Mar 15 2020 - Fix an issue where the device would repor its speed as "on" sometimes
// * Apr 23 2020 - Add support for the "off" thermostat mode and the
//                 SwitchLevel capability
// * Jul 17 2020 - Fix thermostat mode and setpoint reverting to default on hub reboot
// * Aug 10 2020 - Fix issue with manual override not working
// * Aug 17 2020 - Fix issue when controlling switches
// * May 04 2022 - Add support for the ThermostatOperatingState capability

metadata {
    definition(
        name: "Fan Thermostat",
        namespace: "mbudnek",
        author: "Miles Budnek",
    ) {
        capability "Actuator"
        capability "Switch"
        capability "SwitchLevel"
        capability "FanControl"
        capability "Initialize"
        capability "ThermostatCoolingSetpoint"
        capability "ThermostatSetpoint"
        capability "TemperatureMeasurement"
        capability "ThermostatMode"
        capability "ThermostatOperatingState"

        command "setManualOverride", ["number"]
        command "clearManualOverride"

        attribute "manualOverride", "enum", ["active", "inactive"]
        attribute "defaultManualOverrideTime", "number"
        attribute "thermostatOperatingState", "enum", ["cooling", "idle"]
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    sendEvent(name: "supportedThermostatModes", value: ["off", "cool"])
    if (device.currentThermostatMode == null) {
        setThermostatMode("cool")
    }
    if (device.currentThermostatSetpoint == null) {
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
            sendEvent(name: "thermostatOperatingState", value: "idle")
        } else {
            if (value == "on" && state.lastSpeed && state.lastSpeed != "off") {
                value = state.lastSpeed
            }
            sendEvent(name: "switch", value: "on")
            sendEvent(name: "thermostatOperatingState", value: "cooling")
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

def setLevel(level) {
    if (level < 0) {
        level = 0;
    } else if (level > 99) {
        level = 99
    }
    parent.setLevel(level)
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
        overrideSeconds = device.currentDefaultManualOverrideTime
    }
    if (overrideSeconds) {
        sendEvent(name: "manualOverride", value: "active")
        runIn(overrideSeconds.toLong(), "clearManualOverride")
    }
}

def setThermostatMode(mode) {
    sendEvent(
        name: "thermostatMode",
        value: mode == "off" ? "off" : "cool"
    )
}

def cool() {
    setThermostatMode("cool")
}

// These are just here to fulfill the contract for the ThermostatMode capability
// These modes aren't actually supported, nor do them make sense for this device
def auto() {
    setThermostatMode("cool")
}

def heat() {
    setThermostatMode("cool")
}

def emergencyHeat() {
    setThermostatMode("cool")
}
