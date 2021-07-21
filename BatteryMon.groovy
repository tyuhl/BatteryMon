/**
 *  Low Battery Alert Application
 *
 *  Author: Tim Yuhl
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 *  modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 *  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  History:
 *
 *  v1.0.0 - 2-Oct-2020 - Port from SmartThings, minor changes needed.
 *  v1.0.1 - 21-7-21 - Minor cosmetic fixes
 *
 */

String appVersion()   { return "1.0.1" }
def setVersion(){
	state.name = "Battery Monitor"
	state.version = "1.0.1"
}

definition(
	name: "Battery Monitor",
	namespace: "tyuhl",
	author: "Tim Yuhl",
	description: "Alert if low battery",
	importUrl: "https://raw.githubusercontent.com/tyuhl/BatteryMon/main/BatteryMon.groovy",
	category: "Convenience",
	iconUrl: "",
	iconX2Url: ""
)

preferences {
	def defaultTime = Date.parse("HHmm", "0930")
	section(getFormat("header-blue-grad","About")) {
		paragraph "This app will poll selected devices that use a battery and send an alert or SMS when the level reaches a specified threshold percentage."
		paragraph "You may configure up to four groups with different thresholds."
	}
	for (int i = 0; i < 4; i++) {
		section(getFormat("item-light-grey","Monitoring group ${i+1}")) {
			input "group_${i}", "capability.battery", title: "Select devices to monitor", multiple: true, required: false
			input "threshold_${i}", "number", title: "Notify if battery is below (%)", defaultValue: 25
		}
	}
	section(getFormat("header-blue-grad","Scheduled Time")) {
		input name: "sched_time", type: "time", title: "Time when check is done: ", defaultValue: defaultTime
	}
	section(getFormat("header-blue-grad", "Notification Device")) {
		input "sendPushMessage", "capability.notification", title: "Notification Devices: Hubitat PhoneApp or Pushover", multiple: true, required: false
	}
	section("Logging") {
		input "logEnable","bool", title: "Enable Debug Logging", description: "Debugging", defaultValue: false, submitOnChange: true
	}
}

def installed() {
	if (logEnable) log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	if (logEnable) log.debug "Updated with settings: ${settings}"
	initialize()
}

def initialize() {
	setVersion()
	unschedule()
	// Run at user specified time every day
	// Second Minute Hour DayOfMonth Month DayOfWeek Year
	// schedule("0 15 9 * * ?", check_batteries)
	schedule(settings.sched_time, check_batteries)
	check_batteries()
}

def check_batteries() {
	def size, device, threshold, value, sms
	log.info("Battery Monitor is doing daily battery check.")

	for (int i = 0; i < 4; i++) {
		size = settings["group_${i}"]?.size() ?: 0
		sms = settings."sms_${i}".toString() ?: 0
		if (size > 0) {
			threshold = settings."threshold_${i}".toInteger()
			if (logEnable) log.debug "***Checking batteries for group ${i+1} (threshold ${threshold}%)"

			for (int j = 0; j < size; j++) {
				  device = settings["group_${i}"][j]
				if (device != null) {
					value = device.currentValue("battery")
					if ((value != null) && (value < threshold)) {
						if (logEnable) log.debug "The $device battery is at ${value}%, below threshold (${threshold}%)"
						if (sendPushMessage) {
							sendPushMessage.deviceNotification("The $device battery is at ${value}%, below threshold of ${threshold}%")
						} else {
							log.info("The $device battery is at ${value}%, below threshold (${threshold}%)")
						}
					} else {
						if (logEnable) log.debug "The $device battery is at ${value}%"
					}
				}
			}
		} else {
			if (logEnable) log.debug "***Group ${i+1} has no devices (${size} devices)"
		}
	}
}

// concept stolen bptworld, who stole from @Stephack Code
def getFormat(type, myText="") {
	if(type == "header-green") return "<div style='color:#ffffff; border-radius: 5px 5px 5px 5px; font-weight: bold; padding-left: 10px; background-color:#81BC00; border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
	if(type == "header-light-grey") return "<div style='color:#000000; border-radius: 5px 5px 5px 5px; font-weight: bold; padding-left: 10px; background-color:#D8D8D8; border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
	if(type == "header-blue-grad") return "<div style='color:#000000; border-radius: 5px 5px 5px 5px; font-weight: bold; padding-left: 10px; background: linear-gradient(to bottom, #d4e4ef 0%,#86aecc 100%);  border: 2px'>${myText}</div>"
	if(type == "item-light-grey") return "<div style='color:#000000; border-radius: 5px 5px 5px 5px; font-weight: normal; padding-left: 10px; background-color:#D8D8D8; border: 1px solid'>${myText}</div>"
	if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
	if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}


