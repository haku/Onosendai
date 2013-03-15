Onosendai - A better deck
=========================

A multi-column Twitter client with a focus on list support.

Features
--------

* Background tweet fetching (currently fixed 15 min interval)
* Home timeline, mentions, personal timeline, and lists can all be columns.
* Configured via JSON file on the file system.
* Currently can only read, not write (its a work in progress)

Configuration
-------------

All configuration is stored in `deck.conf` file that lives in the root
of the external storage device, typically `/sdcard/deck.conf`.
If this file does not exist it will created when the UI is launched.

### Example config

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
    }
  ],
  "columns": [
    {
      "id": 0,
      "title": "My World",
      "account": "t0",
      "resource": "timeline",
      "refresh": "15min"
    }, {
      "id": 1,
      "title": "About Me",
      "account": "t0",
      "resource": "mentions",
      "refresh": "15min"
    }, {
      "id": 2,
      "title": "My Tweets",
      "account": "t0",
      "resource": "me",
      "refresh": "15min"
    }, {
      "id": 3,
      "title": "My List",
      "account": "t0",
      "resource": "lists/mylist",
      "refresh": "15min"
    }
  ]
}
```

### Background refreshing

Currently all lists will always background refresh on a 15 min time.

License
-------
This source code is made avaiable under the Apache 2 licence.
This copy of the source code should also contain LICENCE and NOTICE files which contain the full licence terms copyright notices respectfully.
The full licence can also be found here: http://www.apache.org/licenses/LICENSE-2.0

Building from source
--------------------

This code is build using Maven.
You must first install [maven-android-sdk-deployer](https://github.com/mosabua/maven-android-sdk-deployer).

```sh
mvn clean install android:deploy android:run
```

Logging
-------

Enable debug logging with:
```sh
adb shell
setprop log.tag.onosendai DEBUG
```

