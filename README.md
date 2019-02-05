# SmartThings
SmartThings Stuff

TMR Life Plugs
https://smile.amazon.com/gp/product/B0786L8TC1/ref=oh_aui_search_asin_title?ie=UTF8&psc=1

Because of security concerns with the Tuya App that normally runs these plugs, I wanted to flash them and see if I could get them to work directly in SmartThings without IFTTT integration.

This is based off work from lots of others.  

First you need to reflash the device.  I used this method.
https://github.com/ct-Open-Source/tuya-convert

There's a Youtube Video with instructions
https://www.youtube.com/watch?v=O5GYh470m5k

I then used this video to help me figure out the IO assignments
https://www.youtube.com/watch?v=m_O24tTzv8g

My device handlers are based off
https://github.com/BrettSheleski/SmartThingsPublic/tree/master/devicetypes/brettsheleski/tasmota.src

I chose to make seperate device handlers instead of using child devices.  Just preference.

TMRLifeFull.groovy - This combines both the plug control and control of the RGB ring light in one device.

TMRLifePlug.groovy - This controls just the plug

TMRLifeRingLIght - This controls just the RGB ring lights

You can have more than one device in SmartThings for each plug.  When adding the device in the SmartThings IDE, simply edit the preferences to the IP address assigned to the device.

With slight changes, I'm sure this could be used for many other devices flashed the same way.  See the tuya-convert wiki regarding which devices have been successfully flashed.
