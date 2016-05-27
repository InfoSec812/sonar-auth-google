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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.server.authentication.UserIdentity;

import static org.assertj.core.api.Assertions.assertThat;

public class UserIdentityFactoryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  Settings settings = new Settings(new PropertyDefinitions(GoogleSettings.definitions()));
  UserIdentityFactory underTest = new UserIdentityFactory(new GoogleSettings(settings));

  /**
   * Keep the same login as at GitHub
   */
  @Test
  public void create_login_for_provider_strategy() {
    GsonUser gson = GsonUser.parse("{\n" +
            "    \"id\": \"42\",\n" +
            "    \"email\": \"john.smith@google.com\",\n" +
            "    \"verified_email\": true,\n" +
            "    \"name\": \"John Smith\",\n" +
            "    \"given_name\": \"John\",\n" +
            "    \"family_name\": \"Smith\",\n" +
            "    \"picture\": \"https://lh3.googleusercontent.com/-AAAAAAAA/AAAAAAAAAAA/AAAAAAAAAAA/AAAAAAAAAA/photo.jpg\",\n" +
            "    \"locale\": \"en-US\"\n" +
            "}");
    settings.setProperty(GoogleSettings.LOGIN_STRATEGY, GoogleSettings.LOGIN_STRATEGY_PROVIDER_LOGIN);
    UserIdentity identity = underTest.create(gson);
    assertThat(identity.getLogin()).isEqualTo("john.smith@google.com");
    assertThat(identity.getName()).isEqualTo("John Smith");
    assertThat(identity.getEmail()).isEqualTo("john.smith@google.com");
  }

  @Test
  public void create_login_for_unique_login_strategy() {
    GsonUser gson = GsonUser.parse("{\n" +
            "    \"id\": \"42\",\n" +
            "    \"email\": \"john.smith@google.com\",\n" +
            "    \"verified_email\": true,\n" +
            "    \"name\": \"John Smith\",\n" +
            "    \"given_name\": \"John\",\n" +
            "    \"family_name\": \"Smith\",\n" +
            "    \"picture\": \"https://lh3.googleusercontent.com/-AAAAAAAA/AAAAAAAAAAA/AAAAAAAAAAA/AAAAAAAAAA/photo.jpg\",\n" +
            "    \"locale\": \"en-US\"\n" +
            "}");
    settings.setProperty(GoogleSettings.LOGIN_STRATEGY, GoogleSettings.LOGIN_STRATEGY_UNIQUE);

    UserIdentity identity = underTest.create(gson);
    assertThat(identity.getLogin()).isEqualTo("john.smith@google.com");
    assertThat(identity.getName()).isEqualTo("John Smith");
    assertThat(identity.getEmail()).isEqualTo("john.smith@google.com");
  }

  @Test
  public void empty_name_is_replaced_by_provider_login() {
    GsonUser gson = new GsonUser("john", "");

    UserIdentity identity = underTest.create(gson);
    assertThat(identity.getName()).isEqualTo("john");
  }

  @Test
  public void null_name_is_replaced_by_provider_login() {
    GsonUser gson = new GsonUser("john", null);

    UserIdentity identity = underTest.create(gson);
    assertThat(identity.getName()).isEqualTo("john");
  }

  @Test
  public void throw_ISE_if_strategy_is_not_supported() {
    settings.setProperty(GoogleSettings.LOGIN_STRATEGY, "xxx");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Login strategy not supported : xxx");
    underTest.create(new GsonUser("john", "john"));
  }
}
