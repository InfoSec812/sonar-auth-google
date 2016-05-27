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
package org.sonarqube.auth.googleoauth;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IntegrationTest {

  private static final String CALLBACK_URL = "http://localhost/oauth/callback/googleoauth";

  @Rule
  public MockWebServer google = new MockWebServer();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  // load settings with default values
  Settings settings = new Settings(new PropertyDefinitions(GoogleSettings.definitions()));
  GoogleSettings googleSettings = new GoogleSettings(settings);
  UserIdentityFactory userIdentityFactory = new UserIdentityFactory(googleSettings);
  GoogleScribeApi scribeApi = new GoogleScribeApi(googleSettings);
  GoogleIdentityProvider underTest = new GoogleIdentityProvider(googleSettings, userIdentityFactory, scribeApi);

  @Before
  public void enable() {
    settings.setProperty("sonar.auth.googleoauth.clientId.secured", "the_id");
    settings.setProperty("sonar.auth.googleoauth.clientSecret.secured", "the_secret");
    settings.setProperty("sonar.auth.googleoauth.enabled", true);
    settings.setProperty("sonar.auth.googleoauth.limitOauthDomain", "googleoauth.com");
    settings.setProperty("sonar.auth.googleoauth.apiUrl", format("http://%s:%d", google.getHostName(), google.getPort()));
    settings.setProperty("sonar.auth.googleoauth.webUrl", format("http://%s:%d/o/oauth2/auth", google.getHostName(), google.getPort()));
  }

  /**
   * First phase: SonarQube redirects browser to Google authentication form, requesting the
   * minimal access rights ("scope") to get user profile.
   */
  @Test
  public void redirect_browser_to_google_authentication_form() throws Exception {
    DumbInitContext context = new DumbInitContext("the-csrf-state");
    underTest.init(context);
    assertThat(context.redirectedTo)
      .startsWith(google.url("o/oauth2/auth").toString())
      .contains("scope=" + "openid%20email");
  }

  /**
   * Second phase: Google redirects browser to SonarQube at /oauth/callback/googleoauth?code={the verifier code}.
   * This SonarQube web service sends two requests to Google:
   * <ul>
   *   <li>get an access token</li>
   *   <li>get the profile (login, name) of the authenticated user</li>
   * </ul>
   */
  @Test
  public void callback_on_successful_authentication() throws IOException, InterruptedException {
    google.enqueue(newSuccessfulAccessTokenResponse());
    // response of https://www.googleapis.com/oauth2/v3/token
    google.enqueue(new MockResponse().setBody("{\n" +
            "    \"id\": \"42\",\n" +
            "    \"email\": \"john.smith@googleoauth.com\",\n" +
            "    \"verified_email\": true,\n" +
            "    \"name\": \"John Smith\",\n" +
            "    \"given_name\": \"John\",\n" +
            "    \"family_name\": \"Smith\",\n" +
            "    \"picture\": \"https://lh3.googleusercontent.com/-AAAAAAAA/AAAAAAAAAAA/AAAAAAAAAAA/AAAAAAAAAA/photo.jpg\",\n" +
            "    \"locale\": \"en-US\"\n" +
            "}"));

    HttpServletRequest request = newRequest("the-verifier-code");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request);
    underTest.callback(callbackContext);

    assertThat(callbackContext.csrfStateVerified.get()).isFalse();
    assertThat(callbackContext.userIdentity.getLogin()).isEqualTo("john.smith@googleoauth.com");
    assertThat(callbackContext.userIdentity.getName()).isEqualTo("John Smith");
    assertThat(callbackContext.userIdentity.getEmail()).isEqualTo("john.smith@googleoauth.com");
    assertThat(callbackContext.redirectSent.get()).isTrue();

    // Verify the requests sent to Google
    RecordedRequest accessTokenRequest = google.takeRequest();
    assertThat(accessTokenRequest.getPath()).startsWith("/oauth2/v3/token");
    RecordedRequest userRequest = google.takeRequest();
    assertThat(userRequest.getPath()).startsWith("/oauth2/v1/userinfo");
  }

  /**
   * Second phase: Google redirects browser to SonarQube at /oauth/callback/googleoauth?code={the verifier code}.
   * This SonarQube web service sends two requests to Google:
   * <ul>
   *   <li>get an access token</li>
   *   <li>get the profile (login, name) of the authenticated user</li>
   * </ul>
   */
  @Test
  public void callback_on_successful_authentication_without_domain() throws IOException, InterruptedException {
    settings.removeProperty("sonar.auth.googleoauth.limitOauthDomain");
    callback_on_successful_authentication();
  }

  @Test
  public void callback_throws_OAE_if_error_when_requesting_user_profile() throws IOException, InterruptedException {
    google.enqueue(newSuccessfulAccessTokenResponse());
    // https://accounts.google.com/o/oauth2/token fails
    google.enqueue(new MockResponse().setResponseCode(500).setBody("{error}"));

    DumbCallbackContext callbackContext = new DumbCallbackContext(newRequest("the-verifier-code"));
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not get Google user profile. HTTP code: 500, response: {error}");
    underTest.callback(callbackContext);

    assertThat(callbackContext.csrfStateVerified.get()).isTrue();
    assertThat(callbackContext.userIdentity).isNull();
    assertThat(callbackContext.redirectSent.get()).isFalse();
  }

  @Test
  public void callback_redirects_to_unauthorized_if_domain_does_not_match() throws IOException, InterruptedException {
    google.enqueue(newSuccessfulAccessTokenResponse());
    // https://accounts.google.com/o/oauth2/token fails
    google.enqueue(new MockResponse().setResponseCode(200).setBody("{\n"+
            "    \"email\": \"john.smith@example.com\",\n" +
            "    \"verified_email\": true,\n" +
            "    \"name\": \"John Smith\",\n" +
            "    \"given_name\": \"John\",\n" +
            "    \"family_name\": \"Smith\",\n" +
            "    \"picture\": \"https://lh3.googleusercontent.com/-AAAAAAAA/AAAAAAAAAAA/AAAAAAAAAAA/AAAAAAAAAA/photo.jpg\",\n" +
            "    \"locale\": \"en-US\"\n" +
            "}"));

    HttpServletRequest request = newRequest("the-verifier-code");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request);
    underTest.callback(callbackContext);

    assertThat(callbackContext.csrfStateVerified.get()).isFalse();
    assertThat(callbackContext.userIdentity).isNull();
  }

  /**
   * Response sent by Bitbucket to SonarQube when generating an access token
   */
  private static MockResponse newSuccessfulAccessTokenResponse() {
    return new MockResponse().setBody("{\"access_token\":\"e72e16c7e42f292c6912e7710c838347ae178b4a\",\"scope\":\"user\"}");
  }

  private static HttpServletRequest newRequest(String verifierCode) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter("code")).thenReturn(verifierCode);
    return request;
  }

  private static class DumbCallbackContext implements OAuth2IdentityProvider.CallbackContext {
    final HttpServletRequest request;
    final AtomicBoolean csrfStateVerified = new AtomicBoolean(false);
    final AtomicBoolean redirectSent = new AtomicBoolean(false);
    final AtomicBoolean redirectedToRequestedPage = new AtomicBoolean(false);
    UserIdentity userIdentity = null;

    public DumbCallbackContext(HttpServletRequest request) {
      this.request = request;
    }

    @Override
    public void verifyCsrfState() {
      this.csrfStateVerified.set(true);
    }

    @Override
    public void redirectToRequestedPage() {
      redirectedToRequestedPage.set(true);
    }

    @Override
    public void authenticate(UserIdentity userIdentity) {
      this.userIdentity = userIdentity;
    }

    @Override
    public String getCallbackUrl() {
      return CALLBACK_URL;
    }

    @Override
    public HttpServletRequest getRequest() {
      return request;
    }

    @Override
    public HttpServletResponse getResponse() {
      return new HttpServletResponse() {
        @Override
        public void addCookie(Cookie cookie) {

        }

        @Override
        public boolean containsHeader(String name) {
          return false;
        }

        @Override
        public String encodeURL(String url) {
          return null;
        }

        @Override
        public String encodeRedirectURL(String url) {
          return null;
        }

        @Override
        public String encodeUrl(String url) {
          return null;
        }

        @Override
        public String encodeRedirectUrl(String url) {
          return null;
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {

        }

        @Override
        public void sendError(int sc) throws IOException {

        }

        @Override
        public void sendRedirect(String location) throws IOException {
          redirectSent.set(true);
        }

        @Override
        public void setDateHeader(String name, long date) {

        }

        @Override
        public void addDateHeader(String name, long date) {

        }

        @Override
        public void setHeader(String name, String value) {

        }

        @Override
        public void addHeader(String name, String value) {

        }

        @Override
        public void setIntHeader(String name, int value) {

        }

        @Override
        public void addIntHeader(String name, int value) {

        }

        @Override
        public void setStatus(int sc) {

        }

        @Override
        public void setStatus(int sc, String sm) {

        }

        @Override
        public int getStatus() {
          return 0;
        }

        @Override
        public String getHeader(String name) {
          return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
          return null;
        }

        @Override
        public Collection<String> getHeaderNames() {
          return null;
        }

        @Override
        public String getCharacterEncoding() {
          return null;
        }

        @Override
        public String getContentType() {
          return null;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
          return null;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
          return null;
        }

        @Override
        public void setCharacterEncoding(String charset) {

        }

        @Override
        public void setContentLength(int len) {

        }

        @Override
        public void setContentType(String type) {

        }

        @Override
        public void setBufferSize(int size) {

        }

        @Override
        public int getBufferSize() {
          return 0;
        }

        @Override
        public void flushBuffer() throws IOException {

        }

        @Override
        public void resetBuffer() {

        }

        @Override
        public boolean isCommitted() {
          return false;
        }

        @Override
        public void reset() {

        }

        @Override
        public void setLocale(Locale loc) {

        }

        @Override
        public Locale getLocale() {
          return null;
        }
      };
    }
  }

  private static class DumbInitContext implements OAuth2IdentityProvider.InitContext {
    String redirectedTo = null;
    private final String generatedCsrfState;

    public DumbInitContext(String generatedCsrfState) {
      this.generatedCsrfState = generatedCsrfState;
    }

    @Override
    public String generateCsrfState() {
      return generatedCsrfState;
    }

    @Override
    public void redirectTo(String url) {
      this.redirectedTo = url;
    }

    @Override
    public String getCallbackUrl() {
      return CALLBACK_URL;
    }

    @Override
    public HttpServletRequest getRequest() {
      return null;
    }

    @Override
    public HttpServletResponse getResponse() {
      return null;
    }
  }
}
