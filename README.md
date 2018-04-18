# RaspberryPi
Final assignment of the second module.

### Starting the server application on the Raspberry Pi
- Power up the Raspberry Pi.
- Wait a few seconds such that the Pi can start its server application.
- Done!

### Starting the client application
Make sure the Pi started and you are connected to its ad-hac wifi network `NedapUniverisityMN`.
- Open command prompt at the location of the `ClientApplication.jar`-file.
- Enter `java -jar ClientApplication.jar`

If the client cannot find the server, an error message is shown. The reponses from the server might be blocked by a security policy, turning it off can prevent this.
Once the client is has found the server, a menu is shown. Furthermore, is asks to set the path to the folder the application can read and write the files you want to transfer. The default path is set to the location of the application. You can maintain this path by pressing ENTER without giving any input.

#### Using the client application
There are several commands a client can make depending on its status. The user interface will guide the client such that it can use the application without consulting this file.

When a client finds the server, a menu is shown with the following commands:
- `0` to upload a file. The filename is asked next.
- `1` to download a file. The filename is asked next.
- `2` to request the list of files from the server.
- `3` to show a list of all currently active up/downloads.
- `4` to pause all currently active up/downloads.
- `5` to change the path at which files will be written to and read from. By returning no input, the path remains the same.
- `6` to exit the application.

And eventhough it is not listed, the command `HELP` will show the menu again.

Please note that the option to transfer multiple files at the same time is disabled in this version.
