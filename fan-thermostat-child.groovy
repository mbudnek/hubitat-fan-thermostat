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
// * Mar 01 2020 - Make manualOverride device command paramter optional
// * Apr 23 2020 - Add support for the "off" thermostat mode and the
//                 SwitchLevel capability
// * Aug 17 2020 - Fix broken child device initialization
// * May 17 2021 - Fix errors when using switch with no fan controller
//                 (i.e. for using with box fan on a switched outlet)

import groovy.transform.Field

definition(
    name: "Fan Thermostat Child",
    namespace: "mbudnek",
    author: "Miles Budnek",
    description: "Thermostat to control ceiling fan speed",
    parent: "mbudnek:Fan Thermostat",
    category: "",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences {
    page(name: "mainPreferences")
}

def mainPreferences() {
    return dynamicPage(name: "mainPreferences", install: true, uninstall: true) {
        section {
            input(
                name: "name",
                title: "Name",
                type: "text",
                required: true
            )
            input(
                name: "temperatureSensors",
                type: "capability.temperatureMeasurement",
                title: "Temperature Sensors",
                multiple: true,
                required: true,
                submitOnChange: true
            )
            if (settings.temperatureSensors && settings.temperatureSensors.size() > 1) {
                input(
                    name: "temperatureOp",
                    title: "Temperature Sensor Operation",
                    description: "Use this operation to determine the current temperature from multiple sensors",
                    type: "enum",
                    options: [
                        max: "Maximum",
                        min: "Minimum",
                        avg: "Average"
                    ],
                    defaultValue: "max",
                    required: true
                )
            }
            paragraph("At least one fan controller or switch must be selected")
            input(
                name: "fanControllers",
                type: "capability.fanControl",
                title: "Fan Controllers",
                multiple: true,
                submitOnChange: true,
                required: !(settings.switches as boolean)
            )
            if (settings.fanControllers) {
                input(
                    name: "fanSpeeds",
                    title: "Participating Speeds",
                    descriptions: "Speed settings that this thermostat will set the fan to",
                    type: "enum",
                    options: FAN_LEVELS,
                    required: true,
                    multiple: true
                )
                input(
                    name: "temperatureStep",
                    title: "Temperature Step",
                    description: "Increase fan speed when the temperature exceeds the set point by this many degrees",
                    type: "number",
                    required: true,
                    defaultValue: 3
                )
            }
            input(
                name: "switches",
                title: "Switches",
                type: "capability.switch",
                multiple: true,
                submitOnChange: true,
                required: !(settings.fanControllers as boolean)
            )
            input(
                name: "motionSensors",
                title: "Motion Sensors",
                type: "capability.motionSensor",
                multiple: true,
                submitOnChange: true
            )
            if (settings.motionSensors) {
                input(
                    name: "motionTimeout",
                    title: "Motion Timeout",
                    description: "Turn off fans after motion has been inactive for this many seconds",
                    type: "number",
                    required: true,
                    defaultValue: 0
                )
            }
            input(
                name: "retriggerTime",
                title: "Retrigger Time",
                description: "Don't turn fans on for this many seconds after turning them off",
                type: "number",
                required: true,
                defaultValue: 0
            )
            input(
                name: "manualOverrideTime",
                title: "Manual Override Time",
                description: "Don't control fans for this many seconds after they're controlled manually",
                type: "number",
                required: true,
                defaultValue: 0
            )
        }
    }
}

def on() {
    setManualOverride()
    setAllFans("on")
}

def off() {
    setManualOverride()
    setAllFans("off")
}

private levelToSpeed(level) {
    if (level == 0) {
        return "off"
    }

    def fanSpeeds = settings.fanSpeeds?.reverse() ?: ["on"]
    def interval = Math.round(100 / fanSpeeds.size())
    for (def i = 0; i < fanSpeeds.size(); ++i) {
        if (level > i * interval && level <= (i + 1) * interval) {
            return fanSpeeds[i]
        }
    }
}

private speedToLevel(speed) {
    if (speed == "off") {
        return 0
    }

    def speeds = settings.fanSpeeds?.reverse() ?: ["on"]
    def speedIndex = speeds.indexOf(speed)
    def levelInterval = Math.round(100 / speeds.size())
    return (levelInterval * speedIndex) + 1
}

def setLevel(level) {
    if (level == 0) {
        off()
    } else {
        setManualOverride()
        setAllFans(levelToSpeed(level), level)
    }
}

def setSpeed(speed) {
    if (speed == "on") {
        on()
    } else if (speed == "off") {
        off()
    } else {
        setManualOverride()
        setAllFans(speed, speedToLevel(speed))
    }
}

def getManualOverrideTime() {
    return settings.manualOverrideTime
}

def setManualOverrideTime(manualOverrideTime) {
    if (settings.manualOverrideTime != manualOverrideTime) {
        app.updateSetting("manualOverrideTime", manualOverrideTime)
    }
}

private controlFans() {
    def childDev = getThermostatDevice()
    if (childDev.currentManualOverride == "active" || childDev.currentThermostatMode == "off") {
        return
    }

    def setPoint = childDev.currentThermostatSetpoint
    if (childDev.currentTemperature < setPoint || !state.motionActive) {
        if (childDev.currentSpeed != "off") {
            state.lastOffTime = new Date().getTime()
            setAllFans("off")
        }
    } else {
        def now = new Date().getTime()
        def lastOffTime = state.lastOffTime
        if (lastOffTime == null) {
            lastOffTime = new Date(0).getTime()
        }
        def retriggerTime = settings.retriggerTime * 1000
        if (now - lastOffTime > retriggerTime) {
            def fanSpeeds = settings.fanSpeeds ?: ["on"]
            def tempStep = settings.temperatureStep ?: 3
            for (i = 0; i < fanSpeeds.size(); ++i) {
                def temp = setPoint + (fanSpeeds.size() - 1 - i) * tempStep
                if (childDev.currentTemperature > temp) {
                    setAllFans(fanSpeeds[i], speedToLevel(fanSpeeds[i]))
                    break
                }
            }
        }
    }
}

private setAllFans(speed, level=null) {
    def childDev = getThermostatDevice()
    childDev.parse("speed ${speed}")
    if (level != null) {
        childDev.parse("level ${level}")
    }
    settings.fanControllers?.each { fanController ->
        setDevice(fanController, speed)
    }
    // All we can do is turn these on or off
    settings.switches?.each { sw ->
        setDevice(sw, speed)
    }
}

private setDevice(device, speed) {
    if (device.hasCommand("setSpeed")) {
        device.setSpeed(speed)
    } else if (speed == "off") {
        device.off()
    } else {
        device.on()
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def uninstalled() {
    deleteChildDevice(getChildDeviceName())
}

private initialize() {
    app.updateLabel(settings.name)

    if (!state.lastChangeTimes) {
        state.lastChangeTimes = [:]
    }

    getTemperature()
    getFanSpeed()
    getMotionState()

    subscribe(getThermostatDevice(), "thermostatSetpoint", childEventHandler)
    subscribe(getThermostatDevice(), "thermostatMode", childEventHandler)
    subscribe(getThermostatDevice(), "manualOverride", childEventHandler)
    subscribe(settings.temperatureSensors, "temperature", temperatureHandler)
    if (settings.fanControllers) {
        subscribe(settings.fanControllers, "speed", fanSpeedHandler)
    }
    if (settings.switches) {
        subscribe(settings.switches, "switch", switchHandler)
    }
    if (settings.motionSensors) {
        subscribe(settings.motionSensors, "motion", motionHandler)
    }
}

def temperatureHandler(evt) {
    getTemperature()
    controlFans()
}

def motionHandler(evt) {
    getMotionState()
    controlFans()
}

def retriggerTimeoutCheck() {
    controlFans()
}

def fanSpeedHandler(evt) {
    if (evt.value == "off") {
        runIn(settings.retiggerTime, "retriggerTimeoutCheck")
    }
    def childDev = getThermostatDevice()
    if (evt.value != childDev.currentSpeed) {
        setManualOverride()
        childDev.parse("speed ${evt.value}")
    }
}

def switchHandler(evt) {
    if (evt.value == "off") {
        runIn(settings.retiggerTime, "retriggerTimeoutCheck")
    }
    def childDev = getThermostatDevice()
    def childOn = childDev.currentSpeed != "off"
    if (evt.value == "off" && childOn) {
        setManualOverride()
        childDev.parse("speed ${off}")
    } else if (evt.value == "on" && !childOn) {
        setManualOverride()
        childDev.parse("speed ${on}")
    }
}

def childEventHandler(evt) {
    controlFans()
}

private getTemperature() {
    def temperatureReadings = settings.temperatureSensors.collect { tempSensor ->
        tempSensor.currentTemperature
    }
    def temperatureOp = "max"
    if (settings.temperatureOp) {
        temperatureOp = settings.temperatureOp
    }
    def temperature = TEMPERATURE_OPERATIONS[temperatureOp](temperatureReadings)

    getThermostatDevice().parse("temperature ${temperature}")
}

private getFanSpeed() {
    def childDev = getThermostatDevice()

    def newFanSpeed = childDev.currentSpeed
    if (settings.fanControllers) {
        settings.fanControllers.each { fanController ->
            if (fanController.currentSpeed != childDev.currentSpeed) {
                setManualOverride()
                newFanSpeed = fanController.currentSpeed
            }
        }
    }
    if (settings.switches) {
        settings.switches.each { sw ->
            if (sw.currentSwitch == "off" && childDev.currentSpeed != "off") {
                setManualOverride()
                newFanSpeed = "off"
            }
        }
    }
    childDev.parse("speed ${newFanSpeed}")
}

private getMotionState() {
    def oldMotionState = true
    if (state.motionActive != null) {
        oldMotionState = state.motionActive
    }

    def motion = true
    if (settings.motionSensors) {
        motion = settings.motionSensors.any { sensor -> sensor.currentMotion == "active" }
    }

    if (motion || settings.motionTimeout == 0) {
        unschedule("setMotionInactive")
        state.motionActive = motion
    } else if (oldMotionState) {
        runIn(settings.motionTimeout, "setMotionInactive")
    }
}

private setMotionInactive() {
    state.motionActive = false
    controlFans()
}

private setManualOverride() {
    getThermostatDevice().setManualOverride()
}

private getChildDeviceName() {
    return "FanThermostat-${app.id}"
}

private getThermostatDevice() {
    def childDev = getChildDevice(getChildDeviceName())
    if (!childDev) {
        childDev = addChildDevice(
            "mbudnek",
            "Fan Thermostat",
            "FanThermostat-${app.id}",
            null,
            [
                label: settings.name,
                name: settings.name
            ]
        )
    }
    if (childDev.currentDefaultManualOverrideTime != settings.manualOverrideTime) {
        childDev.parse("defaultManualOverrideTime ${settings.manualOverrideTime}")
    }
    return childDev
}

@Field
TEMPERATURE_OPERATIONS = [
    min: { list -> list.min() },
    max: { list -> list.max() },
    avg: { list -> list.sum() / list.size() }
]

@Field
FAN_LEVELS = [
    "high":        "High",
    "medium-high": "Medium-High",
    "medium":      "Medium",
    "medium-low":  "Medium-Low",
    "low":         "Low"
]
