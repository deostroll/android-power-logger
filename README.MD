# Androd Power Logger

Simple android app that logs all power interruptions. Have the phone connected to the AC charger,
and leave it on. When the power goes or comes back on, the applications logs it into google sheets.

Very useful if you have a phone lying around that you don't use that often. This project is also a
small scale IoT project. Most of the hard requirements like time keeping, internet connection, etc
are handled by the phone itself.

You need to have a phone, with average battery backup (At least, it should survive the worst power
blackout in your area). Its recommended to have the least possible number of applications running on
the phone.

This project bundles with it the android app, and, a small http server (express). The server is
purely for testing purposes only.

For this to work though, you need to deploy a google script application as a web application, give it necessary permissions so that anyone with the link can access anonymously. (Those have to be changed in the code too - it is currently hard coded to my account).
