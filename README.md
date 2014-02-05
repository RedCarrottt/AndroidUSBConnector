AndroidUSBConnector
===================

AndroidUSBConnector is a abstracted code and sample code about connection between host PC and Android target device. It is composed of host client and Android server app. It uses ADB for USB communication additionally.

Why Abstraction?
===================
ADB's port-forwarding function allows host PC only act as client, not server. In this reason, sending a message from host PC to client is easy to implement, but the reverse is not. AndroidUSBConnector allows both host PC and target Android device listen and send messages each other. It also makes sensing target device's connection easier.

Components
===================
* Sample client: GUI application running on host PC (it uses swing)
* Sample server: Android application

Prerequisite
===================
* ADB binary on host PC
* JDK/JRE on host PC

How to use it in your code
===================
You can pick USBConnector and ADBConnector and use it in your code. As you can see in this sample, server on target device uses only USBConnector, and client on host PC uses both USBConnector and ADBConnector. ADBConnector senses connection with target device and opens a port to target device through ADB's port-forwarding function.

Watch out!
===================
* Due to limitation of ADB port-forwarding, server can get messages from client by only polling. You should send a dummy message to client periodically for getting messages from client.
* Send buffers of USBConnectors stores messages and USBConnectors send messages in one string which can be split by '\n'. Splitting messages is your charge. You can change this split character.
