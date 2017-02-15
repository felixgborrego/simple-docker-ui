# Native Docker UI using Electron

** Note: This is a prototype and a work in progress**

## Known limitations/bugs


## Development

### Download Node.js & Electron dependencies
````
cd electron
npm install
bower install
```

### Compile Scala.js

```
export SBT_OPTS="-Xmx1G"
sbt electron/fullOptJS
```

### Run the Electron UI
```
cd electron
npm start
```

### Package the native app for Mac

```
cd electron
npm run-script package-mac
npm run-script dmg
```

###Package Native app for Windows
Require Wine 
brew install wine 
brew install mono

```
cd electron
npm run-script package-exe
npm run-script create-installer-win
``` 

##Package Native app for Linux
```
cd electron
brew install fakeroot dpkg
npm run-script package-linux
npm run-script create-installer-debian
```