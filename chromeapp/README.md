#  Docker UI using Chrome app


## Development

### Compile Scala.js

```
export SBT_OPTS="-Xmx1G"
sbt chromeapp/fullOptJS
```


### Download dependencies

````
cd chromeapp
bower install
```
