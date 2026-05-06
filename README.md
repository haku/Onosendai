Onosendai - A Better Deck
=========================

A multi-column social network client.

![Onosendai Phone and Tablet UI](http://vaguehope.com/uploads/onoseldai-2013-05.png "Onosendai Phone and Tablet UI")

Features
--------

* Multiple accounts.
* Timelines, mentions, lists and searches can all be columns.
* Adaptive layout: tablets show multiple columns at once.
* De-duplicate: filter the items in one column out of another.
* Read Later column: any tweet can be saved locally for reading later.
* Background feed fetching and caching.
* Preemptive background image fetching (handy when out of network range).
* Posts sent via local outbox - automatically retried when network access is unreliable.

Supported Services
------------------

* Read-only Mastodon support, write still needs implementing.

Background Tasks
----------------

Note that background tasks will run based on battery level.

| Task                          | Min battery |
| ----------------------------- | ----------- |
| Send outbox                   | 15%         |
| Fetch tweets                  | 30%         |
| Prefetch pics and link titles | 30%         |
| Clean temp files              | 50%         |

### Background refreshing

The background refresh service will check approximately every 15 minutes for columns that need refreshing.
Keep this in mind when choosing refresh times.  The following are examples of valid values:
`15min`, `30mins`, `1hour`, `3hours`, `2hour30mins`.

License
-------
This source code is made available under the Apache 2 licence.
This copy of the source code should also contain LICENCE and NOTICE files which contain the full licence terms copyright notices respectfully.
The full licence can also be found here: http://www.apache.org/licenses/LICENSE-2.0

Building from source
--------------------

This code is build using Maven.

Maven and Android SDK setup (on Ubuntu).
Download the sdkmanager from https://developer.android.com/studio under "Command line tools only".
```sh
$ sudo apt install openjdk-8-jdk maven

$ mkdir ~/opt/android-home
$ mkdir ~/opt/android-home/cmdline-tools
$ cd ~/opt/android-home/cmdline-tools
$ unzip ~/Downloads/commandlinetools-linux-6609375_latest.zip

$ export ANDROID_HOME=~/opt/android-home
$ $ANDROID_HOME/cmdline-tools/tools/bin/sdkmanager --list
$ $ANDROID_HOME/cmdline-tools/tools/bin/sdkmanager "platform-tools"
$ $ANDROID_HOME/cmdline-tools/tools/bin/sdkmanager "build-tools;30.0.1"
$ $ANDROID_HOME/cmdline-tools/tools/bin/sdkmanager "platforms;android-19"
$ $ANDROID_HOME/cmdline-tools/tools/bin/sdkmanager "platforms;android-21"
```

Use [maven-android-sdk-deployer](https://github.com/mosabua/maven-android-sdk-deployer) to get android.jar.
```sh
$ git clone https://github.com/simpligility/maven-android-sdk-deployer.git
$ cd maven-android-sdk-deployer
$ mvn install -P 5.0
$ ls ~/.m2/repository/android/android/5.0.1_r2/android-5.0.1_r2.jar
```

Build the project:
```sh
mvn clean package android:deploy android:run
```

The APK can be made smaller using ProGuard.  This project is configured to only shrink the output, not obfuscate.  Use this to make a release build.

```sh
mvn clean install -P release
```

Logging
-------

Logcat via ADB:
```sh
adb logcat -s "onosendai:I"
```

Run these commands on the device either via a local shell (e.g. Connect Bot) or `adb shell`.

Capture last 10000 lines of Onosendai's log at INFO level to a file with:
```sh
logcat -b main -t 10000 -f /sdcard/onosendai.log -v time -s onosendai:I
```

Enable debug level logging for Onosendai with:
```sh
setprop log.tag.onosendai DEBUG
```

Apology
-------
Some of the icons use Kanji characters.
They are intended to be visually distinct and not to convey any specific meaning.
Apologies for any nonsense or unintended meaning.
