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

**Updated 2/22/2020
Updated to use latest mqttnet.dll along with changing a bunch of methods to work async.


Most people use HomeAssistant with the Mosquitto MQTT broker.  However, since I'm more of a Microsoft .net kind of person, I wrote my own Windows Service MQTT bridge using mqttnet (https://github.com/chkr1011/MQTTnet).  It's a MQTT service which the devices connect to.  When the service received a message from the device, it forwards it to SmartThings via their API.  To control the devices, my SmartThings device handlers can either send commands over http directly to the device (requires static IP addresses) or can send to a web service I created which forwards to the MQTT broker.  The device handlers will work without a broker, but status of events happening physically at the device will be delayed as it only polls once every 5 minutes.

There are 2 config files.

deviceList.cfg - List of devices and their corresponding SmartThings device IDs (guids)
SmartThingsMQTT.cfg - Holds your API key for the SmartThings API and log level.

You can have more than one device in SmartThings for each Tasmota device.  When adding the device in the SmartThings IDE, simply edit the preferences to the IP address assigned to the device.

With slight changes, I'm sure this could be used for many other devices flashed the same way.  See the tuya-convert wiki regarding which devices have been successfully flashed.

I.  Installation
  A.  Device Handlers
    1.  Log into SmartThings IDE
    2.  Go to My Device Handlers
    3.  Click Settings
    4.  Click Add new repository
        a.  Owner - DavinKD
        b.  Name - SmartThings
        c.  Branch - master
    5.  Click Save
    6.  Click Update from Repo
    7.  Select SmartThings (master)
    8.  In the New column select all the device handlers you want.
    9.  Put a checkmark in the Publish field
    10. Click Execute update
  B.  Add new devices
    1.  In the IDE go to My Device
    2.  Click +New Device
        a.  Name - Anything you want
        b.  Label - Anything you want
        c.  Zigbee Id - Leave blank
        d.  Device Network Id - Enter something unique for each device
        e.  Type - Select the appropriate device handle added in I.A. (they will be at the bottom of the list)
        f.  Version - Published
        g.  Location - Pick your location
        h.  Hub - Select your hub
        i.  Group - Optional
    3.  Click Create
II. Device Settings
    1.  After installing the device you can pull them up in the SmartThings mobile app and edit the settings.  Please note that not all devices have the same settings
        a.  IP Address - The IP address of the device.
        b.  Power Channel - This is the power channel in Tasmota, this is usaually 1, but if you have devices with multiple relays, etc, it can be a different number
        c.  LED 1-3 Channel - If you've assigned a seperate relay to a LED on the device enter it's number.  Enter 0 if you did not assign relays to the LEDs and are instead using the built in Tasmota LED functionality
        d.  Turn on LED 1-3 with switch - Yes/No to tell the device handler you want to turn LEDs on/off when the main power is turned on/off
        e.  USE MQTT for Commands - If turned on, any commands will be sent to my custom Web service which forwards the commands to your MQTT broker.  This is optional
        f.  Use MQTT for Updates - If turned on, the device handler will be expecting my MQTT broker to send "Execute" commands through the SmartThings API whenever the devices send updates to the MQTT broker.  This is required if you want real-time status updates for actions done at the device itself.  Otherwise, this is optional
        g.  MQTT Proxy Web Server - This is the IP address of the web server that send commands to MQTT (see Use MQTT for Commands)
        h.  MQTT Topic - MQTT Topic as defined in Tasmota
        i.  Turn on debug logging - If on, logging will show up in the SmartThings IDE.  If off, all but errors will be supressed.
        j.  Username - Optional - If your tasmota web interface ie password protected it will be required.
        k.  Password - Optional - If your tasmota web interface ie password protected it will be required.
    
    
    


