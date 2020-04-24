# Fan Thermostat

This app allows you to create virtual thermostat devices to control ceiling fans based on the temperature.

## Installation

This app comes in three parts: a parent app, a child app, and a device driver.
To install:
1. Navigate to "Apps Code" in Hubitat
2. Click "New Aapp"
3. Paste the code from [fan-thermostat-parent.groovy](fan-thermostat-parent.groovy) into the editor and click save
4. Repeat steps 2 and 3 for [fan-thermostat-child.groovy](fan-thermostat-child.groovy)
5. Navigate to "Drivers Code"
6. Click "New Driver"
7. Paste the code from [fan-thermostat-device.groovy](fan-thermostat-device.groovy) into the editor and click save
8. Navigate to "Apps"
9. Click "Add User App" and select "Fan Thermostat" from the dialog that pops up

## Creating Devices

In the main preferences page for the Fan Thermostat app there is an "Add Fan Thermostat" button.  Click it to create a new Fan Thermostat instance.

Each Fan Thermostat instance has the following configuration parameters:

* Name: The name of this instance.  A virtual thermostat with this name will be created for this instance.
* Temperature Sensors: The sensors used to determine the temperature for this thermostat.
    * Temperature Sensor Operation: If more than one temperature sensor is selected then you must specify how to combine their individual readings to get a temperature.  Options are "Minimum", "Maximum", or "Average".
* Fan Controllers: Multi-speed fan controller devices to control.
    * Participating Speeds: The fan speed settings that this thermostat will set the selected controllers to.  Select the speed settings that your fan controller supports.
    * Temperature Step: The number of degrees over the thermostat setpoint at which the thermostat will increase the fan speed to the next setting.
* Switches: On/Off switches for this thermostat to control
* Motion Sensors: If selected, the thermostat will turn the fan on only when at leaste one of these sensors is active and will turn the fan off when all of them become inactive.
    * Motion Timout: The amount of time, in seconds, that fans will remain on after all selected motion sensors become inactive.
* Retrigger Time: The amount of time, in seconds, before the thremostat will turn the fan back on after turning it off.
* Manual Override Time: The amount of time, in seconds, before the thermostat will resume controlling the fan after it is controlled manually (by using a wall switch or voice control, for example).

## Usage

Each Fan Thermostat instance will create a virtual Fan Thermostat device.  That device acts as both a fan controller and a thermostat.  They provide the following commands:

* On - Turn the linked fans on
* Off - Turn the linked fans off
* Set Speed - Set the linked fans to the specified speed.  Linked On/Off switches will be turned on for any speed other than "off".
* Set Level - Set the linked fans' speed based on level. The range is split into a number of equal-sized ranges that each set the fan to a given speed.  For example, for a 3-speed fan, level 1-33 will set the fan to low, 34-66 to medium and 67-99 high.
* Set Cooling Setpoint - Set the temperature at which the linked fans will be turned on automatically
* Set Manual Override - Disable thermostat control of the linked fans for the specified time, in seconds
* Clear Manual Override - Resume thermostat control of the linked fans if a manual override was active
* Set Thermostat Mode - Set the mode of the thermostat.  Only the "off" and "cool" modes are supported.  When the mode is set to "off", the thermostat will not control of linked fans; control will be fully manual.

The "Auto", "Heat", and "Emergency Heat" commands are provided to fulfil the requirements for the "ThermostatMode" capability, but the only supported modes are "off" and "cool".
