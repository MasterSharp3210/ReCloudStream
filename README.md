<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="ReCloudStream Logo" width="128" />
</p>

# ReCloudStream

*As you might imagine or already know, apps and solutions for viewing multimedia content have almost always been short-lived. They typically last only about 10 days—or, at best, a few months—before you’re forced to abandon them and move on to other platforms. ReCloudStream, by contrast, is built upon a foundation of long-term databases and code. Consequently, there won’t be frequent updates; my goal is to make the original 'CloudStream' project more secure and sustainable over time.*


## Table of Contents: 
+ [About Us:](#about_us)
+ [Installation Steps:](#install_rules)
+ [Changes from Original:](#changes_from_original)
+ [Contributing:](#contributing)
+ [Issues:](#issues)
  + [Bugs Reports:](#bug_report)
  + [Enhancement:](#enhancment)
+ [Extension Development:](#extensions)
+ [Language Support:](#languages)
+ [Further Sources](#contact_and_sources)


<a id="about_us"></a>

## About us: 

**ReCloudStream is a media center that prioritizes and emphasizes complete freedom and flexibility for users and developers.**

ReCloudStream is an extension-based multimedia player with tracking support. There are extensions to view videos from:

+ [Librevox (audio-books)](https://librivox.org/) 
+ [Youtube](https://www.youtube.com/)
+ [Twitch](https://www.twitch.tv/)
+ [iptv-org (A collection of publicly available IPTV (Internet Protocol television) channels from all over the world.)](https://github.com/iptv-org/iptv) 
+ [nginx](https://nginx.org/)
+ And more... 


**Please don't create illegal extensions or use any that host any copyrighted media.** For more details about our stance on the DMCA and EUCD, you can read about it on our organization: [reCloudStream](https://github.com/recloudstream)

#### Important Copyright Note: 

Our documentation is unmaintained and open to contributions; therefore, apps and sources, extensions in recommended sources, and recommended apps are not officially moderated or endorsed by CloudStream; if you or another copyright owner identify an extension that breaches your copyright, please let us know. 


#### Features:
+ **AdFree**, No ads whatsoever
+ No tracking/analytics
+ Bookmarks
+ Phone and TV support
+ Chromecast
+ Extension system for personal customization


<a id="install_rules"></a>

## Installation: 

Our documentation provides the steps to install and configure CloudStream for your streaming needs.

[Getting Started With CloudStream:](https://recloudstream.github.io/csdocs/)

<a id="changes_from_original"></a>

## Changes from Original Project:

ReCloudStream includes several enhancements and bug fixes compared to the original CloudStream project to ensure better stability, security, and user experience:

### 🎬 Subtitle Engine Improvements
* **Format Resilience**: Flexible detection of `.srt` and WebVTT files, handling control characters, Byte Order Mark (`\uFEFF`), and non-standard header lines.
* **Desynchronization & Skipped Dialog Fixes**: Resolved parsing bugs in `CustomSubripParser` that caused skipped dialogs or misplaced lines when encountering malformed cue indices.
* **End-of-Video Truncation Fix**: Corrected buffer size management during decoding, preventing subtitles from being cut off during the final minutes of playback.
* **Accented Encoding Support (Windows-1252 / ISO-8859-1)**: Automatic charset detection for European and Italian subtitles (`à, è, é, ì, ò, ù`), eliminating corrupted or garbled text characters.
* **Overlapping Subtitles**: Set `MERGE` behavior as default so concurrent subtitles and on-screen text in the same timestamp display simultaneously rather than replacing each other.
* **Offset & Delay Sync Stability**: Retained active decoder references to prevent "No subtitles loaded" errors when adjusting subtitle timing in the sync menu.

### ⚙️ Build System & Toolchain
* **Gradle 9+ Compatibility**: Integrated Foojay Toolchain Resolver plugin (`1.0.0`) in `settings.gradle.kts` for automatic Java 17 toolchain provisioning.

<a id="contributing"></a>

## Contributing:
We **happily** accept any contributions to our project. To find out where you can start contributing towards the project, please look [at our issues tab](/cloudstream/issues)



<a id="issues"></a> 
 
### Issues: 
While we **actively** accept issues and pull requests, we do require you fill out an [template](https://github.com/recloudstream/cloudstream/issues/new/choose) for issues. These include the following:

<a id="bug_report"></a>

- [Bug Report Template: ](https://github.com/recloudstream/cloudstream/issues/new?assignees=&labels=bug&projects=&template=application-bug.yml)
  - For bug reports, we want as much info as possible, including your downloaded version of CloudeStream, device and updated version (if possible, current API),
    expected behavior of the program, and the actual behavior that the program did, most importantly we require clear, reproducible steps of the bug. If your bug can't be       reproduced, it is unlikely we'll work on your issue.
    
<a id="enhancment"></a>
  
- [Feature Request Template: ](https://github.com/recloudstream/cloudstream/issues/new?assignees=&labels=enhancement&projects=&template=feature-request.yml)
  - Before adding a feature request, please check to see if a feature request already has been requested.  


### Extensions:
 
**Further details on creating extensions for CloudStream are found in our documentation.**

[Guide: For Extension Developers](https://recloudstream.github.io/csdocs/devs/gettingstarted/) 

<a id="contact_and_sources"></a>

## Further Sources: 

As well as providing clear install steps, our [website](https://dweb.link/ipns/cloudstream.on.fleek.co/) includes a wide variety of other tools, such as: 
- [Troubleshooting](https://recloudstream.github.io/csdocs/troubleshooting/)
- [Further CloudStream Repositories](https://recloudstream.github.io/csdocs/repositories/) 
- Set-Up for Android devices, including [Android TV](https://recloudstream.github.io/csdocs/other-devices/tv/).
- ReCloudStream currently provides an Android app; desktop clients are not included.
- And more...

<a id="languages"> </a>  

### Supported languages:

The app interface is available in English and Italian. English is the default language; Italian can be selected during setup or from Settings.
