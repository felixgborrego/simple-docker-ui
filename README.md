## Docker UI for Chrome
 
Simple Docker UI for Chrome is an unofficial developer tool for monitoring and managing your local Docker containers.

<a target="_blank" href="https://chrome.google.com/webstore/detail/jfaelnolkgonnjdlkfokjadedkacbnib">
<img alt="Try it now" src="https://raw.github.com/GoogleChrome/chrome-app-samples/master/tryitnowbutton_small.png" title="Click here to install this app from the Chrome Web Store"></img>
</a>


This app uses Docker Remote Api.
Please note this is a beta version and only provides a basic set of features.
I'm actively working to add new features and bug fixing.
Any feedback is more than welcome!

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/felixgborrego/docker-ui-chrome-app)

![Home](docs/screenshots/home.png)

![Container](docs/screenshots/container.png)

![Image](docs/screenshots/container.png)

[Video Example](https://youtu.be/x6RVTHp5M7w)

### Main features

* Garbage collection for unused containers. Removing containers that haven't been in use for the last few days.
* Garbage collection for unused images, Keeping only the once used by a container.
* Virtual terminal to log into the container.
* Search & pull images from the Docker Hub
* Start/stop/delete containers
* List of running containers and containers history.
* See Docker events.

### Config

[Connect to Boot2Docker on OS X](https://github.com/felixgborrego/docker-ui-chrome-app/wiki/Mac-OS-X)

[Connect to Linux](https://github.com/felixgborrego/docker-ui-chrome-app/wiki/linux)

### Stack

*  [Scala.js](http://www.scala-js.org/)
*  [React on Scala.js](https://github.com/japgolly/scalajs-react)
*  Bootstrap
*  Bower
*  Momentjs
*  Chrome packaged app framework
  
