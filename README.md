# SmartThings
SmartThings Stuff

Because of security concerns with the Tuya App that normally runs the wifi devices I have, I wanted to flash them and see if I could get them to work directly in SmartThings without IFTTT integration.

This is based off work from lots of others.  

First you need to reflash the device.  I used this method.
https://github.com/ct-Open-Source/tuya-convert

There's a Youtube Video with instructions
https://www.youtube.com/watch?v=O5GYh470m5k

I then used this video to help me figure out the IO assignments
https://www.youtube.com/watch?v=m_O24tTzv8g

Lots of devices have premade templates
https://blakadder.github.io/templates/

My device handlers are based off
https://github.com/BrettSheleski/SmartThingsPublic/tree/master/devicetypes/brettsheleski/tasmota.src

I chose to make seperate device handlers instead of using child devices.  Just preference.  Device handlers are in devicetypes/davindameron

tasmota-energy-monitoring-plug - This device handler supports plugs that do energy monitoring

tasmota-multi-outlet - Devices with more than one outlet (relay).  You can of course just use one of the other device handlers to control them individually.

tasmota-plug - Devices without energy monitoring

tasmota-rgb-bulb - For rgb devices

tasmota-rgbw-bulb - For rgbw devices

tasmota-sprinker-controller - For my custom sprinkler controller

tasmota-temperature - For my custom pool temperature probe


MQTT Bridge
https://github.com/DavinKD/SmartThings/blob/master/SmartThingsMQTT.zip

**Updated 2/22/2020
Updated to use latest mqttnet.dll along with changing a bunch of methods to work async.


Most people use HomeAssistant with the Mosquitto MQTT broker.  However, since I'm more of a Microsoft .net kind of person, I wrote my own Windows Service MQTT bridge using mqttnet (https://github.com/chkr1011/MQTTnet).  It's a one-way MQTT service which the devices connect to.  When the service received a message from the device, it forwards it to SmartThings via their API.  To control the devices, my SmartThings device handlers just send commands over http.  The device handlers will work without a broker, but status of events happening physically at the device will be delayed as it only polls once every 5 minutes.

There are 2 config files.

deviceList.cfg - List of devices and their corresponding SmartThings device IDs (guids)
SmartThingsMQTT.cfg - Holds your API key for the SmartThings API

You can have more than one device in SmartThings for each Tasmota device.  When adding the device in the SmartThings IDE, simply edit the preferences to the IP address assigned to the device.

With slight changes, I'm sure this could be used for many other devices flashed the same way.  See the tuya-convert wiki regarding which devices have been successfully flashed.


