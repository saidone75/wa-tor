# Wa-Tor

A population dynamics simulation devised by A.K. Dewdney, loosely mimicking the [Lotka–Volterra equations](https://en.wikipedia.org/wiki/Lotka%E2%80%93Volterra_equations)

Based on [the original article](https://github.com/saidone75/wa-tor/blob/master/wator_dewdney.pdf) appeared on the December 1984 issue of Scientific American

Live version [here](http://wa-tor.saidone.org)

[![wa-tor](https://i.postimg.cc/Dw2t9XQt/wa-tor.gif)](http://wa-tor.saidone.org/)

## Usage

* spacebar or "two fingers tap" to pause/resume the game
* "C" or "swipe left" to clear the board and pause the game
* "R" or "swipe right" to reset the board with the current parameters
* "H" or "swipe up" to toggle the control panel
* "S" or "long touch" (> 2s) to show statistics

When paused click on a square to cycle between water >>> fish >>> shark

From the control panel you can set the initial number of fish and sharks, their breeding thresholds, sharks lifespan without food and extra randomness for thresholds:

[![control-panel](https://i.postimg.cc/nLX8B70b/wa-tor-control-panel.gif)](http://wa-tor.saidone.org)

[![stats](https://i.postimg.cc/6QXB2gtv/wa-tor-stats.gif)](http://wa-tor.saidone.org)

## Deploy your own

Get the sources:

```$ git clone https://github.com/saidone75/wa-tor.git```

build with:

```
$ cd wa-tor
$ lein do clean, fig:min, assemble
[Figwheel] Validating figwheel-main.edn
[Figwheel] figwheel-main.edn is valid \(ツ)/
[Figwheel] Compiling build wa-tor to "resources/public/wa-tor.js"
[...]
Writing  target  ->  wa-tor-1.2-archive.tgz
Done creating assembly
```

then just copy/expand the archive into a web server

## License
Copyright (c) 2020-2021 Saidone

Distributed under the MIT License
