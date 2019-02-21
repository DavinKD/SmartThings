# SmartThings
SmartThings Stuff

TMR Life Plugs (Round RGB Night Light)
https://smile.amazon.com/gp/product/B0786L8TC1/ref=oh_aui_search_asin_title?ie=UTF8&psc=1

https://smile.amazon.com/Compatible-DAILYCOMB-Anywhere-Required-Configuration/dp/B0795VYV54/ref=pd_day0_hl_60_9?_encoding=UTF8&pd_rd_i=B0795VYV54&pd_rd_r=66369936-3623-11e9-b07b-c7bfd0064ef8&pd_rd_w=04CYk&pd_rd_wg=4slyO&pf_rd_p=ad07871c-e646-4161-82c7-5ed0d4c85b07&pf_rd_r=VHNKABFCERKPM8T98B5F&psc=1&refRID=VHNKABFCERKPM8T98B5F

https://smile.amazon.com/Compatible-Assistant-Wireless-Function-Certified/dp/B07K16SD3X/ref=pd_sbs_60_3/132-5955578-2370827?_encoding=UTF8&pd_rd_i=B07K16SD3X&pd_rd_r=66369936-3623-11e9-b07b-c7bfd0064ef8&pd_rd_w=F4j8u&pd_rd_wg=4slyO&pf_rd_p=588939de-d3f8-42f1-a3d8-d556eae5797d&pf_rd_r=VHNKABFCERKPM8T98B5F&psc=1&refRID=VHNKABFCERKPM8T98B5F

https://smile.amazon.com/EEEKit-Version-Anywhere-Wireless-Smartphone/dp/B07F597K9M/ref=pd_sbs_60_32?_encoding=UTF8&pd_rd_i=B07F597K9M&pd_rd_r=66369936-3623-11e9-b07b-c7bfd0064ef8&pd_rd_w=F4j8u&pd_rd_wg=4slyO&pf_rd_p=588939de-d3f8-42f1-a3d8-d556eae5797d&pf_rd_r=VHNKABFCERKPM8T98B5F&psc=1&refRID=VHNKABFCERKPM8T98B5F

https://smile.amazon.com/Cxy-Controlled-Household-Appliances-Assistant/dp/B077CZ85WD/ref=pd_sbs_60_47?_encoding=UTF8&pd_rd_i=B077CZ85WD&pd_rd_r=66369936-3623-11e9-b07b-c7bfd0064ef8&pd_rd_w=F4j8u&pd_rd_wg=4slyO&pf_rd_p=588939de-d3f8-42f1-a3d8-d556eae5797d&pf_rd_r=VHNKABFCERKPM8T98B5F&psc=1&refRID=VHNKABFCERKPM8T98B5F

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


SP201 Based Plugs

There's lots of these on Amazon, just shop around.  You can search for SP201 and they'll come up under many different names.  They flash pretty easily.  Be aware that the latest Tasmota has a bug that crashes when you have energy monitoring enabled with these.  I have an older .bin uploaded that you can use.  Otherwise, once they fix the bug, just use the custom template.

https://github.com/arendst/Sonoff-Tasmota/wiki/User-created-templates
