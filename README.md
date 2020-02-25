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
&emsp;A.  Device Handlers<br>
&emsp;&emsp;1.  Log into SmartThings IDE<br>
&emsp;&emsp;2.  Go to My Device Handlers<br>
&emsp;&emsp;3.  Click Settings<br>
&emsp;&emsp;4.  Click Add new repository<br>
&emsp;&emsp;&emsp;a.  Owner - DavinKD<br>
&emsp;&emsp;&emsp;b.  Name - SmartThings<br>
&emsp;&emsp;&emsp;c.  Branch - master<br>
&emsp;&emsp;5.  Click Save<br>
&emsp;&emsp;6.  Click Update from Repo<br>
&emsp;&emsp;7.  Select SmartThings (master)<br>
&emsp;&emsp;8.  In the New column select all the device handlers you want.<br>
&emsp;&emsp;9.  Put a checkmark in the Publish field<br>
&emsp;&emsp;10. Click Execute update<br>
&emsp;B.  Add new devices<br>
&emsp;&emsp;1.  In the IDE go to My Device<br>
&emsp;&emsp;2.  Click +New Device<br>
&emsp;&emsp;&emsp;a.  Name - Anything you want<br>
&emsp;&emsp;&emsp;b.  Label - Anything you want<br>
&emsp;&emsp;&emsp;c.  Zigbee Id - Leave blank<br>
&emsp;&emsp;&emsp;d.  Device Network Id - Enter something unique for each device<br>
&emsp;&emsp;&emsp;e.  Type - Select the appropriate device handle added in I.A. (they will be at the bottom of the list)<br>
&emsp;&emsp;&emsp;f.  Version - Published<br>
&emsp;&emsp;&emsp;g.  Location - Pick your location<br>
&emsp;&emsp;&emsp;h.  Hub - Select your hub<br>
&emsp;&emsp;&emsp;i.  Group - Optional<br>
&emsp;&emsp;3.  Click Create<br>
&emsp;&emsp;4. Note the GUID that is assigned to the device.  It will be in the URL at the top of the screen.  For example "device/show/xxxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" You just need the part after show/
&emsp;C. Install SmartThings MQTT Broker
&emsp;&emsp;1. On a Windows PC create a folder where you wish to run the broker<br>
&emsp;&emsp;2. Put the following files in the newly created folder.<br>
&emsp;&emsp;&emsp;a. SmartThingsMQTT.exe<br>
&emsp;&emsp;&emsp;b. SmartThingsMQTT.cfg<br>
&emsp;&emsp;&emsp;c. deviceList.cfg<br>
&emsp;&emsp;&emsp;d. MQTTnet.dll<br>
&emsp;&emsp;&emsp;e. Newtonsoft.Json.dll<br>
&emsp;&emsp;3. Go to https://account.smartthings.com/tokens and create a new token<br>
&emsp;&emsp;4. Edit the SmartThingsMQTT.cfg and enter the token you created.<br>
&emsp;&emsp;5. Edit the deviceList.cfg and enter the details for your devices(s).  You will need your Tasmota topic and the GUID for the SmartThings device.<br>
&emsp;&emsp;6. From a command prompt (with admin privileges) run the following.  c:\windows\microsoft.net\framework\v4.0.30319\installutil.exe c:\yourdirectory\smartThingsMQTT.exe<br>
&emsp;&emsp;7. run services.msc in Windows and verify that the SmartThingsMQTT service is running.  Start it if not.<br>
&emsp;&emsp;8. Open port 1883 on your Windows Firewall if needed.<br>
&emsp;&emsp;9. In Tasmota set your MQTT server to the IP address of your Windows PC<br>
II. Device Settings<br>
&emsp;A.  After installing the device you can pull them up in the SmartThings mobile app and edit the settings.  Please note that not all devices have the same settings<br>
&emsp;&emsp;1.  IP Address - The IP address of the device.<br>
&emsp;&emsp;2.  Power Channel - This is the power channel in Tasmota, this is usaually 1, but if you have devices with multiple relays, etc, it can be a different number<br>
&emsp;&emsp;3.  LED 1-3 Channel - If you've assigned a seperate relay to a LED on the device enter it's number.  Enter 0 if you did not assign relays to the LEDs and are instead using the built in Tasmota LED functionality<br>
&emsp;&emsp;4.  Turn on LED 1-3 with switch - Yes/No to tell the device handler you want to turn LEDs on/off when the main power is turned on/off<br>
 &emsp;&emsp;5.  USE MQTT for Commands - If turned on, any commands will be sent to my custom Web service which forwards the commands to your MQTT broker.  This is optional<br>
&emsp;&emsp;6.  Use MQTT for Updates - If turned on, the device handler will be expecting my MQTT broker to send "Execute" commands through the SmartThings API whenever the devices send updates to the MQTT broker.  This is required if you want real-time status updates for actions done at the device itself.  Otherwise, this is optional<br>
&emsp;&emsp;7.  MQTT Proxy Web Server - This is the IP address of the web server that send commands to MQTT (see Use MQTT for Commands)<br>
&emsp;&emsp;8.  MQTT Topic - MQTT Topic as defined in Tasmota<br>
&emsp;&emsp;9.  Turn on debug logging - If on, logging will show up in the SmartThings IDE.  If off, all but errors will be supressed.<br>
&emsp;&emsp;10.  Username - Optional - If your tasmota web interface ie password protected it will be required.<br>
&emsp;&emsp;11.  Password - Optional - If your tasmota web interface ie password protected it will be required.<br>
    
    
    


