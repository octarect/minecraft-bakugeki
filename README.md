# ExplodingSnowball

A [Paper](https://papermc.io/) plugin for Minecraft 1.21.4 that adds an exploding snowball item.

## Features

- Craft exploding snowballs using 4 regular snowballs
- Throw them to create a TNT-level explosion on impact

## Crafting

| Recipe | Result |
|--------|--------|
| 4× Snowball (shapeless) | 1× Exploding Snowball |

## Usage

Right-click to throw an exploding snowball. It explodes on contact with any block or entity (power 4.0, no fire, block damage enabled).

## Installation

1. Download the latest JAR from [Releases](https://github.com/octarect/minecraft-bakugeki/releases)
2. Place it in your server's `plugins/` directory
3. Restart the server

## Building

```bash
cd plugins/exploding-snowball
./gradlew shadowJar
# Output: build/libs/exploding-snowball-<version>.jar
```

Requires Java 21.

## Releases

- **Stable**: tagged releases (`v*`)
- **Nightly**: built automatically on every push to `master`, available as a pre-release
