# Bitbucket Authentication Plug-in for SonarQube #

## Description ##
This plugin enables user authentication and Single Sign-On via [Bitbucket](https://bitbucket.org/).

## Installation ##
1. Put the jar created by maven in the target directory into the *SONARQUBE_HOME/extensions/plugins* directory
1. Restart the SonarQube server

## Usage ##
1. In Bitbucket, create a Developer application :
  1. Go to "Add-ons" -> "OAuth" -> "Add consumer"
  2. Name : Something like "My Company SonarQube"
  3. URL : SonarQube URL
  4. Callback URL : <SonarQube URL>/oauth2/callback
  5. Permissions : Check Account -> Read (Email will automatically be selected)
2. In SonarQube settings :
  1. Go to "Security" -> "Bitbucket"
  2. Set the "Enabled" property to true
  3. Set the "OAuth consumer Key" from the value provided by the Bitbucket OAuth consumer
  4. Set the "OAuth consumer Secret" from the value provided by the Bitbucket OAuth consumer
3. Go to the login form, a new button "Log in with Bitbucket" allow users to connect to SonarQube with their Bitbucket accounts.

> Note: Only HTTPS is supported
> * SonarQube must be publicly accessible through HTTPS only
> * The property 'sonar.core.serverBaseURL' must be set to this public HTTPS URL

## General Configuration ##

Property | Description | Default value
---------| ----------- | -------------
sonar.auth.bitbucket.allowUsersToSignUp|Allow new users to authenticate. When set to 'false', only existing users will be able to authenticate to the server|true
sonar.auth.bitbucket.clientId|Consumer Key provided by Bitbucket when registering the consumer|None
sonar.auth.bitbucket.clientSecret|Consumer password provided by Bitbucket when registering the consumer|None
sonar.auth.bitbucket.enabled|Enable Bitbucket users to login. Value is ignored if consumer Key and Secret are not defined|false
sonar.auth.bitbucket.loginStrategy|When the login strategy is set to 'Unique', the user's login will be auto-generated the first time so that it is unique. When the login strategy is set to 'Same as Bitbucket login', the user's login will be the Bitbucket login. This last strategy allows, when changing the authentication provider, to keep existing users (if logins from new provider are the same than Bitbucket)|Unique







