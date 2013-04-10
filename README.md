Onosendai - A Better Deck
=========================

A multi-column Twitter client.

Features
--------

* Timelines, mentions, lists and searches can all be columns.
* Read Later column: any tweet can be saved locally for reading later.
* Background feed fetching and caching.
* Configured via JSON file on the file system.

Supported Services
------------------

* Twitter columns are supported directly (bring your own API key).
* Twitter and Facebook columns via [SuccessWhale](successwhale.com) (work in progress).

Download
--------

Currently there are no stable releases of Onosendai.
Recent snapshot builds can be downloaded from http://karasu.vaguehope.com/onosendai.

Configuration
-------------

All configuration is stored in `deck.conf` file that lives in the root of the external storage device, typically `/sdcard/deck.conf`.
If this file does not exist a template will be created for you to edit when the UI is launched.

### Example config

**Note**: This is to show what possible.  It is not necessarily a good example of what you should put in your config.  Just saying.

**Account IDs and Feed IDs**: These are used to separate data in the DB.
The are arbitrary and do not have to be in order.
If you remove an account or column it may be best not to reuse the ID as strange things may happen.

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
    },
    {
      "id": "sw0",
      "provider": "successwhale",
      "username": "?username?",
      "password": "?password?"
    }
  ],
  "feeds": [
    {
      "id": 0,
      "title": "My World",
      "account": "t0",
      "resource": "timeline",
      "refresh": "30min"
    }, {
      "id": 2,
      "title": "About Me",
      "account": "t0",
      "resource": "mentions",
      "refresh": "15min"
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
      "refresh": "15min"
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
      "refresh": "15min"
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
You must first install [maven-android-sdk-deployer](https://github.com/mosabua/maven-android-sdk-deployer).

```sh
mvn clean install android:deploy android:run
```

The apk can be made smaller using ProGuard.  This project is configured to only shrink the output, not obfuscate.  Use this to make a release build.

```sh
mvn clean install -P release
```

Logging
-------

Enable debug logging with:
```sh
adb shell
setprop log.tag.onosendai DEBUG
```

