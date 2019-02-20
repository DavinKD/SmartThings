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

The IO assignments I used were...

Io0=pwm1

Io2=pwm2

Io5=pwm3

Io13=button1n

Io15=relay1

My device handlers are based off
https://github.com/BrettSheleski/SmartThingsPublic/tree/master/devicetypes/brettsheleski/tasmota.src

I chose to make seperate device handlers instead of using child devices.  Just preference.

TMRLifeFull.groovy - This combines both the plug control and control of the RGB ring light in one device.

TMRLifePlug.groovy - This controls just the plug

TMRLifeRingLIght - This controls just the RGB ring lights

You can have more than one device in SmartThings for each plug.  When adding the device in the SmartThings IDE, simply edit the preferences to the IP address assigned to the device.

With slight changes, I'm sure this could be used for many other devices flashed the same way.  See the tuya-convert wiki regarding which devices have been successfully flashed.


RGBW Bulbs

I've been able to flash 2 different kinds of RGBW bulbs.

https://smile.amazon.com/gp/product/B07H258JCL/ref=ppx_yo_dt_b_asin_title_o01__o00_s00?ie=UTF8&psc=1

https://smile.amazon.com/gp/product/B07544GPWR/ref=oh_aui_search_asin_title?ie=UTF8&psc=1

The Kohree bulbs were easy.  Just set the module type to generic and use the following assignments.

GIO04 - PWM1
GIO12 - PWM2
GIO14 - PWM3
GIO13 - PWM4
GIO05 - PWM5

In SmartThings use the Tasmota RGBW Bulb device handler I posted here.  Just assign the IP address and optionally the LoopRate (Speed of the color loop).

The Lohas bulbs were way more difficult.  The LEDs are not connected directly to the ESP chip.  Instead they use 2 MY9231 chips.  In the latest Tasmota you can use the AILight module profile, but you'll need to compile a custom version to fix the color channels as they are in a different order from the AILight.  Channel1=Red, Channel2=Green, Channel3=Blue, Channel4=Cold White, Channel5=Also Cold White (wth).  The same device handler will work in SmartThings, but the color temp doesn't work.
