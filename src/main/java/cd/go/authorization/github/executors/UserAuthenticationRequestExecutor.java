/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cd.go.authorization.github.executors;

import cd.go.authorization.github.GitHubAuthenticator;
import cd.go.authorization.github.GitHubAuthorizer;
import cd.go.authorization.github.GitHubClientBuilder;
import cd.go.authorization.github.exceptions.NoAuthorizationConfigurationException;
import cd.go.authorization.github.models.AuthConfig;
import cd.go.authorization.github.models.User;
import cd.go.authorization.github.requests.UserAuthenticationRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.kohsuke.github.GitHub;

import java.util.HashMap;
import java.util.Map;

import static cd.go.authorization.github.utils.Util.GSON;
import static com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse.SUCCESS_RESPONSE_CODE;

public class UserAuthenticationRequestExecutor implements RequestExecutor {
    private final UserAuthenticationRequest request;
    private final GitHubClientBuilder providerManager;
    private final GitHubAuthenticator gitHubAuthenticator;
    private final GitHubAuthorizer gitHubAuthorizer;

    public UserAuthenticationRequestExecutor(UserAuthenticationRequest request) {
        this(request, new GitHubClientBuilder(), new GitHubAuthenticator(), new GitHubAuthorizer());
    }

    UserAuthenticationRequestExecutor(UserAuthenticationRequest request, GitHubClientBuilder providerManager, GitHubAuthenticator gitHubAuthenticator, GitHubAuthorizer gitHubAuthorizer) {
        this.request = request;
        this.providerManager = providerManager;
        this.gitHubAuthenticator = gitHubAuthenticator;
        this.gitHubAuthorizer = gitHubAuthorizer;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        if (request.authConfigs() == null || request.authConfigs().isEmpty()) {
            throw new NoAuthorizationConfigurationException("[Authenticate] No authorization configuration found.");
        }

        final AuthConfig authConfig = request.authConfigs().get(0);
        final GitHub gitHub = providerManager.build(request.tokenInfo().accessToken(), authConfig);
        final User user = gitHubAuthenticator.authenticate(gitHub, authConfig);

        Map<String, Object> userMap = new HashMap<>();
        if (user != null) {
            userMap.put("user", user);
            userMap.put("roles", gitHubAuthorizer.authorize(user, gitHub, request.roles()));
        }

        DefaultGoPluginApiResponse response = new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, GSON.toJson(userMap));
        return response;
    }
}
