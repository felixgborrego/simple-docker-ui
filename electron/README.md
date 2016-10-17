# Native Docker UI using Electron

** Note: This is a prototype and a work in progress**

## Known limitations/bugs

- Work witn errors with Unix sockets : unix:///var/run/docker.sock
- Docker TLS is not supported yet.


## Development

### Download Node.js & Electron dependencies
````
cd electron
npm install
bower install
npm install electron-packager -g
```

### Compile Scala.js

```
sbt
project electron
~fullOptJS'
```

### Run the Electron UI
```
cd electron
npm start
```

### Package the native app
```
electron-packager .
```