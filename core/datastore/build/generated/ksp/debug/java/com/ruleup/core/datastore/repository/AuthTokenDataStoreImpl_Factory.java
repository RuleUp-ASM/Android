package com.ruleup.core.datastore.repository;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
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
public final class AuthTokenDataStoreImpl_Factory implements Factory<AuthTokenDataStoreImpl> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  private AuthTokenDataStoreImpl_Factory(Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public AuthTokenDataStoreImpl get() {
    return newInstance(dataStoreProvider.get());
  }

  public static AuthTokenDataStoreImpl_Factory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new AuthTokenDataStoreImpl_Factory(dataStoreProvider);
  }

  public static AuthTokenDataStoreImpl newInstance(DataStore<Preferences> dataStore) {
    return new AuthTokenDataStoreImpl(dataStore);
  }
}
