# Google Authentication Plug-in for SonarQube #
[![Build Status](https://api.travis-ci.org/InfoSec812/sonar-auth-google.svg)](https://travis-ci.org/InfoSec812/sonar-auth-google)


## Description ##
This plugin enables user authentication and Single Sign-On via [Google](https://google.com/).
It is heavily based on the code by [Julien Lancelot](https://github.com/SonarQubeCommunity/sonar-auth-bitbucket)
Compatible with SonarQube version 5.6 and higher.

## Feedback Process ##
* [Releases](releases/) are created with every SNAPSHOT, but only non-SNAPSHOT releases are added to the Update Center
* Snapshot releases are up for public review as release candidates
* When a SNAPSHOT is deemed of sufficient quality to release, an announcement will be sent to the SonarQube Google Group requesting feedback
* After the feedback period, the SNAPSHOT release will be re-tagged as a release and uploaded to the Update Center

## Installation ##
1. Install the plugin through the [Update Center](http://docs.sonarqube.org/display/SONAR/Update+Center) or download it into the *SONARQUBE_HOME/extensions/plugins* directory
1. Restart the SonarQube server

## Usage ##
In the [Google Developers Console](https://console.developers.google.com/):
1. Go to "Credentials"
2. Click on the "Create credentials" drop-down, and select "OAuth client ID"
3. Set the "Application type" to "Web application"
4. Set the "Name" value to something which you will associated with SonarQube
5. Set the "Authorized JavaScript origins" to the base URL of your SonarQube server web application (no path allowed)
6. Set the "Authorized redirect URIs" to be:
   * ${sonarBaseURL}/oauth2/callback/googleoauth

In SonarQube settings :
1. Go to "Security" -> "Google"
2. Set the "Enabled" property to true
3. Set Google authentication URI: https://accounts.google.com/o/oauth2/v2/auth
4. Set the "OAuth client ID" from the value provided by the Google OAuth consumer
5. Set the "OAuth consumer Secret" from the value provided by the Google OAuth consumer

Go to the login form, a new button "Log in with Google" allow users to connect to SonarQube with their Google accounts.

> Note: Only HTTPS is supported
> * SonarQube must be publicly accessible through HTTPS only
> * The property 'sonar.core.serverBaseURL' must be set to this public HTTPS URL

## General Configuration ##

Property | Description | Default value
---------| ----------- | -------------
sonar.auth.google.allowUsersToSignUp|Allow new users to authenticate. When set to 'false', only existing users will be able to authenticate to the server|true
sonar.auth.google.clientId.secured|Consumer Key provided by Google when registering the consumer|None
sonar.auth.google.clientSecret.secured|Consumer password provided by Google when registering the consumer|None
sonar.auth.google.enabled|Enable Google users to login. Value is ignored if consumer Key and Secret are not defined|false
sonar.auth.google.loginStrategy|When the login strategy is set to 'Unique', the user's login will be auto-generated the first time so that it is unique. When the login strategy is set to 'Same as Google login', the user's login will be the Google login. This last strategy allows, when changing the authentication provider, to keep existing users (if logins from new provider are the same than Google)|Unique
sonar.auth.google.limitOauthDomain|When set with a GApps domain, only allow users from that domain to authenticate|None






