package nextstep.jwp.controller;

import java.util.Map;
import java.util.Optional;
import nextstep.jwp.model.User;
import nextstep.jwp.service.UserService;
import org.apache.coyote.http11.common.HttpHeaders;
import org.apache.coyote.http11.controller.AbstractController;
import org.apache.coyote.http11.request.HttpRequest;
import org.apache.coyote.http11.response.HttpResponse;
import org.apache.coyote.http11.response.HttpResponse.ResponseBuilder;
import org.apache.coyote.http11.response.Status;
import org.apache.coyote.http11.session.Session;
import utils.ParseUtils;

public class LoginController extends AbstractController {

    private static final String PAGE_401 = "/401.html";
    private static final String PAGE_INDEX = "/index.html";
    private final UserService userService = new UserService();

    @Override
    protected HttpResponse doGet(final HttpRequest request, final HttpResponse response) throws Exception {
        Optional<User> user = request.findUserBySessionId();
        if (user.isPresent()) {
            return response.redirect(PAGE_INDEX);
        }

        final String url = request.getPath() + ".html";
        final String body = readResourceBody(url);
        final HttpHeaders responseHeaders = readResourceHeader(url, body);

        return new ResponseBuilder().status(Status.OK)
                .headers(responseHeaders)
                .body(body)
                .build();
    }

    @Override
    protected HttpResponse doPost(final HttpRequest request, final HttpResponse response) throws Exception {
        Map<String, String> body = ParseUtils.parse(request.getBody(), "&", "=");
        final Optional<User> user = userService.login(body);

        if (user.isEmpty()) {
            return response.redirect(PAGE_401);
        }

        Session session = request.getSession();
        session.setAttribute("user", user.get());
        response.addSessionCookie(session.getId());

        return response.redirect(PAGE_INDEX);
    }
}
