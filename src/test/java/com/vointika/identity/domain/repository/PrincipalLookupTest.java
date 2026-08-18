package com.vointika.identity.domain.repository;

import com.vointika.identity.domain.entity.User;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.shared.port.UserAccountQuery;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@code UserRepository.requireById} actually runs.
 *
 * <p>`PATTERNS.md` §9: Mockito stubs a {@code default} like any other method, and
 * `UserRepository` is mocked in every identity use-case test — so folding five
 * `orElseThrow`s into this one moved the throw somewhere a mock reaches only when
 * asked. The five call sites keep the stronger arrangement (`doCallRealMethod` in
 * their `setUp`, stubbing the abstract `findById`), so they exercise the real branch
 * too; this file pins it independently of any caller.
 *
 * <p><b>It is a 401, not a 404</b>, and that is the assertion worth having: the id
 * comes from a verified token, so an empty lookup means the account went away under a
 * live session. Answering 404 would tell the client to stop asking for a resource
 * when what it needs to do is re-authenticate. Change the exception type and this
 * fails.
 */
class PrincipalLookupTest {

    private static final UUID USER = UUID.fromString("019f8c73-4d21-7b90-a5e2-6c1f80d34a77");

    @Test
    void aPrincipalWithNoAccountIsUnauthorized() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findById(USER)).thenReturn(Optional.empty());
        doCallRealMethod().when(repository).requireById(USER);

        assertThatThrownBy(() -> repository.requireById(USER))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(UserAccountQuery.INVALID_PRINCIPAL);
    }

    @Test
    void aPresentUserComesBackUnwrapped() {
        User found = mock(User.class);
        UserRepository repository = mock(UserRepository.class);
        when(repository.findById(USER)).thenReturn(Optional.of(found));
        doCallRealMethod().when(repository).requireById(USER);

        assertThat(repository.requireById(USER)).isSameAs(found);
    }
}
