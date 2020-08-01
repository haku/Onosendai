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
* In-line image previews from Twitter, Instagram, Twitpic and Imgur.
* Preemptive background image fetching (handy when out of network range).
* Posts sent via local outbox - automatically retried when network access is unreliable.

Supported Services
------------------

* Twitter columns are supported directly.
* Very proof-of-concept Mastodon support, read-only with no formatting.
* Posting to [Buffer](https://bufferapp.com) (currently requires advanced config to add account).
* Forwarding Read Later column to [Instapaper](https://www.instapaper.com).

Background Tasks
----------------

Note that background tasks will run based on battery level.

| Task                          | Min battery |
| ----------------------------- | ----------- |
| Send outbox                   | 15%         |
| Fetch tweets                  | 30%         |
| Prefetch pics and link titles | 30%         |
| Clean temp files              | 50%         |

Advanced Configuration
----------------------

Advanced configuration (not using the UI) is available but not recommended for typical users.
It is both a legacy feature and a useful development hook when testing new features.
This is via the `deck.conf` file that lives in the root of the external storage device, typically `/sdcard/deck.conf`.
If this file does not exist a template can be created by the welcome screen for you to edit.

### Example config

**Note**: This is to show what possible.  It is not necessarily a good example of what you should put in your config.  Just saying.

**Account IDs and Feed IDs**: These are used to separate data in the DB.
The are arbitrary and do not have to be in order.
If you remove an account or column it may be best not to reuse the ID as strange things may happen.
Column IDs must be positive integers.

**SuccessWhale feeds**: Refer to [SuccessWhale docs](https://github.com/ianrenton/successwhale-api/blob/master/docs/feed-get.md) for the format of the 'resource' field.

```JSON
{
  "accounts": [
    {
      "id": "t0",
      "provider": "twitter",
      "consumerKey": "?ckey?",
      "consumerSecret": "?csecret?",
      "accessToken": "?atoken?",
      "accessSecret": "?asecret?"
    }, {
      "id": "sw0",
      "provider": "successwhale",
      "username": "?username?",
      "password": "?password?"
    }, {
      "id": "b0",
      "provider": "buffer",
      "accessToken": "?accesstoken?"
    }
  ],
  "feeds": [
    {
      "id": 0,
      "title": "My World",
      "account": "t0",
      "resource": "timeline",
      "refresh": "30min",
      "exclude": [1,4]
    }, {
      "id": 2,
      "title": "About Me",
      "account": "t0",
      "resource": "mentions",
      "refresh": "15min",
      "notify": true
    }, {
      "id": 1,
      "title": "My Tweets",
      "account": "t0",
      "resource": "me",
      "refresh": "1hour"
    }, {
      "id": 4,
      "title": "My List",
      "account": "t0",
      "resource": "lists/mylist",
      "refresh": "15min",
      "notify": true
    }, {
      "id": 10,
      "title": "My Search for #tag",
      "account": "t0",
      "resource": "search/#tag",
      "refresh": "120min"
    }, {
      "id": 7,
      "title": "Read Later",
      "resource": "later"
    }, {
      "id": 8,
      "title": "Mentions and Me",
      "account": "sw0",
      "resource": "twitter/12345678/statuses/mentions:twitter/12345678/statuses/user_timeline",
      "refresh": "15min",
      "notify": true
    }, {
      "id": 9,
      "title": "Facebook Home",
      "account": "sw0",
      "resource": "facebook/123456789/me/home",
      "refresh": "30min"
    }
  ]
}
```

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

### Twitter OAuth key and secret

Provide OAuth details build time using environment variables.
The Maven build will then add these into the `.apk`.

```sh
export API_TWITTER_CONSUMER_KEY=1234567890abcdef
export API_TWITTER_CONSUMER_SECRET=1234567890abcdef1234567890abcdef
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
