package com.ruleup.domain.auth.usecase;

import com.ruleup.domain.auth.repository.OAuthAuthorizer;
import com.ruleup.domain.repository.AuthRepository;
import com.ruleup.domain.repository.SessionRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class AuthUseCase_Factory implements Factory<AuthUseCase> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<OAuthAuthorizer> oAuthProvider;

  private final Provider<SessionRepository> sessionRepositoryProvider;

  private AuthUseCase_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<OAuthAuthorizer> oAuthProvider,
      Provider<SessionRepository> sessionRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.oAuthProvider = oAuthProvider;
    this.sessionRepositoryProvider = sessionRepositoryProvider;
  }

  @Override
  public AuthUseCase get() {
    return newInstance(authRepositoryProvider.get(), oAuthProvider.get(), sessionRepositoryProvider.get());
  }

  public static AuthUseCase_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<OAuthAuthorizer> oAuthProvider,
      Provider<SessionRepository> sessionRepositoryProvider) {
    return new AuthUseCase_Factory(authRepositoryProvider, oAuthProvider, sessionRepositoryProvider);
  }

  public static AuthUseCase newInstance(AuthRepository authRepository,
      OAuthAuthorizer oAuthProvider, SessionRepository sessionRepository) {
    return new AuthUseCase(authRepository, oAuthProvider, sessionRepository);
  }
}
