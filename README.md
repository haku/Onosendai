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

Supported Services
------------------

* Twitter columns are supported directly.
* Twitter and Facebook columns via [SuccessWhale](http://successwhale.com).
* Posting to [Buffer](https://bufferapp.com) (currently requires advanced config to add account).

Download
--------

Currently there are no 'official' releases of Onosendai.
Recent snapshot builds can be downloaded from http://builds.onosendai.mobi

Background Tasks
----------------

Note that background tasks will run based on battery level.

| Task             | Min battery |
| ---------------- | ----------- |
| Fetch tweets     |         30% |
| Send outbox      |         20% |
| Clean temp files |         50% |

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
You must first install [maven-android-sdk-deployer](https://github.com/mosabua/maven-android-sdk-deployer).

```sh
mvn clean install android:deploy android:run
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
