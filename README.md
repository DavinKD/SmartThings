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

I.  Installation<br>
  A.  Device Handlers<br>
    1.  Log into SmartThings IDE<br>
    2.  Go to My Device Handlers<br>
    3.  Click Settings<br>
    4.  Click Add new repository<br>
        a.  Owner - DavinKD<br>
        b.  Name - SmartThings<br>
        c.  Branch - master<br>
    5.  Click Save<br>
    6.  Click Update from Repo<br>
    7.  Select SmartThings (master)<br>
    8.  In the New column select all the device handlers you want.<br>
    9.  Put a checkmark in the Publish field<br>
    10. Click Execute update<br>
  B.  Add new devices<br>
    1.  In the IDE go to My Device<br>
    2.  Click +New Device<br>
        a.  Name - Anything you want<br>
        b.  Label - Anything you want<br>
        c.  Zigbee Id - Leave blank<br>
        d.  Device Network Id - Enter something unique for each device<br>
        e.  Type - Select the appropriate device handle added in I.A. (they will be at the bottom of the list)<br>
        f.  Version - Published<br>
        g.  Location - Pick your location<br>
        h.  Hub - Select your hub<br>
        i.  Group - Optional<br>
    3.  Click Create<br>
II. Device Settings<br>
    1.  After installing the device you can pull them up in the SmartThings mobile app and edit the settings.  Please note that not all devices have the same settings<br>
        a.  IP Address - The IP address of the device.<br>
        b.  Power Channel - This is the power channel in Tasmota, this is usaually 1, but if you have devices with multiple relays, etc, it can be a different number<br>
        c.  LED 1-3 Channel - If you've assigned a seperate relay to a LED on the device enter it's number.  Enter 0 if you did not assign relays to the LEDs and are instead using the built in Tasmota LED functionality<br>
        d.  Turn on LED 1-3 with switch - Yes/No to tell the device handler you want to turn LEDs on/off when the main power is turned on/off<br>
        e.  USE MQTT for Commands - If turned on, any commands will be sent to my custom Web service which forwards the commands to your MQTT broker.  This is optional<br>
        f.  Use MQTT for Updates - If turned on, the device handler will be expecting my MQTT broker to send "Execute" commands through the SmartThings API whenever the devices send updates to the MQTT broker.  This is required if you want real-time status updates for actions done at the device itself.  Otherwise, this is optional<br>
        g.  MQTT Proxy Web Server - This is the IP address of the web server that send commands to MQTT (see Use MQTT for Commands)<br>
        h.  MQTT Topic - MQTT Topic as defined in Tasmota<br>
        i.  Turn on debug logging - If on, logging will show up in the SmartThings IDE.  If off, all but errors will be supressed.<br>
        j.  Username - Optional - If your tasmota web interface ie password protected it will be required.<br>
        k.  Password - Optional - If your tasmota web interface ie password protected it will be required.<br>
    
    
    


