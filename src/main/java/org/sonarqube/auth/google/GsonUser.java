/*
 * Google Authentication for SonarQube
 * Copyright (C) 2016-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarqube.auth.google;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.math.BigInteger;

/**
 * Lite representation of JSON response of GET https://www.googleapis.com/oauth2/v1/userinfo
 */
public class GsonUser {
  @SerializedName("id")
  private BigInteger id;

  @SerializedName("picture")
  private String picture;

  @SerializedName("locale")
  private String locale;

  @SerializedName("verified_email")
  private Boolean verifiedEmail;

  @SerializedName("given_name")
  private String givenName;

  @SerializedName("family_name")
  private String familyName;

  @SerializedName("email")
  private String email;

  @SerializedName("name")
  private String name;

  public GsonUser() {
    // even if empty constructor is not required for Gson, it is strongly
    // recommended:
    // http://stackoverflow.com/a/18645370/229031
  }

  GsonUser(String email, @Nullable String name) {
    this.email = email;
    this.name = name;
  }

  public String getUsername() {
    return email;
  }

  @CheckForNull
  public String getDisplayName() {
    return name;
  }

  public String getEmail() {
    return this.email;
  }

  public static GsonUser parse(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, GsonUser.class);
  }
}
